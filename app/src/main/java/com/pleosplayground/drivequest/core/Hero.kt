package com.pleosplayground.drivequest.core

data class Hero(
    val level: Int = 1, val xp: Int = 0,
    val hp: Int = 100, val atk: Int = 10, val def: Int = 5, val agi: Int = 5,
)

private const val XP_PER_LEVEL = 100

fun Hero.gain(score: TripScore): Hero {
    var h = copy(
        xp = xp + score.xp,
        def = def + score.smoothBrakes / 10,
        agi = agi + score.bestCombo / 3,
    )
    while (h.xp >= XP_PER_LEVEL * h.level) {
        h = h.copy(
            xp = h.xp - XP_PER_LEVEL * h.level, level = h.level + 1,
            hp = h.hp + 10, atk = h.atk + 2, def = h.def + 1, agi = h.agi + 1,
        )
    }
    return h
}

fun Hero.encode(): String = "$level,$xp,$hp,$atk,$def,$agi"

fun decodeHero(raw: String?): Hero {
    val v = raw?.split(',')?.map { it.toIntOrNull() }
    if (v == null || v.size != 6 || v.any { it == null || it < 0 } || v[0] == 0 || v[2] == 0) return Hero()
    return Hero(v[0]!!, v[1]!!, v[2]!!, v[3]!!, v[4]!!, v[5]!!)
}
