package com.manocormen.flashcardimporter

import com.ichi2.anki.api.AddContentApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

private const val BASIC_NOTE_TYPE_NAME = "Flashcard Importer"

private val markdownFlavour = GFMFlavourDescriptor()
private val markdownParser = MarkdownParser(markdownFlavour)
private val htmlSafelist = Safelist.relaxed() // Keep common md such as headings

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

internal fun markdownToHtml(markdown: String): String {
    val document = markdownParser.buildMarkdownTreeFromString(markdown)

    val html =
        HtmlGenerator(markdown, document, markdownFlavour)
            .generateHtml()
            .removeSurrounding("<body>", "</body>") // Not needed by AnkiDroid

    return Jsoup.clean(html, htmlSafelist).trimEnd()
}

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
): Int =
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

        api.addNotes(
            noteTypeId,
            deckId.value,
            cards.map { card ->
                arrayOf(
                    markdownToHtml(card.front),
                    markdownToHtml(card.back),
                )
            },
            null,
        )
    }
