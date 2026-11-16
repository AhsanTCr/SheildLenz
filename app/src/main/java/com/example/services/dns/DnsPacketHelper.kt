package com.example.services.dns

import java.util.Locale

data class ParsedDnsQuery(
    val transactionId: Int,
    val domain: String,
    val queryType: Int,
    val srcIp: ByteArray,
    val dstIp: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val dnsPayload: ByteArray
)

object DnsPacketHelper {

    const val DNS_TYPE_A = 1
    const val DNS_TYPE_AAAA = 28

    private val ADULT_TLDS = setOf("xxx", "porn", "sex", "adult")

    private val BUILTIN_ADULT_DOMAINS = setOf(
        "pornhub.com",
        "xvideos.com",
        "xnxx.com",
        "xhamster.com",
        "redtube.com",
        "youporn.com",
        "tube8.com",
        "spankbang.com",
        "chaturbate.com",
        "stripchat.com",
        "livejasmin.com",
        "cam4.com",
        "myfreecams.com",
        "bongacams.com",
        "onlyfans.com",
        "fansly.com",
        "brazzers.com",
        "beeg.com",
        "eporner.com",
        "txxx.com",
        "hqporner.com",
        "motherless.com",
        "rule34.xxx",
        "nhentai.net",
        "hanime.tv",
        "erome.com",
        "youjizz.com",
        "tnaflix.com",
        "fapello.com",
        "coomer.su",
        "kemono.su"
    )

    private val BUILTIN_GAMBLING_DOMAINS = setOf(
        "bet365.com",
        "draftkings.com",
        "fanduel.com",
        "betway.com",
        "pokerstars.com",
        "888casino.com",
        "stake.com",
        "roobet.com",
        "1xbet.com",
        "betfair.com",
        "bovada.lv",
        "williamhill.com",
        "unibet.com",
        "ladbrokes.com",
        "caesarscasino.com"
    )

