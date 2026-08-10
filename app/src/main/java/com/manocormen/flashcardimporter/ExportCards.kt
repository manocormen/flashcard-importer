package com.manocormen.flashcardimporter

import com.ichi2.anki.api.AddContentApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BASIC_NOTE_TYPE_NAME = "Flashcard Importer"

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

internal suspend fun addCards(
    api: AddContentApi,
    deckId: DeckId,
    cards: List<BasicCard>,
): Unit =
    withContext(Dispatchers.IO) {
        val noteTypes = checkNotNull(api.getModelList())

        val existingNoteTypeId =
            noteTypes.entries
                .firstOrNull { (_, name) -> name == BASIC_NOTE_TYPE_NAME }
                ?.key
        val noteTypeId =
            if (existingNoteTypeId == null) {
                checkNotNull(api.addNewBasicModel(BASIC_NOTE_TYPE_NAME))
            } else {
                // Reuse our note type if it already exists and still has the right shape
                check(api.getFieldList(existingNoteTypeId)?.size == 2)
                existingNoteTypeId
            }

        val addedCardCount =
            api.addNotes(
                noteTypeId,
                deckId.value,
                cards.map { card -> arrayOf(card.front, card.back) },
                null,
            )

        check(addedCardCount == cards.size) // Success only if all cards are added
    }
