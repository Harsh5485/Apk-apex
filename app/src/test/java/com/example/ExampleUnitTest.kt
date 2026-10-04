package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testTitleCleaningAndYear() {
    val clean = com.example.data.MediaCatalogMetadata.cleanTitle("Baaghi.4.2025.270p.AMZN.WEB-DL.Hindi.AAC.H.264-APeX.mkv")
    val year = com.example.data.MediaCatalogMetadata.extractYear("Baaghi 4 (2025)")
    assertEquals("2025", year)
    assertTrue(clean.isNotBlank())
  }
}
