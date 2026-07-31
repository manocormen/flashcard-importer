package com.manocormen.flashcardimporter

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val importViewModel by viewModels<ImportViewModel>()

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
            val importState = importViewModel.state

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val importFailureMessage = stringResource(R.string.import_failure)

            fun showImportFailure() {
                importViewModel.reset()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(importFailureMessage)
                }
            }

            fun startScan() {
                scanner
                    .startScan()
                    .addOnSuccessListener { qrcode ->
                        if (isDestroyed) {
                            return@addOnSuccessListener
                        }

                        importViewModel.importCards(qrcode.rawValue)
                    }.addOnFailureListener { exception ->
                        if (isDestroyed) {
                            return@addOnFailureListener
                        }

                        // TODO: Remove when this is fixed:
                        // https://issuetracker.google.com/issues/461717098
                        if (exception is MlKitException && exception.errorCode == MlKitException.INTERNAL) {
                            return@addOnFailureListener
                        }

                        showImportFailure()
                    }
            }

            val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        startScan()
                    } else {
                        showImportFailure()
                    }
                }

            fun scanCards() {
                // Pre-Cinnamon, this came with the INTERNET permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                } else {
                    startScan()
                }
            }

            LaunchedEffect(importState) {
                if (importState == ImportState.Failure) {
                    showImportFailure()
                }
            }

            BackHandler(importState != ImportState.Initial) {
                importViewModel.reset()
            }

            FlashcardImporterTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    when (importState) {
                        ImportState.Initial,
                        ImportState.Failure,
                        ->
                            ScanScreen(
                                onScanClick = ::scanCards,
                                modifier = Modifier.padding(innerPadding),
                            )

                        ImportState.Fetching ->
                            ImportScreen(
                                cards = null,
                                onDiscard = importViewModel::discardCard,
                                modifier = Modifier.padding(innerPadding),
                            )

                        is ImportState.Success ->
                            ImportScreen(
                                cards = importState.cards,
                                onDiscard = importViewModel::discardCard,
                                modifier = Modifier.padding(innerPadding),
                            )
                    }
                }
            }
        }
    }
}
