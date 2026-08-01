package com.manocormen.flashcardimporter

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ImportCardsTest {
    @Test
    fun `fetch cards`() =
        runBlocking {
            MockWebServer().use { server ->
                server.start()
                server.enqueue(
                    MockResponse(
                        body = """{"data":[{"cards":[{"front":"front","back":"back"}]}]}""",
                    ),
                )

                assertEquals(
                    listOf(BasicCard(front = "front", back = "back")),
                    fetchCards(server.url("/gradio_api/api/cards").toString()),
                )
                val request = server.takeRequest()
                assertEquals("POST", request.method)
                assertEquals("""{"data":[]}""", request.body?.utf8())
            }
        }

    @Test
    fun `reject empty card response`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse(body = """{"data":[]}"""))

            assertThrows(IOException::class.java) {
                runBlocking {
                    fetchCards(server.url("/gradio_api/api/cards").toString())
                }
            }
        }
    }

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
