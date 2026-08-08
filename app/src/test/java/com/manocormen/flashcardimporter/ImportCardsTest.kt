package com.manocormen.flashcardimporter

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

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
                val endpoint =
                    requireNotNull(
                        CardsEndpoint.validateOrNull(
                            server.url("/gradio_api/api/cards").toString(),
                        ),
                    )

                assertEquals(
                    listOf(BasicCard(front = "front", back = "back")),
                    fetchCards(endpoint),
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
            val endpoint =
                requireNotNull(
                    CardsEndpoint.validateOrNull(
                        server.url("/gradio_api/api/cards").toString(),
                    ),
                )

            assertThrows(SerializationException::class.java) {
                runBlocking {
                    fetchCards(endpoint)
                }
            }
        }
    }

    @Test
    fun `accept valid cards endpoint`() {
        val urls =
            listOf(
                "http://192.168.0.1:7860/gradio_api/api/cards",
                "http://0.0.0.0:1/gradio_api/api/cards",
            )

        for (url in urls) {
            assertNotNull(
                "Expected valid URL: $url",
                CardsEndpoint.validateOrNull(url),
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
                "http://0.0.0.0:0/gradio_api/api/cards",
                "http://192.168.0.1:7860/INVALID-ENDPOINT",
            )

        for (url in urls) {
            assertNull(
                "Expected invalid URL: $url",
                CardsEndpoint.validateOrNull(url),
            )
        }
    }
}
