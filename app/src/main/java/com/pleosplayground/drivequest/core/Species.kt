package com.pleosplayground.drivequest.core

enum class EarStyle { BUNNY, CAT, BEAR, DOG, FOX, DUCK }
enum class Charm { HEART, STAR, HONEY, BONE, FLAME, DROP }
enum class LineKind { GREETING, DRIVE_START, NOT_UNDERSTOOD }

enum class Species(
    val id: String,
    val displayName: String,
    val callName: String,
    val body: Long, val shade: Long, val ear: Long, val inner: Long, // ARGB
    val ears: EarStyle,
    val charm: Charm,
    val tagline: String,
    val personality: String,
) {
    RABBIT(
        "rabbit", "토끼", "토끼야",
        0xFFBFEEDD, 0xFF9EDCC5, 0xFFA8E3CC, 0xFFFFC9D6,
        EarStyle.BUNNY, Charm.HEART,
        "다정하고 조금 수줍어요", "다정하고 조금 수줍음. 부드러운 존댓말",
    ),
    CAT(
        "cat", "고양이", "냥이야",
        0xFFFFD6E0, 0xFFF5B7C9, 0xFFFFC7D5, 0xFFFFF6E2,
        EarStyle.CAT, Charm.STAR,
        "도도한데 은근 애교쟁이", "도도한데 은근 애교. 가끔 \"냥\"",
    ),
    BEAR(
        "bear", "곰", "곰돌아",
        0xFFE9CDA8, 0xFFD6B089, 0xFFDDBB93, 0xFFFFF6E2,
        EarStyle.BEAR, Charm.HONEY,
        "느긋하고 든든해요", "느긋하고 든든. 천천히 말하고 먹는 얘기를 좋아함",
    ),
    DOG(
        "dog", "강아지", "멍이야",
        0xFFC9E4F8, 0xFFA7CEEE, 0xFFA9CBEA, 0xFFFFF6E2,
        EarStyle.DOG, Charm.BONE,
        "활발하고 늘 신나요", "활발하고 신남. 느낌표가 많음",
    ),
    FOX(
        "fox", "여우", "여우야",
        0xFFFFD2A8, 0xFFF6B37F, 0xFFFFC28E, 0xFF8C5E4A,
        EarStyle.FOX, Charm.FLAME,
        "영리하고 장난꾸러기", "영리하고 장난기 많음",
    ),
    DUCK(
        "duck", "오리", "오리야",
        0xFFFFF0A6, 0xFFF2D676, 0xFFF9E08A, 0xFFFFB46B,
        EarStyle.DUCK, Charm.DROP,
        "엉뚱하고 순해요", "엉뚱하고 순함. 짧게 말함",
    ),
}

/** Exact match only; null, blank, unknown, or differently cased/spaced values give null ("not chosen"). */
fun speciesFromId(id: String?): Species? = Species.entries.firstOrNull { it.id == id }

// Each row: HAPPY, SULKY, HUNGRY (Face declaration order).
private val SPECIES_LINES: Map<Species, Map<LineKind, List<String>>> = mapOf(
    Species.CAT to mapOf(
        LineKind.GREETING to listOf("왔냥? 조금은 반갑다냥", "...흥. 오늘은 별로다냥", "배고프다냥..."),
        LineKind.DRIVE_START to listOf("출발? 같이 가 줄게냥", "...출발이냥.", "배고픈데 출발이냥..."),
        LineKind.NOT_UNDERSTOOD to listOf("음? 무슨 말이냥", "...모르겠다냥", "배고파서 모르겠다냥..."),
    ),
    Species.BEAR to mapOf(
        LineKind.GREETING to listOf("안녀엉~ 오늘도 든든하게 가자", "음... 오늘은 조금 처지네", "꿀 생각이 나네... 배고파"),
        LineKind.DRIVE_START to listOf("천천히, 든든하게 출발~", "음... 출발하는구나", "배고프지만... 출발~"),
        LineKind.NOT_UNDERSTOOD to listOf("음... 잘 모르겠네~", "음... 모르겠어", "배고파서... 생각이 안 나"),
    ),
    Species.DOG to mapOf(
        LineKind.GREETING to listOf("안녕! 안녕! 너무 좋아!", "안녕... 오늘은 기운이 없어", "배고파! 그래도 안녕!"),
        LineKind.DRIVE_START to listOf("출발이다! 신난다!", "출발... 힘내 볼게!", "배고프지만 출발!"),
        LineKind.NOT_UNDERSTOOD to listOf("앗! 잘 모르겠어!", "으응... 모르겠어", "배고파서 모르겠어!"),
    ),
    Species.FOX to mapOf(
        LineKind.GREETING to listOf("안녕~ 오늘은 뭐 하고 놀까?", "...오늘은 장난칠 기분 아냐", "배고파서 꾀가 안 나와"),
        LineKind.DRIVE_START to listOf("출발! 재밌는 일 생기겠다", "...출발이구나", "배고픈데... 출발이야?"),
        LineKind.NOT_UNDERSTOOD to listOf("흠, 수수께끼인걸?", "...잘 모르겠어", "배고파서 머리가 안 돌아"),
    ),
    Species.DUCK to mapOf(
        LineKind.GREETING to listOf("꽥! 안녕!", "...꽥. 시무룩", "꽥... 배고파"),
        LineKind.DRIVE_START to listOf("꽥! 출발!", "...출발, 꽥", "배고파... 출발?"),
        LineKind.NOT_UNDERSTOOD to listOf("꽥? 몰라!", "...몰라, 꽥", "배고파서 몰라..."),
    ),
)

fun lineFor(species: Species, kind: LineKind, face: Face): String {
    if (species == Species.RABBIT) {
        return when (kind) {
            LineKind.GREETING -> greeting(face)
            LineKind.DRIVE_START -> driveStartLine(face)
            LineKind.NOT_UNDERSTOOD -> notUnderstoodLine(face)
        }
    }
    return SPECIES_LINES.getValue(species).getValue(kind)[face.ordinal]
}

fun petDescription(face: Face, species: Species): String {
    val mood = when (face) {
        Face.HAPPY -> "행복한"
        Face.SULKY -> "시무룩한"
        Face.HUNGRY -> "배고픈"
    }
    return "$mood ${species.displayName} 캐릭터"
}
