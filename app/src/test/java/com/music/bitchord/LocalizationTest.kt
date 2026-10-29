package com.music.bitchord

import com.music.bitchord.ui.components.SUPPORTED_LANGUAGES
import com.music.bitchord.ui.components.languageDisplayNameRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationTest {

    @Test
    fun supportedLanguages_areEnglishAndIndonesian() {
        assertEquals(listOf("en", "id"), SUPPORTED_LANGUAGES.map { it.tag })
        assertNotNull(SUPPORTED_LANGUAGES.firstOrNull { it.tag == "id" })
        assertEquals(R.string.indonesian, languageDisplayNameRes("id"))
    }

    @Test
    fun localesConfig_matchesSupportedLanguages() {
        val projectRoot = File(".").canonicalFile
        val resDir = if (File(projectRoot, "app/src/main/res").exists()) {
            File(projectRoot, "app/src/main/res")
        } else {
            File(projectRoot, "src/main/res")
        }
        val localesConfigFile = File(resDir, "xml/locales_config.xml")
        assertTrue("locales_config.xml must exist", localesConfigFile.exists())

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(localesConfigFile)
        val localeNodes = doc.getElementsByTagName("locale")

        val configuredLocales = mutableListOf<String>()
        for (i in 0 until localeNodes.length) {
            val node = localeNodes.item(i)
            val name = node.attributes.getNamedItem("android:name")?.nodeValue
            if (name != null) configuredLocales.add(name)
        }

        for (lang in SUPPORTED_LANGUAGES) {
            assertTrue(
                "locales_config.xml must include '${lang.tag}'",
                configuredLocales.contains(lang.tag),
            )
        }
    }
}
