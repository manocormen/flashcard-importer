package com.manocormen.flashcardimporter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme

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
