package com.manocormen.flashcardimporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ichi2.anki.api.AddContentApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ExportFailureReason {
    DECKS_UNAVAILABLE,
    ADD_CARDS_FAILED,
}

sealed interface ExportState {
    data class ChoosingDeck(
        val cards: List<BasicCard>,
        val deckList: DeckList? = null,
    ) : ExportState

    object PushingCards : ExportState

    object Success : ExportState

    class Failure(
        val reason: ExportFailureReason,
    ) : ExportState
}

class ExportViewModel : ViewModel() {
    var state by mutableStateOf<ExportState?>(null)
        private set

    private var exportJob: Job? = null

    fun startExport(cards: List<BasicCard>) {
        exportJob?.cancel()
        state = ExportState.ChoosingDeck(cards)
    }

    fun loadDecks(api: AddContentApi) {
        val export = state as? ExportState.ChoosingDeck ?: return

        exportJob?.cancel()
        exportJob =
            viewModelScope.launch {
                val result =
                    try {
                        export.copy(deckList = fetchDecks(api))
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        ExportState.Failure(ExportFailureReason.DECKS_UNAVAILABLE)
                    }

                if (isActive) {
                    state = result
                }
            }
    }

    fun selectDeck(id: DeckId) {
        val export = state as? ExportState.ChoosingDeck ?: return
        val deckList = export.deckList ?: return

        state = export.copy(deckList = deckList.copy(selectedDeckId = id))
    }

    fun pushCards(api: AddContentApi) {
        val export = state as? ExportState.ChoosingDeck ?: return
        val deckList = export.deckList ?: return

        exportJob?.cancel()
        state = ExportState.PushingCards
        exportJob =
            viewModelScope.launch {
                val result =
                    try {
                        addCards(
                            api = api,
                            deckId = deckList.selectedDeckId,
                            cards = export.cards,
                        )
                        ExportState.Success
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        ExportState.Failure(ExportFailureReason.ADD_CARDS_FAILED)
                    }

                if (isActive) {
                    state = result
                }
            }
    }

    fun reset() {
        exportJob?.cancel()
        state = null
    }
}
