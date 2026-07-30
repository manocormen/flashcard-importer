package com.manocormen.flashcardimporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface ImportState {
    object Initial : ImportState

    object Fetching : ImportState

    class Success(
        val cards: List<BasicCard>,
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
                        ImportState.Success(fetchCards(endpoint))
                    } catch (_: IOException) {
                        ImportState.Failure
                    } catch (_: IllegalArgumentException) {
                        // For json decoding or endpoint issues
                        ImportState.Failure
                    }
            }
    }

    fun reset() {
        importJob?.cancel()
        state = ImportState.Initial
    }
}
