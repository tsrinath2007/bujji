package com.pupil.app.core

import com.pupil.app.core.creature.CreatureDialogue
import org.junit.Assert.assertEquals
import org.junit.Test

class CreatureDialogueTest {

    @Test
    fun testLevelForXp() {
        assertEquals(1, CreatureDialogue.getLevelForXp(0))
        assertEquals(1, CreatureDialogue.getLevelForXp(24))
        assertEquals(2, CreatureDialogue.getLevelForXp(25))
        assertEquals(2, CreatureDialogue.getLevelForXp(59))
        assertEquals(3, CreatureDialogue.getLevelForXp(60))
        assertEquals(5, CreatureDialogue.getLevelForXp(180))
        assertEquals(8, CreatureDialogue.getLevelForXp(500))
        assertEquals(8, CreatureDialogue.getLevelForXp(1000))
    }

    @Test
    fun testFormAndStageForLevel() {
        assertEquals(CreatureDialogue.FORM_1_NAME, CreatureDialogue.getFormForLevel(1))
        assertEquals(1, CreatureDialogue.getStageForLevel(1))

        assertEquals(CreatureDialogue.FORM_1_NAME, CreatureDialogue.getFormForLevel(2))
        assertEquals(1, CreatureDialogue.getStageForLevel(2))

        assertEquals(CreatureDialogue.FORM_2_NAME, CreatureDialogue.getFormForLevel(3))
        assertEquals(2, CreatureDialogue.getStageForLevel(3))

        assertEquals(CreatureDialogue.FORM_3_NAME, CreatureDialogue.getFormForLevel(5))
        assertEquals(3, CreatureDialogue.getStageForLevel(5))
    }

    @Test
    fun testBaseAndNextLevelXp() {
        assertEquals(0, CreatureDialogue.getCurrentLevelBaseXp(1))
        assertEquals(25, CreatureDialogue.getCurrentLevelBaseXp(2))
        assertEquals(60, CreatureDialogue.getCurrentLevelBaseXp(3))

        assertEquals(25, CreatureDialogue.getNextLevelXp(1))
        assertEquals(60, CreatureDialogue.getNextLevelXp(2))
    }
}
