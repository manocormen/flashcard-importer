package com.manocormen.flashcardimporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class WrappedCard(
    val id: Int, // Needed for swipe-to-discard
    val card: BasicCard,
)

sealed interface ImportState {
    object Initial : ImportState

    object Fetching : ImportState

    class Success(
        val cards: List<WrappedCard>,
    ) : ImportState

    object Failure : ImportState
}

class ImportViewModel : ViewModel() {
    var state by mutableStateOf<ImportState>(ImportState.Initial)
        private set

    private var importJob: Job? = null

    fun importCards(endpoint: String?) {
        if (endpoint == null || !isValidCardsEndpoint(endpoint)) {
            state = ImportState.Failure
            return
        }

        state = ImportState.Fetching
        importJob =
            viewModelScope.launch {
                state =
                    try {
                        ImportState.Success(
                            fetchCards(endpoint)
                                .mapIndexed { index, card -> WrappedCard(index, card) },
                        )
                    } catch (_: IOException) {
                        ImportState.Failure
                    } catch (_: IllegalArgumentException) {
                        // For json decoding or endpoint issues
                        ImportState.Failure
                    }
            }
    }

    fun discardCard(id: Int) {
        val currentState = state as ImportState.Success // Stabilize state for compiler
        state = ImportState.Success(currentState.cards.filterNot { it.id == id })
    }

    fun reset() {
        importJob?.cancel()
        state = ImportState.Initial
    }
}
