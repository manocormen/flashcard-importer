package com.manocormen.flashcardimporter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme

@Composable
fun ExportScreen(
    deckList: DeckList?,
    onDeckSelected: (DeckId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (deckList == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.export_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        items(deckList.decks) { deck ->
            val selected = deck.id == deckList.selectedDeckId
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDeckSelected(deck.id) }
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                )
                Text(
                    text = deck.name,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExportScreenPreview() {
    FlashcardImporterTheme {
        ExportScreen(
            deckList =
                DeckList(
                    decks =
                        listOf(
                            Deck(DeckId(1L), "Mnemonics"),
                            Deck(DeckId(2L), "Hello::There"),
                        ),
                    selectedDeckId = DeckId(2L),
                ),
            onDeckSelected = {},
        )
    }
}
