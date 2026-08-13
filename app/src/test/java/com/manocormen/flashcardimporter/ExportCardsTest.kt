package com.manocormen.flashcardimporter

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportCardsTest {
    @Test
    fun `convert Markdown to HTML`() {
        val markdown =
            """
            * hello
            * **there**
            * `2 < 3`
            """.trimIndent()

        assertEquals(
            """
            <ul>
             <li>hello</li>
             <li><strong>there</strong></li>
             <li><code>2 &lt; 3</code></li>
            </ul>
            """.trimIndent(),
            markdownToHtml(markdown),
        )
    }

    @Test
    fun `convert Markdown paragraphs to HTML`() {
        val markdown =
            """
            First paragraph.

            Second paragraph.
            """.trimIndent()

        assertEquals(
            """
            <p>First paragraph.</p>
            <p>Second paragraph.</p>
            """.trimIndent(),
            markdownToHtml(markdown),
        )
    }

    @Test
    fun `sanitize raw HTML`() {
        val markdown = "<script>alert('Hello, there!')</script>"

        assertEquals("", markdownToHtml(markdown))
    }
}
