package com.manocormen.flashcardimporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val scannerOptions =
            GmsBarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
        val scanner = GmsBarcodeScanning.getClient(this, scannerOptions)

        setContent {
            var scannedContent by rememberSaveable { mutableStateOf<String?>(null) }

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val scanFailureMessage = stringResource(R.string.scan_failure)

            fun showScanFailure() {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(scanFailureMessage)
                }
            }

            BackHandler(scannedContent != null) {
                scannedContent = null
            }

            FlashcardImporterTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                ) { innerPadding ->
                    // Stabilize mutable value for the compiler (ScanResultScreen can't take null)
                    val content = scannedContent

                    if (content == null) {
                        ScanScreen(
                            onScanClick = {
                                scanner
                                    .startScan()
                                    .addOnSuccessListener { qrcode ->
                                        if (isDestroyed) {
                                            return@addOnSuccessListener
                                        }

                                        val rawValue = qrcode.rawValue
                                        if (rawValue == null) {
                                            showScanFailure()
                                            return@addOnSuccessListener
                                        }

                                        scannedContent = rawValue
                                    }.addOnFailureListener { exception ->
                                        if (isDestroyed) {
                                            return@addOnFailureListener
                                        }

                                        // TODO: Remove when this is fixed:
                                        // https://issuetracker.google.com/issues/461717098
                                        if (exception is MlKitException && exception.errorCode == MlKitException.INTERNAL) {
                                            return@addOnFailureListener
                                        }

                                        showScanFailure()
                                    }
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        ScanResultScreen(
                            scannedContent = content,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
