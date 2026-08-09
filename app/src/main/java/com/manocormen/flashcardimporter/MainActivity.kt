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
import com.ichi2.anki.api.AddContentApi
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val importViewModel by viewModels<ImportViewModel>()
    private val exportViewModel by viewModels<ExportViewModel>()

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
            val exportState = exportViewModel.state

            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val importFailureMessage =
                stringResource(
                    when ((importState as? ImportState.Failure)?.reason) {
                        ImportFailureReason.INVALID_ENDPOINT -> R.string.import_failure_invalid_endpoint
                        ImportFailureReason.CONNECTION_FAILED -> R.string.import_failure_connection_failed
                        ImportFailureReason.INVALID_RESPONSE -> R.string.import_failure_invalid_response
                        ImportFailureReason.UNEXPECTED -> R.string.import_failure_unexpected
                        null -> R.string.import_failure
                    },
                )
            val exportFailureMessage = stringResource(R.string.export_failure)

            fun showSnackbar(message: String) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }

            fun showImportFailure() {
                importViewModel.reset()
                showSnackbar(importFailureMessage)
            }

            fun showExportFailure() {
                exportViewModel.reset()
                showSnackbar(exportFailureMessage)
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

            val networkPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        startScan()
                    } else {
                        showImportFailure()
                    }
                }

            val ankiDroidPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (exportViewModel.state is ExportState.ChoosingDeck) {
                        if (isGranted) {
                            exportViewModel.loadDecks(AddContentApi(applicationContext))
                        } else {
                            showExportFailure()
                        }
                    }
                }

            fun startExport(cards: List<WrappedCard>) {
                exportViewModel.startExport(cards.map { it.card })
                ankiDroidPermissionLauncher.launch(AddContentApi.READ_WRITE_PERMISSION)
            }

            fun scanCards() {
                // Pre-Cinnamon, this came with the INTERNET permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
                    networkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                } else {
                    startScan()
                }
            }

            LaunchedEffect(importState) {
                if (importState is ImportState.Failure) {
                    showImportFailure()
                }
            }

            LaunchedEffect(exportState) {
                if (exportState == ExportState.Failure) {
                    showExportFailure()
                }
            }

            BackHandler(importState != ImportState.Initial || exportState != null) {
                if (exportState != null) {
                    exportViewModel.reset()
                } else {
                    importViewModel.reset()
                }
            }

            FlashcardImporterTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    if (exportState is ExportState.ChoosingDeck) {
                        ExportScreen(
                            deckList = exportState.deckList,
                            onDeckSelected = exportViewModel::selectDeck,
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        when (importState) {
                            ImportState.Initial,
                            is ImportState.Failure,
                            ->
                                ScanScreen(
                                    onScanClick = ::scanCards,
                                    modifier = Modifier.padding(innerPadding),
                                )

                            ImportState.Fetching ->
                                ImportScreen(
                                    cards = null,
                                    onDiscard = importViewModel::discardCard,
                                    onExport = {},
                                    modifier = Modifier.padding(innerPadding),
                                )

                            is ImportState.Success ->
                                ImportScreen(
                                    cards = importState.cards,
                                    onDiscard = importViewModel::discardCard,
                                    onExport = { startExport(importState.cards) },
                                    modifier = Modifier.padding(innerPadding),
                                )
                        }
                    }
                }
            }
        }
    }
}
