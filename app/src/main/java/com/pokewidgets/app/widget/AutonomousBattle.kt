package com.pokewidgets.app.widget

import kotlin.random.Random

/**
 * Small, deterministic-by-seed battle model for the autonomous home-screen battle.
 *
 * The roster is deliberately closed: only the ten Pokémon requested for this widget
 * can be selected. A null lock means that slot is chosen randomly.
 */
object AutonomousBattle {

    data class Fighter(
        val id: Int,
        val name: String,
        val types: Set<String>,
        val maxHp: Int,
        val attack: Int,
        val defense: Int,
        val speed: Int,
        val moves: List<Move>,
    )

    data class Move(
        val name: String,
        val type: String,
        val power: Int,
        val accuracy: Int = 95,
    )

    data class Combatant(val fighter: Fighter, val hp: Int = fighter.maxHp) {
        val fainted: Boolean get() = hp <= 0
    }

    data class BattleState(
        val left: Combatant,
        val right: Combatant,
        val turn: Int = 0,
        val log: String = "Battle start!",
        val finished: Boolean = false,
        val winnerId: Int? = null,
    )

    private val roster = listOf(
        Fighter(25, "Pikachu", setOf("Electric"), 95, 80, 55, 110,
            listOf(Move("Thunderbolt", "Electric", 90), Move("Quick Attack", "Normal", 40, 100), Move("Iron Tail", "Steel", 100, 75))),
        Fighter(6, "Charizard", setOf("Fire", "Flying"), 125, 105, 85, 100,
            listOf(Move("Flamethrower", "Fire", 90), Move("Slash", "Normal", 70), Move("Air Slash", "Flying", 75, 95))),
        Fighter(34, "Nidoking", setOf("Poison", "Ground"), 135, 110, 90, 85,
            listOf(Move("Earthquake", "Ground", 100), Move("Poison Jab", "Poison", 80), Move("Megahorn", "Bug", 120, 85))),
        Fighter(18, "Pidgeot", setOf("Normal", "Flying"), 115, 85, 75, 105,
            listOf(Move("Aerial Ace", "Flying", 60, 100), Move("Wing Attack", "Flying", 60), Move("Quick Attack", "Normal", 40, 100))),
        Fighter(123, "Scyther", setOf("Bug", "Flying"), 110, 110, 80, 115,
            listOf(Move("X-Scissor", "Bug", 80), Move("Wing Attack", "Flying", 60), Move("Slash", "Normal", 70))),
        Fighter(95, "Onix", setOf("Rock", "Ground"), 105, 75, 150, 55,
            listOf(Move("Rock Slide", "Rock", 75), Move("Earthquake", "Ground", 100), Move("Iron Tail", "Steel", 100, 75))),
        Fighter(93, "Haunter", setOf("Ghost", "Poison"), 100, 105, 65, 105,
            listOf(Move("Shadow Ball", "Ghost", 80), Move("Sludge Bomb", "Poison", 90), Move("Night Shade", "Ghost", 65, 100))),
        Fighter(150, "Mewtwo", setOf("Psychic"), 145, 130, 105, 130,
            listOf(Move("Psychic", "Psychic", 90), Move("Shadow Ball", "Ghost", 80), Move("Aura Sphere", "Fighting", 80, 100))),
        Fighter(121, "Starmie", setOf("Water", "Psychic"), 110, 100, 85, 115,
            listOf(Move("Surf", "Water", 90), Move("Psychic", "Psychic", 90), Move("Ice Beam", "Ice", 90))),
        Fighter(2, "Ivysaur", setOf("Grass", "Poison"), 115, 85, 85, 75,
            listOf(Move("Razor Leaf", "Grass", 55, 95), Move("Vine Whip", "Grass", 45, 100), Move("Sludge Bomb", "Poison", 90))),
    )

    fun roster(): List<Fighter> = roster

    fun fighter(id: Int): Fighter? = roster.firstOrNull { it.id == id }

    /** Returns two distinct roster members; locked ids are honoured when valid. */
    fun choosePair(leftLock: Int? = null, rightLock: Int? = null, random: Random = Random.Default): Pair<Fighter, Fighter> {
        val left = fighter(leftLock ?: -1) ?: roster[random.nextInt(roster.size)]
        val right = fighter(rightLock ?: -1)?.takeIf { it.id != left.id }
            ?: roster.filterNot { it.id == left.id }[random.nextInt(roster.size - 1)]
        return left to right
    }

