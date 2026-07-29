package com.manocormen.flashcardimporter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportCardsTest {
    @Test
    fun `accept valid cards endpoint`() {
        val urls =
            listOf(
                "http://192.168.0.1:7860/gradio_api/api/cards",
                "http://0.0.0.0:0/gradio_api/api/cards",
            )

        for (url in urls) {
            assertTrue(
                "Expected valid URL: $url",
                isValidCardsEndpoint(url),
            )
        }
    }

    @Test
    fun `reject invalid cards endpoint`() {
        val urls =
            listOf(
                "INVALID-URI",
                "INVALID-SCHEME://192.168.0.1:7860/gradio_api/api/cards",
                "http://:7860/gradio_api/api/cards",
                "http://192.168.0.1:INVALID-PORT/gradio_api/api/cards",
                "http://192.168.0.1:7860/INVALID-ENDPOINT",
            )

        for (url in urls) {
            assertFalse(
                "Expected invalid URL: $url",
                isValidCardsEndpoint(url),
            )
        }
    }
}
