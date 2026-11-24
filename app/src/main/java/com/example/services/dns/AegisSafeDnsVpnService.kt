package com.example.services.dns

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.core.models.SafeSearchPack
import com.example.core.notifications.AegisNotificationHelper
import com.example.di.AegisContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Zero-battery local Safe DNS VPN tunnel.
 *
 * Routes ONLY DNS resolver traffic (10.111.222.2/32) through TUN while letting all
 * normal TCP/UDP app traffic bypass the VPN natively. Allowed queries are forwarded via
 * protect() to Cloudflare Family Safe DNS (1.1.1.3) and CleanBrowsing Family (185.228.168.168).
 */
class AegisSafeDnsVpnService : VpnService() {

    private val container by lazy { AegisContainer.getInstance(this) }
    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    private var upstreamPool: ExecutorService? = null

    @Volatile
    private var isRunningInternal: Boolean = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!container.preferences.masterPowerEnabled.value) {
            stopVpnTunnel()
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpnTunnel()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (prepare(this) != null) {
                    Log.w(TAG, "VPN permission not granted; stopping Safe DNS VPN service.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForegroundNotification()
                startVpnTunnel()
                return START_STICKY
            }
        }
    }

    private fun startForegroundNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sheildlenz Safe DNS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Zero-battery local DNS filtering against unsafe domains"
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            3001,
            launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notificationsOn = container.preferences.notificationsEnabled.value
        val channelId = if (notificationsOn) {
            CHANNEL_ID
        } else {
            AegisNotificationHelper.ensureSilentChannel(this)
            AegisNotificationHelper.SILENT_MONITOR_CHANNEL_ID
        }

        val note: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (notificationsOn) "Sheildlenz Safe DNS Active" else "Network Filter")
            .setContentText(if (notificationsOn) "System-wide SafeSearch & blocklists filtering DNS queries locally" else "Active")
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(!notificationsOn)
            .setPriority(if (notificationsOn) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, note)
        }
    }

    @Synchronized
    private fun startVpnTunnel() {
        if (isRunningInternal && vpnInterface != null) return

        stopVpnTunnel()
        try {
            val builder = Builder()
                .setSession("Sheildlenz Safe DNS")
                .addAddress(VPN_CLIENT_ADDRESS, 32)
                .addDnsServer(DNS_TUNNEL_ADDRESS)
                .addRoute(DNS_TUNNEL_ADDRESS, 32)
                .setMtu(1500)

            // Prevent VPN loop by excluding Sheildlenz's own upstream sockets as defense-in-depth
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {
            }

            val tun = builder.establish()
            if (tun == null) {
                Log.w(TAG, "VpnService.Builder.establish() returned null.")
                stopSelf()
                return
            }

            vpnInterface = tun
            isRunningInternal = true
            _isTunnelActive.value = true
            upstreamPool = Executors.newFixedThreadPool(4)

            workerThread = Thread({ runTunLoop(tun) }, "AegisSafeDnsLoop").apply {
                isDaemon = true
                start()
            }
            Log.i(TAG, "Safe DNS local VPN tunnel started (DNS-only route $DNS_TUNNEL_ADDRESS/32).")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish Safe DNS VPN tunnel", e)
            stopVpnTunnel()
            stopSelf()
        }
    }

    private fun runTunLoop(tun: ParcelFileDescriptor) {
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val writeLock = Any()
        val packet = ByteArray(32767)

        try {
            while (isRunningInternal) {
                val len = input.read(packet)
                if (len <= 0) {
                    if (len < 0) break
                    continue
                }

                val query = DnsPacketHelper.parseIpv4UdpDnsPacket(packet, len) ?: continue
                val prefs = container.preferences
                val packMatch = if (prefs.smartSafeSearchEnabled.value) {
                    SafeSearchPack.matchesDomainInActivePacks(query.domain, prefs.safeSearchPacks.value)
                } else {
                    null
                }
                val blockMatch = DnsPacketHelper.matchesBlockedDomainDetailed(
                    rawDomain = query.domain,
                    customDomains = prefs.customBlockedDomains.value,
                    customKeywords = prefs.customBlockedKeywords.value,
                    blockExplicitAdult = prefs.blockExplicitAdult.value,
                    blockGambling = prefs.blockGamblingKeywords.value,
                    whitelistedDomains = emptySet(),
                    blockDohBypass = true
                ) ?: packMatch?.let {
                    BlockedDomainMatch(
                        category = it.title,
                        reason = it.title,
                        matchedRule = query.domain
                    )
                }

                if (blockMatch != null) {
                    val blockedDns = DnsPacketHelper.buildBlockedDnsResponsePayload(query.dnsPayload)
                    if (blockedDns != null) {
                        val respPacket = DnsPacketHelper.buildIpv4UdpResponsePacket(query, blockedDns)
                        synchronized(writeLock) {
                            output.write(respPacket)
                        }
                        _blockedQueryCount.value += 1
                        if (packMatch != null) {
                            prefs.incrementSafeSearchBlockedCount()
                        }
                        _lastBlockedDomain.value = "${query.domain} (${blockMatch.reason})"
                        container.blockedDnsLogRepository.recordBlockedQuery(
                            domain = query.domain,
                            reasonCategory = blockMatch.category,
                            matchedRule = blockMatch.matchedRule
                        )
                    }
                    continue
                }

                val pool = upstreamPool ?: continue
                pool.execute {
                    forwardToUpstreamDns(query, output, writeLock)
                }
            }
        } catch (e: Exception) {
            if (isRunningInternal) {
                Log.w(TAG, "Safe DNS TUN loop terminated: ${e.message}")
            }
        } finally {
            try {
                input.close()
            } catch (_: Exception) {
            }
            try {
                output.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun forwardToUpstreamDns(
        query: ParsedDnsQuery,
        tunOutput: FileOutputStream,
        writeLock: Any
    ) {
        val selectedProvider = container.preferences.safeDnsProvider.value
        val resolverList = listOf(
            selectedProvider.primaryIp,
            selectedProvider.secondaryIp,
            "185.228.168.168",
            "185.228.168.10",
            "208.67.222.123",
            "76.76.2.4",
            "1.1.1.3"
        ).distinct()

        for (resolverIp in resolverList) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                if (!protect(socket)) {
                    Log.w(TAG, "Failed to protect upstream DNS socket.")
                }
                socket.soTimeout = 2500
                val upstreamAddr = InetAddress.getByName(resolverIp)
                val outPacket = DatagramPacket(query.dnsPayload, query.dnsPayload.size, upstreamAddr, 53)
                socket.send(outPacket)

                val respBuf = ByteArray(2048)
                val respPacket = DatagramPacket(respBuf, respBuf.size)
                socket.receive(respPacket)

                val dnsResponse = respBuf.copyOfRange(0, respPacket.length)
                val ipPacket = DnsPacketHelper.buildIpv4UdpResponsePacket(query, dnsResponse)
                synchronized(writeLock) {
                    tunOutput.write(ipPacket)
                }
                return
            } catch (_: Exception) {
                // Try fallback family safe resolver
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    @Synchronized
    private fun stopVpnTunnel() {
        isRunningInternal = false
        _isTunnelActive.value = false
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        workerThread?.interrupt()
        workerThread = null
        upstreamPool?.shutdownNow()
        upstreamPool = null
    }

    override fun onRevoke() {
        super.onRevoke()
        Log.w(TAG, "Safe DNS VPN revoked by user/system.")
        stopVpnTunnel()
        stopSelf()
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AegisSafeDnsVpn"
        private const val CHANNEL_ID = "aegis_safe_dns_channel"
        private const val NOTIFICATION_ID = 1009

        private const val VPN_CLIENT_ADDRESS = "10.111.222.1"
        private const val DNS_TUNNEL_ADDRESS = "10.111.222.2"
        private val UPSTREAM_SAFE_DNS_SERVERS = listOf("1.1.1.3", "185.228.168.168")

        const val ACTION_START = "com.example.action.START_SAFE_DNS"
        const val ACTION_STOP = "com.example.action.STOP_SAFE_DNS"

        private val _isTunnelActive = MutableStateFlow(false)
        val isTunnelActive: StateFlow<Boolean> = _isTunnelActive.asStateFlow()

        private val _blockedQueryCount = MutableStateFlow(0L)
        val blockedQueryCount: StateFlow<Long> = _blockedQueryCount.asStateFlow()

        private val _lastBlockedDomain = MutableStateFlow<String?>(null)
        val lastBlockedDomain: StateFlow<String?> = _lastBlockedDomain.asStateFlow()

        fun start(context: Context) {
            if (!AegisContainer.getInstance(context).preferences.masterPowerEnabled.value) return
            val intent = Intent(context, AegisSafeDnsVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val appContext = context.applicationContext
            if (_isTunnelActive.value) {
                val intent = Intent(appContext, AegisSafeDnsVpnService::class.java).apply {
                    action = ACTION_STOP
                }
                runCatching { appContext.startService(intent) }
            }
            runCatching {
                appContext.stopService(Intent(appContext, AegisSafeDnsVpnService::class.java))
            }
        }
    }
}
