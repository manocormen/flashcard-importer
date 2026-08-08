package com.manocormen.flashcardimporter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException

@JvmInline
value class CardId(
    val value: Int,
)

class WrappedCard(
    val id: CardId, // Needed for swipe-to-discard
    val card: BasicCard,
)

enum class ImportFailureReason {
    INVALID_ENDPOINT,
    CONNECTION_FAILED,
    INVALID_RESPONSE,
    UNEXPECTED,
}

sealed interface ImportState {
    object Initial : ImportState

    object Fetching : ImportState

    class Success(
        val cards: List<WrappedCard>,
    ) : ImportState

    class Failure(
        val reason: ImportFailureReason,
    ) : ImportState
}

class ImportViewModel : ViewModel() {
    var state by mutableStateOf<ImportState>(ImportState.Initial)
        private set

    private var importJob: Job? = null

    fun importCards(endpoint: String?) {
        importJob?.cancel()

        val cardsEndpoint = endpoint?.let(CardsEndpoint::validateOrNull)
        if (cardsEndpoint == null) {
            state = ImportState.Failure(ImportFailureReason.INVALID_ENDPOINT)
            return
        }

        state = ImportState.Fetching
        importJob =
            viewModelScope.launch {
                val result =
                    try {
                        ImportState.Success(
                            fetchCards(cardsEndpoint)
                                .mapIndexed { index, card -> WrappedCard(CardId(index), card) },
                        )
                    } catch (exception: CancellationException) {
                        throw exception // To avoid the catch-all below silencing the cancellation
                    } catch (_: IOException) {
                        ImportState.Failure(ImportFailureReason.CONNECTION_FAILED)
                    } catch (_: SerializationException) {
                        ImportState.Failure(ImportFailureReason.INVALID_RESPONSE)
                    } catch (_: Exception) {
                        ImportState.Failure(ImportFailureReason.UNEXPECTED)
                    }

                // A cancelled import (re-scan or reset) must not overwrite newer
                // state: a request that throws after cancellation would otherwise
                // reach the catch arms and clobber the current state with Failure.
                if (isActive) {
                    state = result
                }
            }
    }

    fun discardCard(id: CardId) {
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
