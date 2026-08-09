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

sealed interface ExportState {
    data class ChoosingDeck(
        val cards: List<BasicCard>,
        val deckList: DeckList? = null,
    ) : ExportState

    object Failure : ExportState
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

        exportJob =
            viewModelScope.launch {
                val result =
                    try {
                        export.copy(deckList = fetchDecks(api))
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        ExportState.Failure
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

    fun reset() {
        exportJob?.cancel()
        state = null
    }
}
