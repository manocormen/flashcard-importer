package com.manocormen.flashcardimporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                    // Stabilize mutable value to keep compiler happy (ImportScreen can't take null)
                    val content = scannedContent

                    if (content == null) {
                        ScanScreen(
                            onScanClick = {
                                scanner
                                    .startScan()
                                    .addOnSuccessListener { qrcode ->
                                        scannedContent = qrcode.rawValue
                                    }.addOnFailureListener { exception ->
                                        // TODO: Remove when this is fixed:
                                        // https://issuetracker.google.com/issues/461717098
                                        if (exception is MlKitException && exception.errorCode == MlKitException.INTERNAL) {
                                            return@addOnFailureListener
                                        }

                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(scanFailureMessage)
                                        }
                                    }
                            },
                            modifier = Modifier.padding(innerPadding),
                        )
                    } else {
                        ImportScreen(
                            scannedContent = content,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanScreen(
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scanInstructionsHtml = stringResource(R.string.scan_instructions)
    val scanInstructions =
        remember(scanInstructionsHtml) {
            AnnotatedString.fromHtml(scanInstructionsHtml)
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.scan_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = scanInstructions,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onScanClick) {
            Text(text = stringResource(R.string.scan_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    FlashcardImporterTheme {
        ScanScreen(onScanClick = {})
    }
}

@Composable
fun ImportScreen(
    scannedContent: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.scan_result_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(24.dp),
        )
        Text(
            text = scannedContent,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ImportScreenPreview() {
    FlashcardImporterTheme {
        ImportScreen(scannedContent = "http://0.0.0.0:7860/this_is_an_example_endpoint")
    }
}
