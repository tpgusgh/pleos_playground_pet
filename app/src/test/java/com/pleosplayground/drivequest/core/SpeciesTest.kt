package com.pleosplayground.drivequest.core

import com.pleosplayground.drivequest.core.Face.HAPPY
import com.pleosplayground.drivequest.core.Face.HUNGRY
import com.pleosplayground.drivequest.core.Face.SULKY
import com.pleosplayground.drivequest.core.LineKind.DRIVE_START
import com.pleosplayground.drivequest.core.LineKind.GREETING
import com.pleosplayground.drivequest.core.LineKind.NOT_UNDERSTOOD
import com.pleosplayground.drivequest.core.Species.BEAR
import com.pleosplayground.drivequest.core.Species.CAT
import com.pleosplayground.drivequest.core.Species.DOG
import com.pleosplayground.drivequest.core.Species.DUCK
import com.pleosplayground.drivequest.core.Species.FOX
import com.pleosplayground.drivequest.core.Species.RABBIT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeciesTest {
    @Test
    fun six_species_in_order_with_distinct_values() {
        val all = Species.entries
        assertEquals(listOf("rabbit", "cat", "bear", "dog", "fox", "duck"), all.map { it.id })
        assertEquals(listOf("토끼", "고양이", "곰", "강아지", "여우", "오리"), all.map { it.displayName })
        assertEquals(listOf("토끼야", "냥이야", "곰돌아", "멍이야", "여우야", "오리야"), all.map { it.callName })
        assertEquals(6, all.map { it.body }.toSet().size)
        assertEquals(6, all.map { it.ears }.toSet().size)
        assertEquals(6, all.map { it.charm }.toSet().size)
    }

    @Test
    fun rabbit_colors_are_the_current_drawing_colors() {
        assertEquals(0xFFBFEEDD, RABBIT.body)
        assertEquals(0xFF9EDCC5, RABBIT.shade)
        assertEquals(0xFFA8E3CC, RABBIT.ear)
        assertEquals(0xFFFFC9D6, RABBIT.inner)
    }

    @Test
    fun rabbit_lines_are_the_existing_lines() {
        for (f in Face.entries) {
            assertEquals(greeting(f), lineFor(RABBIT, GREETING, f))
            assertEquals(driveStartLine(f), lineFor(RABBIT, DRIVE_START, f))
            assertEquals(notUnderstoodLine(f), lineFor(RABBIT, NOT_UNDERSTOOD, f))
        }
        assertEquals("안녕! 오늘도 같이 달려요", lineFor(RABBIT, GREETING, HAPPY))
    }

    @Test
    fun species_lines_match_the_table() {
        // species -> kind -> [HAPPY, SULKY, HUNGRY]
        val table = mapOf(
            CAT to mapOf(
                GREETING to listOf("왔냥? 조금은 반갑다냥", "...흥. 오늘은 별로다냥", "배고프다냥..."),
                DRIVE_START to listOf("출발? 같이 가 줄게냥", "...출발이냥.", "배고픈데 출발이냥..."),
                NOT_UNDERSTOOD to listOf("음? 무슨 말이냥", "...모르겠다냥", "배고파서 모르겠다냥..."),
            ),
            BEAR to mapOf(
                GREETING to listOf("안녀엉~ 오늘도 든든하게 가자", "음... 오늘은 조금 처지네", "꿀 생각이 나네... 배고파"),
                DRIVE_START to listOf("천천히, 든든하게 출발~", "음... 출발하는구나", "배고프지만... 출발~"),
                NOT_UNDERSTOOD to listOf("음... 잘 모르겠네~", "음... 모르겠어", "배고파서... 생각이 안 나"),
            ),
            DOG to mapOf(
                GREETING to listOf("안녕! 안녕! 너무 좋아!", "안녕... 오늘은 기운이 없어", "배고파! 그래도 안녕!"),
                DRIVE_START to listOf("출발이다! 신난다!", "출발... 힘내 볼게!", "배고프지만 출발!"),
                NOT_UNDERSTOOD to listOf("앗! 잘 모르겠어!", "으응... 모르겠어", "배고파서 모르겠어!"),
            ),
            FOX to mapOf(
                GREETING to listOf("안녕~ 오늘은 뭐 하고 놀까?", "...오늘은 장난칠 기분 아냐", "배고파서 꾀가 안 나와"),
                DRIVE_START to listOf("출발! 재밌는 일 생기겠다", "...출발이구나", "배고픈데... 출발이야?"),
                NOT_UNDERSTOOD to listOf("흠, 수수께끼인걸?", "...잘 모르겠어", "배고파서 머리가 안 돌아"),
            ),
            DUCK to mapOf(
                GREETING to listOf("꽥! 안녕!", "...꽥. 시무룩", "꽥... 배고파"),
                DRIVE_START to listOf("꽥! 출발!", "...출발, 꽥", "배고파... 출발?"),
                NOT_UNDERSTOOD to listOf("꽥? 몰라!", "...몰라, 꽥", "배고파서 몰라..."),
            ),
        )
        val faces = listOf(HAPPY, SULKY, HUNGRY)
        var count = 0
        for ((species, kinds) in table) for ((kind, lines) in kinds) for (i in faces.indices) {
            assertEquals("$species $kind ${faces[i]}", lines[i], lineFor(species, kind, faces[i]))
            count++
        }
        assertEquals(45, count)
    }

    @Test
    fun every_line_is_short_and_distinct_per_face() {
        for (s in Species.entries) {
            for (k in LineKind.entries) {
                val lines = Face.entries.map { lineFor(s, k, it) }
                assertTrue("$s $k", lines.all { it.isNotEmpty() && it.length <= 20 })
                assertEquals("$s $k", 3, lines.toSet().size)
            }
            assertNotEquals(lineFor(s, GREETING, HAPPY), lineFor(s, DRIVE_START, HAPPY))
        }
    }

    @Test
    fun species_from_id_decodes_only_exact_ids() {
        for (s in Species.entries) assertEquals(s, speciesFromId(s.id))
        for (bad in listOf(null, "", "dragon", "CAT", " cat", "cat ")) assertNull(bad, speciesFromId(bad))
    }

    @Test
    fun description_names_face_and_species() {
        assertEquals("행복한 곰 캐릭터", petDescription(HAPPY, BEAR))
        assertEquals("시무룩한 고양이 캐릭터", petDescription(SULKY, CAT))
        assertEquals("배고픈 토끼 캐릭터", petDescription(HUNGRY, RABBIT))
    }
}
