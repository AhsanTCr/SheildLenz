package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.AegisPreferences
import com.example.security.deviceowner.AegisDeviceAdminReceiver
import com.example.ui.components.StatusPill
import com.example.ui.theme.AegisCardBackground
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneGold
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisGoldBorder
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisSurfaceElevated
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.AegisWarningAmber
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont

/**
 * Guided step-by-step Device Owner Provisioning Wizard for maximum tamper-resistant protection.
 */
@Composable
fun DeviceOwnerWizardDialog(
    preferences: AegisPreferences,
    onDismiss: () -> Unit,
    onStatusChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    var isDeviceAdmin by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceAdmin(context)) }
    var isDeviceOwner by remember { mutableStateOf(AegisDeviceAdminReceiver.isDeviceOwner(context)) }
    val adbCommand = remember(context.packageName) {
        "adb shell dpm set-device-owner ${context.packageName}/${AegisDeviceAdminReceiver::class.java.name}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = AegisLavenderSoft,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 560.dp)
                .border(1.5.dp, AegisLavenderBorder, RoundedCornerShape(22.dp))
                .testTag("device_owner_wizard_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AegisPurple.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = AegisPurple,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Guided Device Owner Setup",
                                color = AegisTextPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            Text(
                                text = "Maximum tamper-resistant protection",
                                color = AegisTextSecondary,
                                fontSize = 11.5.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close wizard",
                            tint = AegisTextPrimary
                        )
                    }
                }

                // Live status badges
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill(
                        label = if (isDeviceAdmin) "ADMIN ACTIVE" else "ADMIN INACTIVE",
                        isActive = isDeviceAdmin,
                        activeColor = AegisPrimaryCyan,
                        inactiveColor = AegisWarningAmber
                    )
                    StatusPill(
                        label = if (isDeviceOwner) "DEVICE OWNER" else "STANDARD MODE",
                        isActive = isDeviceOwner,
                        activeColor = AegisPurple,
                        inactiveColor = AegisChampagneGold
                    )
                }

                if (isDeviceOwner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AegisPrimaryCyan.copy(alpha = 0.12f))
                            .border(1.dp, AegisPrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AegisPrimaryCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Device Owner is active! Uninstall blocking and app suspension enforcement are fully armed.",
                            color = AegisTextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            fontFamily = ClassicalBodyFont
                        )
                    }
                }

                // Step 1
                WizardStepCard(
                    stepNumber = "1",
                    title = "Activate Device Administrator",
                    description = "Grants baseline anti-uninstall resistance so Sheildlenz cannot be removed casually from Android Settings."
                ) {
                    Button(
                        onClick = {
                            preferences.grantTemporarySettingsAuthorization()
                            runCatching {
                                context.startActivity(AegisDeviceAdminReceiver.createActivateAdminIntent(context))
                            }
                            isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                            isDeviceOwner = AegisDeviceAdminReceiver.isDeviceOwner(context)
                            onStatusChanged()
                        },
                        enabled = !isDeviceAdmin,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisPrimaryCyan,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wizard_activate_admin_button")
                    ) {
                        Text(
                            text = if (isDeviceAdmin) "DEVICE ADMIN ALREADY ACTIVE" else "ACTIVATE DEVICE ADMIN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Step 2
                WizardStepCard(
                    stepNumber = "2",
                    title = "Temporarily Remove Device Accounts",
                    description = "Android requires no signed-in Google or Work accounts during the one-time Device Owner ADB provisioning command. You can sign back into your accounts immediately after Step 3."
                ) {
                    OutlinedButton(
                        onClick = {
                            preferences.grantTemporarySettingsAuthorization()
                            runCatching {
                                val intent = Intent(Settings.ACTION_SYNC_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("OPEN ANDROID ACCOUNTS SETTINGS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Step 3
                WizardStepCard(
                    stepNumber = "3",
                    title = "Run ADB Provisioning Command on PC",
                    description = "Connect phone via USB debugging and execute the command below once:"
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AegisSurfaceElevated)
                            .border(1.dp, AegisCardBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = adbCommand,
                            color = AegisTextPrimary,
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("Sheildlenz Device Owner ADB", adbCommand))
                            Toast.makeText(context, "ADB Device Owner command copied", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisChampagneGold,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wizard_copy_adb_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY ADB COMMAND", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isDeviceAdmin = AegisDeviceAdminReceiver.isDeviceAdmin(context)
                            isDeviceOwner = AegisDeviceAdminReceiver.isDeviceOwner(context)
                            onStatusChanged()
                            val msg = if (isDeviceOwner) "Device Owner verified!" else "Still in standard mode."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("wizard_verify_status_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VERIFY", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AegisPurple,
                            contentColor = AegisOnPrimaryWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("DONE", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardStepCard(
    stepNumber: String,
    title: String,
    description: String,
    actionContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AegisCardBackground.copy(alpha = 0.9f))
            .border(1.dp, AegisGoldBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AegisChampagneSoft)
                    .border(1.dp, AegisGoldBorder, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = AegisTextPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = AegisTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ClassicalDisplayFont
            )
        }

        Text(
            text = description,
            color = AegisTextSecondary,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            fontFamily = ClassicalBodyFont
        )

        actionContent()
    }
}
