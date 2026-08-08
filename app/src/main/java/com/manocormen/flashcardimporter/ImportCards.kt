package com.manocormen.flashcardimporter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

private const val CARDS_ENDPOINT = "/gradio_api/api/cards"

@Language("json")
private const val REQUEST_BODY_JSON = """{"data":[]}"""

private val client = OkHttpClient()
private val json = Json { ignoreUnknownKeys = true }
private val requestBody = REQUEST_BODY_JSON.toRequestBody("application/json".toMediaType())

class CardsEndpoint private constructor(
    val url: String,
) {
    companion object {
        fun validateOrNull(value: String): CardsEndpoint? {
            val uri =
                try {
                    URI(value)
                } catch (_: URISyntaxException) {
                    return null
                }

            // TODO: Use https once the generator supports it
            if (
                uri.scheme != "http" ||
                uri.host == null ||
                uri.port <= 0 ||
                uri.path != CARDS_ENDPOINT
            ) {
                return null
            }

            return CardsEndpoint(value)
        }
    }
}

@Serializable
data class BasicCard(
    @SerialName("front")
    val front: String,
    @SerialName("back")
    val back: String,
)

@Serializable
private data class GeneratedCards(
    @SerialName("cards")
    val cards: List<BasicCard>,
)

@Serializable
private data class GradioResponse(
    @SerialName("data")
    val data: List<GeneratedCards?>,
)

/**
 * @throws IOException if we're unable to fetch cards.
 * @throws SerializationException if the fetched card data is malformed or missing.
 */
suspend fun fetchCards(endpoint: CardsEndpoint): List<BasicCard> =
    withContext(Dispatchers.IO) {
        val request =
            Request
                .Builder()
                .url(endpoint.url)
                .post(requestBody)
                .build()
        val body =
            client
                .newCall(request)
                .execute()
                .body
                .string()

        json
            .decodeFromString<GradioResponse>(body)
            .data
            .firstOrNull()
            ?.cards
            ?: throw SerializationException("No cards found")
    }
