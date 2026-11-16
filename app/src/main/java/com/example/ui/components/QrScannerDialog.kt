package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AegisCardBorder
import com.example.ui.theme.AegisChampagneSoft
import com.example.ui.theme.AegisLavenderBorder
import com.example.ui.theme.AegisLavenderSoft
import com.example.ui.theme.AegisOnPrimaryWhite
import com.example.ui.theme.AegisPrimaryCyan
import com.example.ui.theme.AegisPurple
import com.example.ui.theme.AegisTextPrimary
import com.example.ui.theme.AegisTextSecondary
import com.example.ui.theme.ClassicalBodyFont
import com.example.ui.theme.ClassicalDisplayFont
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-app CameraX + ZXing QR Code Scanner dialog for one-tap Parent-Child pairing.
 */
@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit,
    onBeforePermissionRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            permissionDeniedOnce = true
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            onBeforePermissionRequest()
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
                .padding(horizontal = 20.dp)
                .widthIn(max = 520.dp)
                .border(1.5.dp, AegisLavenderBorder, RoundedCornerShape(22.dp))
                .testTag("qr_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AegisPrimaryCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = AegisPrimaryCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Scan Child QR Code",
                                color = AegisTextPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ClassicalDisplayFont
                            )
                            Text(
                                text = "Align pairing QR inside the frame",
                                color = AegisTextSecondary,
                                fontSize = 11.5.sp,
                                fontFamily = ClassicalBodyFont
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("qr_scanner_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close scanner",
                            tint = AegisTextPrimary
                        )
                    }
                }

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .border(2.dp, AegisPrimaryCyan.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    ) {
                        CameraQrPreview(
                            onQrDecoded = { decoded ->
                                onCodeScanned(decoded)
                            }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.68f)
                                .aspectRatio(1f)
                                .border(2.dp, AegisOnPrimaryWhite.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AegisChampagneSoft.copy(alpha = 0.75f))
                            .border(1.dp, AegisCardBorder, RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = AegisPurple,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (permissionDeniedOnce) {
                                "Camera access is needed to scan the Child device's QR code. Please allow camera permission or enter the 6-digit code manually."
                            } else {
                                "Requesting Camera permission to scan the Child pairing QR code..."
                            },
                            color = AegisTextPrimary,
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = ClassicalBodyFont
                        )
                        Button(
                            onClick = {
                                onBeforePermissionRequest()
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisPrimaryCyan,
                                contentColor = AegisOnPrimaryWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("qr_grant_camera_button")
                        ) {
                            Text("GRANT CAMERA ACCESS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("ENTER CODE MANUALLY INSTEAD", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CameraQrPreview(
    onQrDecoded: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val handled = remember { AtomicBoolean(false) }
    val qrReader = remember {
        QRCodeReader()
    }
    val hints = remember {
        mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            @Suppress("DEPRECATION")
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(960, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (handled.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val text = decodeQrFromImageProxy(qrReader, hints, imageProxy)
                imageProxy.close()
                if (!text.isNullOrBlank() && handled.compareAndSet(false, true)) {
                    mainExecutor.execute {
                        onQrDecoded(text)
                    }
                }
            }

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
        }, mainExecutor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
            cameraExecutor.shutdownNow()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private fun decodeQrFromImageProxy(
    reader: QRCodeReader,
    hints: Map<DecodeHintType, Any>,
    imageProxy: ImageProxy
): String? {
    val yPlane = imageProxy.planes.firstOrNull() ?: return null
    val width = imageProxy.width
    val height = imageProxy.height
    if (width <= 0 || height <= 0) return null

    val rowStride = yPlane.rowStride
    val buffer = yPlane.buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)

    val normalized = if (rowStride == width && data.size >= width * height) {
        data
    } else {
        val tight = ByteArray(width * height)
        for (row in 0 until height) {
            val srcOffset = row * rowStride
            if (srcOffset + width <= data.size) {
                System.arraycopy(data, srcOffset, tight, row * width, width)
            }
        }
        tight
    }

    return try {
        val source = PlanarYUVLuminanceSource(
            normalized,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        reader.decode(bitmap, hints).text
    } catch (_: Exception) {
        // Fallback: rotate 90 deg for portrait-oriented sensors if direct decode missed
        try {
            val rotated = ByteArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    rotated[x * height + (height - y - 1)] = normalized[y * width + x]
                }
            }
            val rotatedSource = PlanarYUVLuminanceSource(
                rotated,
                height,
                width,
                0,
                0,
                height,
                width,
                false
            )
            reader.decode(BinaryBitmap(HybridBinarizer(rotatedSource)), hints).text
        } catch (_: Exception) {
            null
        }
    } finally {
        reader.reset()
    }
}
