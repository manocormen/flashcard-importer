package com.manocormen.flashcardimporter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

private const val CARDS_ENDPOINT = "/gradio_api/api/cards"
private val client = OkHttpClient()
private val json = Json { ignoreUnknownKeys = true }
private val requestBody = """{"data":[]}""".toRequestBody("application/json".toMediaType())

@Serializable
data class BasicCard(
    val front: String,
    val back: String,
)

@Serializable
private data class GeneratedCards(
    val cards: List<BasicCard>,
)

@Serializable
private data class GradioResponse(
    val data: List<GeneratedCards?>,
)

fun isValidCardsEndpoint(value: String): Boolean {
    val endpoint =
        try {
            URI(value)
        } catch (_: URISyntaxException) {
            return false
        }

    return endpoint.scheme == "http" &&
        endpoint.host != null &&
        endpoint.port != -1 &&
        endpoint.path == CARDS_ENDPOINT
}

suspend fun fetchCards(endpoint: String): List<BasicCard> =
    withContext(Dispatchers.IO) {
        val request =
            Request
                .Builder()
                .url(endpoint)
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
            ?: throw IOException("No flashcards found")
    }