    fun start(left: Fighter, right: Fighter): BattleState =
        BattleState(Combatant(left), Combatant(right))

    /**
     * Advances one attack. The faster Pokémon acts first; thereafter turns alternate.
     * Damage is bounded and type effectiveness uses the standard single/double/resisted
     * multipliers for the types represented by this roster.
     */
    fun advance(state: BattleState, random: Random = Random.Default): BattleState {
        if (state.finished) return state
        val leftActs = when {
            state.left.fainted -> false
            state.right.fainted -> true
            state.turn == 0 -> state.left.fighter.speed >= state.right.fighter.speed
            else -> state.turn % 2 == 0
        }
        val attacker = if (leftActs) state.left else state.right
        val defender = if (leftActs) state.right else state.left
        val move = attacker.fighter.moves[random.nextInt(attacker.fighter.moves.size)]
        if (random.nextInt(100) >= move.accuracy) {
            return state.copy(turn = state.turn + 1, log = "${attacker.fighter.name} missed with ${move.name}!")
        }
        val effectiveness = effectiveness(move.type, defender.fighter.types)
        val variance = random.nextInt(85, 101) / 100.0
        val raw = (((2.0 * 50 / 5 + 2) * move.power * attacker.fighter.attack /
            defender.fighter.defense) / 50.0 + 2.0) * variance
        val stab = if (move.type in attacker.fighter.types) 1.5 else 1.0
        val damage = (raw * stab * effectiveness).toInt().coerceAtLeast(1)
        val remaining = (defender.hp - damage).coerceAtLeast(0)
        val updatedDefender = defender.copy(hp = remaining)
        val left = if (leftActs) state.left else updatedDefender
        val right = if (leftActs) updatedDefender else state.right
        val fainted = remaining == 0
        val suffix = when {
            fainted -> " ${defender.fighter.name} fainted!"
            effectiveness >= 2.0 -> " It's super effective!"
            effectiveness <= 0.5 -> " It's not very effective."
            else -> ""
        }
        return state.copy(
            left = left,
            right = right,
            turn = state.turn + 1,
            log = "${attacker.fighter.name} used ${move.name}! $damage damage.$suffix",
            finished = fainted,
            winnerId = if (fainted) attacker.fighter.id else null,
        )
    }

    private fun effectiveness(moveType: String, defenderTypes: Set<String>): Double {
        val strong = mapOf(
            "Electric" to setOf("Water", "Flying"),
            "Fire" to setOf("Grass", "Bug"),
            "Ground" to setOf("Fire", "Electric", "Poison", "Rock"),
            "Flying" to setOf("Grass", "Bug"),
            "Bug" to setOf("Grass", "Psychic"),
            "Ghost" to setOf("Psychic", "Ghost"),
            "Psychic" to setOf("Poison"),
            "Water" to setOf("Fire", "Ground", "Rock"),
            "Grass" to setOf("Water", "Ground", "Rock"),
            "Rock" to setOf("Fire", "Flying", "Bug"),
            "Ice" to setOf("Flying", "Ground"),
            "Poison" to setOf("Grass"),
            "Steel" to setOf("Rock"),
            "Fighting" to setOf("Normal", "Rock"),
        )
        val weak = mapOf(
            "Electric" to setOf("Grass"),
            "Fire" to setOf("Fire", "Water", "Rock"),
            "Ground" to setOf("Grass", "Bug"),
            "Flying" to setOf("Electric", "Rock"),
            "Bug" to setOf("Fire", "Flying"),
            "Ghost" to setOf("Dark"),
            "Psychic" to setOf("Psychic"),
            "Water" to setOf("Water", "Grass"),
            "Grass" to setOf("Fire", "Flying", "Bug", "Poison"),
            "Rock" to setOf("Ground"),
            "Ice" to setOf("Fire"),
            "Poison" to setOf("Poison", "Ground", "Rock"),
            "Steel" to setOf("Fire"),
            "Fighting" to setOf("Flying", "Psychic"),
        )
        var multiplier = 1.0
        defenderTypes.forEach { type ->
            if (type in strong[moveType].orEmpty()) multiplier *= 2.0
            if (type in weak[moveType].orEmpty()) multiplier *= 0.5
        }
        return multiplier
    }
}
