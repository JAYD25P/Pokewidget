package com.pokewidgets.app.widget

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutonomousBattleTest {

    @Test
    fun rosterContainsOnlyTheTenConfirmedPokemon() {
        assertEquals(
            listOf("Pikachu", "Charizard", "Nidoking", "Pidgeot", "Scyther",
                "Onix", "Haunter", "Mewtwo", "Starmie", "Ivysaur"),
            AutonomousBattle.roster().map { it.name },
        )
    }

    @Test
    fun lockedSlotsAreHonouredAndOpponentsAreDistinct() {
        val pair = AutonomousBattle.choosePair(25, 6, Random(7))
        assertEquals(25, pair.first.id)
        assertEquals(6, pair.second.id)

        val oneLocked = AutonomousBattle.choosePair(150, null, Random(12))
        assertEquals(150, oneLocked.first.id)
        assertNotEquals(oneLocked.first.id, oneLocked.second.id)
    }

    @Test
    fun battleContinuesUntilOneCombatantFaints() {
        val left = AutonomousBattle.fighter(25)!!
        val right = AutonomousBattle.fighter(6)!!
        var state = AutonomousBattle.start(left, right)
        val random = Random(2026)
        repeat(500) {
            if (!state.finished) state = AutonomousBattle.advance(state, random)
        }
        assertTrue(state.finished)
        assertNotNull(state.winnerId)
        assertTrue(state.left.hp >= 0)
        assertTrue(state.right.hp >= 0)
        assertTrue((state.left.fainted xor state.right.fainted))
    }

    @Test
    fun finishedBattleDoesNotAdvanceAgain() {
        val left = AutonomousBattle.fighter(150)!!
        val right = AutonomousBattle.fighter(95)!!
        var state = AutonomousBattle.start(left, right)
        val random = Random(3)
        repeat(500) {
            if (!state.finished) state = AutonomousBattle.advance(state, random)
        }
        val finished = state
        assertEquals(finished, AutonomousBattle.advance(finished, Random(4)))
    }
}
