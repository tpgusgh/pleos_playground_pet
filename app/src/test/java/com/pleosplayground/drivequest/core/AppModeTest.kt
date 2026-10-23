package com.pleosplayground.drivequest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppModeTest {
    @Test
    fun mode_ids_are_fixed() {
        assertEquals("dev", AppMode.DEV.id)
        assertEquals("connected", AppMode.CONNECTED.id)
        assertEquals(2, AppMode.entries.size)
    }

    @Test
    fun mode_from_id_decodes_only_exact_ids() {
        assertEquals(AppMode.DEV, modeFromId("dev"))
        assertEquals(AppMode.CONNECTED, modeFromId("connected"))
        for (bad in listOf(null, "", "DEV", " dev", "dev ", "Connected", "garage")) assertNull(bad, modeFromId(bad))
    }

    @Test
    fun pet_key_per_mode() {
        assertEquals("pet_dev", petKeyFor(AppMode.DEV))
        assertEquals("pet", petKeyFor(AppMode.CONNECTED))
        for (m in AppMode.entries) {
            assertNotEquals("species", petKeyFor(m))
            assertNotEquals("mode", petKeyFor(m))
        }
    }

    @Test
    fun dev_tools_only_in_dev_mode() {
        assertTrue(showDevTools(AppMode.DEV))
        assertFalse(showDevTools(AppMode.CONNECTED))
    }
}
