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
        val ankiDroidApi = AddContentApi(applicationContext)

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
            val exportFailureMessage =
                stringResource(
                    when ((exportState as? ExportState.Failure)?.reason) {
                        ExportFailureReason.DECKS_UNAVAILABLE -> R.string.export_failure_decks_unavailable
                        ExportFailureReason.ADD_CARDS_FAILED -> R.string.export_failure_add_cards
                        null -> R.string.export_failure
                    },
                )
            val exportSuccessMessage = stringResource(R.string.export_success)

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

            fun showExportSuccess() {
                exportViewModel.reset()
                importViewModel.reset()
                showSnackbar(exportSuccessMessage)
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
                            exportViewModel.loadDecks(ankiDroidApi)
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
                when (exportState) {
                    is ExportState.Failure -> showExportFailure()
                    ExportState.Success -> showExportSuccess()
                    else -> Unit
                }
            }

            BackHandler(importState != ImportState.Initial || exportState != null) {
                when (exportState) {
                    is ExportState.ChoosingDeck -> exportViewModel.reset() // Go to cards list
                    null -> importViewModel.reset() // Go to initial screen
                    ExportState.PushingCards,
                    ExportState.Success,
                    is ExportState.Failure,
                    -> Unit
                }
            }

            FlashcardImporterTheme {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { innerPadding ->
                    if (exportState != null) {
                        ExportScreen(
                            deckList = (exportState as? ExportState.ChoosingDeck)?.deckList,
                            onDeckSelected = exportViewModel::selectDeck,
                            onPush = { exportViewModel.pushCards(ankiDroidApi) },
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
