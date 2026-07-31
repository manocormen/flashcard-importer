package com.manocormen.flashcardimporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manocormen.flashcardimporter.ui.theme.FlashcardImporterTheme
import com.mikepenz.markdown.m3.Markdown

@Composable
fun ImportScreen(
    cards: List<WrappedCard>?,
    onDiscard: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        CardList(
            cards = cards,
            onDiscard = onDiscard,
            modifier = modifier,
        )
    }
}

@Composable
private fun CardList(
    cards: List<WrappedCard>,
    onDiscard: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = cards,
            key = { it.id },
        ) { wrappedCard ->
            CardItem(
                card = wrappedCard.card,
                onDiscard = { onDiscard(wrappedCard.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CardItem(
    card: BasicCard,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer, // Revealed on discard
                            shape = MaterialTheme.shapes.medium, // Rounded corners to match card
                        ).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.discard_card),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = false, // One direction to discard
        onDismiss = { onDiscard() },
    ) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Markdown(card.front)
                HorizontalDivider()
                Markdown(card.back)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ImportScreenPreview() {
    FlashcardImporterTheme {
        ImportScreen(
            cards =
                listOf(
                    BasicCard(
                        front = "hello",
                        back = "there",
                    ),
                    BasicCard(
                        front =
                            """
                            * **hello**
                            * *there*
                            """.trimIndent(),
                        back =
                            """
                            - `there`
                            - ~~hello~~
                            """.trimIndent(),
                    ),
                ).mapIndexed { index, card -> WrappedCard(index, card) },
            onDiscard = {},
        )
    }
}
