package com.manocormen.flashcardimporter

import java.net.URI
import java.net.URISyntaxException

private const val CARDS_ENDPOINT = "/gradio_api/api/cards"

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
