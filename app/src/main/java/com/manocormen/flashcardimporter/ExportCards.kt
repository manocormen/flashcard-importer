package com.manocormen.flashcardimporter

import com.ichi2.anki.api.AddContentApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JvmInline
value class DeckId(
    val value: Long,
)

data class Deck(
    val id: DeckId,
    val name: String,
)

data class DeckList(
    val decks: List<Deck>,
    val selectedDeckId: DeckId,
)

internal suspend fun fetchDecks(api: AddContentApi): DeckList =
    withContext(Dispatchers.IO) {
        val decks =
            checkNotNull(api.getDeckList()) { "Couldn't load decks" }
                .map { (id, name) -> Deck(DeckId(id), name) }
                .sortedBy { it.name }

        check(decks.isNotEmpty()) { "No decks found" }

        val selectedDeckName = api.getSelectedDeckName()
        val selectedDeck =
            decks.firstOrNull { it.name == selectedDeckName }
                ?: decks.first()

        DeckList(
            decks = decks,
            selectedDeckId = selectedDeck.id,
        )
    }
