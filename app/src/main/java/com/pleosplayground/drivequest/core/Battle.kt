package com.pleosplayground.drivequest.core

import kotlin.math.max
import kotlin.random.Random

data class Monster(val name: String, val hp: Int, val atk: Int, val def: Int, val agi: Int)
data class BattleResult(val won: Boolean, val rounds: Int, val log: List<String>)

private const val MAX_ROUNDS = 50

fun weeklyBoss(weekIndex: Int) = Monster(
    "주간 보스", hp = 200 + 50 * weekIndex, atk = 12 + 2 * weekIndex, def = 6 + weekIndex, agi = 6,
)

fun bossChallenger(hero: Hero, weekXp: Int): Hero = hero.copy(atk = hero.atk + weekXp / 50)

fun fight(hero: Hero, monster: Monster, seed: Long): BattleResult {
    if (hero.hp <= 0) return BattleResult(false, 0, listOf("영웅이 쓰러져 있어요"))
    val rnd = Random(seed)
    val log = mutableListOf<String>()
    var heroHp = hero.hp
    var monsterHp = monster.hp
    val heroFirst = hero.agi >= monster.agi
    fun dmg(atk: Int, def: Int) = max(1, atk - def) + rnd.nextInt(0, 2)
    fun heroHits() {
        val d = dmg(hero.atk, monster.def)
        monsterHp -= d
        log += "영웅이 ${monster.name}에게 $d 피해"
    }
    fun monsterHits() {
        val d = dmg(monster.atk, hero.def)
        heroHp -= d
        log += "${monster.name}이(가) 영웅에게 $d 피해"
    }

    var rounds = 0
    while (rounds < MAX_ROUNDS && heroHp > 0 && monsterHp > 0) {
        rounds++
        if (heroFirst) {
            heroHits()
            if (monsterHp > 0) monsterHits()
        } else {
            monsterHits()
            if (heroHp > 0) heroHits()
        }
    }
    val won = monsterHp <= 0 && heroHp > 0
    log += if (won) "승리!" else "패배..."
    return BattleResult(won, rounds, log)
}
