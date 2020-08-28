package org.dockbox.darwin.core.i18n

import org.junit.jupiter.api.Test

internal class SimpleI18NServiceTest {

    private val language: Languages = Languages.EN_US
    private val defaultKey: String = "test.custom"

    @Test
    fun addTranslation() {
        // No need to mock KServer here, as long as .inject() is not called on the service
        val service = SimpleI18NService();
        val custom = SimpleI18NRegistry("Sample line")
        service.addTranslation(defaultKey, language, custom)

        val entry = service.getEntry(defaultKey, language)
        assert(entry.toString() == defaultKey)
    }
}
