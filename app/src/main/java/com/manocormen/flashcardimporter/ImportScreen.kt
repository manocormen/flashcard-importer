package com.manocormen.flashcardimporter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme

@Composable
fun ImportScreen(
    cardCount: Int?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (cardCount == null) {
            CircularProgressIndicator()
        } else {
            Text(
                text = stringResource(R.string.import_card_count, cardCount),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImportScreenPreview() {
    FlashcardImporterTheme {
        ImportScreen(cardCount = 12)
    }
}
