package com.manocormen.flashcardimporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
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
        importJob?.cancel()

        if (endpoint == null || !isValidCardsEndpoint(endpoint)) {
            state = ImportState.Failure
            return
        }

        state = ImportState.Fetching
        importJob =
            viewModelScope.launch {
                val result =
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

                // A cancelled import (re-scan or reset) must not overwrite newer
                // state: a request that throws after cancellation would otherwise
                // reach the catch arms and clobber the current state with Failure.
                if (isActive) {
                    state = result
                }
            }
    }

    fun discardCard(id: Int) {
        val currentState = state as? ImportState.Success ?: return
        val remaining = currentState.cards.filterNot { it.id == id }
        state =
            if (remaining.isEmpty()) {
                ImportState.Initial // Nothing left to review; return to the scan screen
            } else {
                ImportState.Success(remaining)
            }
    }

    fun reset() {
        importJob?.cancel()
        state = ImportState.Initial
    }
}