    fun parseIpv4UdpDnsPacket(packet: ByteArray, length: Int): ParsedDnsQuery? {
        if (length < 28 || length > packet.size) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null

        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl + 8 + 12) return null

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // UDP

        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)

        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return null

        val udpLength = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
        val dnsOffset = ihl + 8
        val availableDnsLen = minOf(length - dnsOffset, udpLength - 8)
        if (availableDnsLen < 12) return null

        val dnsPayload = packet.copyOfRange(dnsOffset, dnsOffset + availableDnsLen)
        val qrBit = (dnsPayload[2].toInt() ushr 7) and 0x01
        if (qrBit != 0) return null // Not a query

        val qdCount = ((dnsPayload[4].toInt() and 0xFF) shl 8) or (dnsPayload[5].toInt() and 0xFF)
        if (qdCount < 1) return null

        val (domain, nextOffset) = readQName(dnsPayload, 12) ?: return null
        if (nextOffset + 4 > dnsPayload.size) return null
        val qType = ((dnsPayload[nextOffset].toInt() and 0xFF) shl 8) or (dnsPayload[nextOffset + 1].toInt() and 0xFF)
        val txId = ((dnsPayload[0].toInt() and 0xFF) shl 8) or (dnsPayload[1].toInt() and 0xFF)

        return ParsedDnsQuery(
            transactionId = txId,
            domain = domain.lowercase(Locale.US),
            queryType = qType,
            srcIp = srcIp,
            dstIp = dstIp,
            srcPort = srcPort,
            dstPort = dstPort,
            dnsPayload = dnsPayload
        )
    }

    private fun readQName(dns: ByteArray, startOffset: Int): Pair<String, Int>? {
        var idx = startOffset
        val sb = StringBuilder()
        var safety = 0
        while (idx < dns.size && safety < 64) {
            safety++
            val labelLen = dns[idx].toInt() and 0xFF
            idx++
            if (labelLen == 0) {
                return if (sb.isNotEmpty()) sb.toString() to idx else null
            }
            if ((labelLen and 0xC0) != 0) {
                // Compressed pointer not expected in standard single question
                return null
            }
            if (idx + labelLen > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until labelLen) {
                sb.append((dns[idx + i].toInt() and 0xFF).toChar())
            }
            idx += labelLen
        }
        return null
    }

    fun matchesBlockedDomain(
        rawDomain: String,
        customDomains: Set<String> = emptySet(),
        customKeywords: Set<String> = emptySet(),
        blockExplicitAdult: Boolean = true,
        blockGambling: Boolean = true
    ): String? {
        val domain = rawDomain.trim().trimEnd('.').lowercase(Locale.US)
        if (domain.isBlank()) return null

        for (custom in customDomains) {
            val clean = custom.trim().trimEnd('.').lowercase(Locale.US)
            if (clean.isNotBlank() && isDomainOrSubdomain(domain, clean)) {
                return "Custom Blocked Domain ($clean)"
            }
        }

        for (kw in customKeywords) {
            val cleanKw = kw.trim().lowercase(Locale.US)
            if (cleanKw.length >= 2 && domain.contains(cleanKw)) {
                return "Custom Keyword Block ($cleanKw)"
            }
        }

        if (blockExplicitAdult) {
            val tld = domain.substringAfterLast('.', "")
            if (tld in ADULT_TLDS) {
                return "Blocked Adult TLD (.$tld)"
            }
            for (adultHost in BUILTIN_ADULT_DOMAINS) {
                if (isDomainOrSubdomain(domain, adultHost)) {
                    return "Safe DNS Adult Block ($adultHost)"
                }
            }
        }

        if (blockGambling) {
            for (gambleHost in BUILTIN_GAMBLING_DOMAINS) {
                if (isDomainOrSubdomain(domain, gambleHost)) {
                    return "Safe DNS Gambling Block ($gambleHost)"
                }
            }
        }

        return null
    }

    private fun isDomainOrSubdomain(candidate: String, rootHost: String): Boolean {
        return candidate == rootHost || candidate.endsWith(".$rootHost")
    }

    fun buildBlockedDnsResponsePayload(queryPayload: ByteArray): ByteArray? {
        if (queryPayload.size < 12) return null
        val (_, afterQName) = readQName(queryPayload, 12) ?: return null
        val questionEnd = afterQName + 4
        if (questionEnd > queryPayload.size) return null

        val qType = ((queryPayload[afterQName].toInt() and 0xFF) shl 8) or
            (queryPayload[afterQName + 1].toInt() and 0xFF)
        val qClass = ((queryPayload[afterQName + 2].toInt() and 0xFF) shl 8) or
            (queryPayload[afterQName + 3].toInt() and 0xFF)

        val rdLength = when (qType) {
            DNS_TYPE_A -> 4
            DNS_TYPE_AAAA -> 16
            else -> 0
        }

        // Question length without any EDNS0 OPT additional bytes
        val headerAndQuestion = ByteArray(questionEnd)
        System.arraycopy(queryPayload, 0, headerAndQuestion, 0, questionEnd)

        // Transaction ID preserved at bytes 0..1
        // Flags: QR=1, Opcode=0, AA=0, TC=0, RD=1, RA=1
        headerAndQuestion[2] = 0x81.toByte()
        headerAndQuestion[3] = if (rdLength > 0) 0x80.toByte() else 0x83.toByte() // NOERROR or NXDOMAIN
        // QDCOUNT = 1
        headerAndQuestion[4] = 0x00
        headerAndQuestion[5] = 0x01
        // ANCOUNT = 1 if A/AAAA else 0
        headerAndQuestion[6] = 0x00
        headerAndQuestion[7] = if (rdLength > 0) 0x01 else 0x00
        // NSCOUNT = 0, ARCOUNT = 0
        headerAndQuestion[8] = 0x00
        headerAndQuestion[9] = 0x00
        headerAndQuestion[10] = 0x00
        headerAndQuestion[11] = 0x00

        if (rdLength == 0) {
            return headerAndQuestion
        }

        val answerLen = 12 + rdLength
        val out = ByteArray(headerAndQuestion.size + answerLen)
        System.arraycopy(headerAndQuestion, 0, out, 0, headerAndQuestion.size)

        var pos = headerAndQuestion.size
        // Name pointer 0xC00C (offset 12)
        out[pos++] = 0xC0.toByte()
        out[pos++] = 0x0C.toByte()
        // TYPE
        out[pos++] = ((qType ushr 8) and 0xFF).toByte()
        out[pos++] = (qType and 0xFF).toByte()
        // CLASS
        out[pos++] = ((qClass ushr 8) and 0xFF).toByte()
        out[pos++] = (qClass and 0xFF).toByte()
        // TTL = 60 seconds
        out[pos++] = 0x00
        out[pos++] = 0x00
        out[pos++] = 0x00
        out[pos++] = 0x3C
        // RDLENGTH
        out[pos++] = 0x00
        out[pos++] = rdLength.toByte()
        // RDATA all zeros (0.0.0.0 or ::) already zero-initialized
        return out
    }

    fun buildIpv4UdpResponsePacket(query: ParsedDnsQuery, responseDnsPayload: ByteArray): ByteArray {
        val ipHeaderLen = 20
        val udpLen = 8 + responseDnsPayload.size
        val totalLen = ipHeaderLen + udpLen
        val out = ByteArray(totalLen)

        // IPv4 Header
        out[0] = 0x45 // Version 4, IHL 5
        out[1] = 0x00
        out[2] = ((totalLen ushr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[4] = ((query.transactionId ushr 8) and 0xFF).toByte()
        out[5] = (query.transactionId and 0xFF).toByte()
        out[6] = 0x40 // Don't fragment
        out[7] = 0x00
        out[8] = 64 // TTL
        out[9] = 17 // UDP
        // Checksum [10..11] filled below
        // Swap src <-> dst IP
        System.arraycopy(query.dstIp, 0, out, 12, 4)
        System.arraycopy(query.srcIp, 0, out, 16, 4)

        val ipChecksum = computeChecksum(out, 0, ipHeaderLen)
        out[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
        out[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header (swap srcPort <-> dstPort)
        out[20] = ((query.dstPort ushr 8) and 0xFF).toByte()
        out[21] = (query.dstPort and 0xFF).toByte()
        out[22] = ((query.srcPort ushr 8) and 0xFF).toByte()
        out[23] = (query.srcPort and 0xFF).toByte()
        out[24] = ((udpLen ushr 8) and 0xFF).toByte()
        out[25] = (udpLen and 0xFF).toByte()
        out[26] = 0x00
        out[27] = 0x00

        System.arraycopy(responseDnsPayload, 0, out, 28, responseDnsPayload.size)

        val udpChecksum = computeUdpChecksum(
            srcIp = query.dstIp,
            dstIp = query.srcIp,
            udpSegment = out,
            udpOffset = 20,
            udpLen = udpLen
        )
        out[26] = ((udpChecksum ushr 8) and 0xFF).toByte()
        out[27] = (udpChecksum and 0xFF).toByte()

        return out
    }

    private fun computeChecksum(buf: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += (((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF))
            i += 2
        }
        if (i < end) {
            sum += (buf[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun computeUdpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpSegment: ByteArray,
        udpOffset: Int,
        udpLen: Int
    ): Int {
        var sum = 0L
        for (i in 0 until 4 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }
        sum += 17L // UDP protocol
        sum += udpLen.toLong()

        var i = udpOffset
        val end = udpOffset + udpLen
        while (i + 1 < end) {
            sum += (((udpSegment[i].toInt() and 0xFF) shl 8) or (udpSegment[i + 1].toInt() and 0xFF))
            i += 2
        }
        if (i < end) {
            sum += (udpSegment[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = (sum.inv() and 0xFFFF).toInt()
        return if (result == 0) 0xFFFF else result
    }
}
