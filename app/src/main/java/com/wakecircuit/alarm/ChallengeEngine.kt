package com.wakecircuit.alarm

import kotlin.math.abs
import kotlin.math.roundToInt

data class MathChallenge(
    val question: String,
    val answer: Double,
    val hint: String
) {
    fun accepts(input: String): Boolean {
        val numeric = input.trim().replace("π", "3.1415926").toDoubleOrNull() ?: return false
        return abs(numeric - answer) <= maxOf(0.01, abs(answer) * 0.01)
    }
}

object MathChallengeFactory {
    fun random(): MathChallenge = when ((0..4).random()) {
        0 -> derivativePower()
        1 -> derivativeTrig()
        2 -> definiteIntegral()
        3 -> exponentialLimit()
        else -> partialDerivative()
    }

    private fun derivativePower(): MathChallenge {
        val n = (2..6).random()
        val coefficient = (2..7).random()
        val x = (1..3).random()
        return MathChallenge(
            "设 f(x) = ${coefficient}x^$n，求 f′($x) 的值。",
            coefficient * n * Math.pow(x.toDouble(), (n - 1).toDouble()),
            "先求导：f′(x) = ${coefficient * n}x^${n - 1}。"
        )
    }

    private fun derivativeTrig(): MathChallenge {
        val x = listOf(0.0, Math.PI / 6, Math.PI / 4, Math.PI / 3).random()
        val label = when (x) {
            0.0 -> "0"
            Math.PI / 6 -> "π/6"
            Math.PI / 4 -> "π/4"
            else -> "π/3"
        }
        return MathChallenge(
            "设 f(x) = sin x，求 f′($label) 的值（保留 2 位小数）。",
            kotlin.math.cos(x),
            "sin x 的导数是 cos x。"
        )
    }

    private fun definiteIntegral(): MathChallenge {
        val upper = (2..5).random()
        return MathChallenge(
            "计算定积分：∫₀^$upper x dx。",
            upper * upper / 2.0,
            "∫x dx = x²/2，再代入上下限。"
        )
    }

    private fun exponentialLimit(): MathChallenge {
        val a = (1..4).random()
        return MathChallenge(
            "计算极限：limₓ→0 (e^(${a}x) − 1) / x。",
            a.toDouble(),
            "可使用基本极限 limₜ→0 (eᵗ − 1)/t = 1。"
        )
    }

    private fun partialDerivative(): MathChallenge {
        val a = (2..5).random()
        val y = (1..4).random()
        return MathChallenge(
            "设 z = ${a}x²y + 3y，求 ∂z/∂x 在 x = 2，y = $y 时的值。",
            4.0 * a * y,
            "将 y 视为常数，∂z/∂x = ${2 * a}xy。"
        )
    }
}

data class EnglishChallenge(val sentence: String, val chinese: String)

object EnglishChallenges {
    private val items = listOf(
        EnglishChallenge("I am awake and ready to start my day.", "我已经清醒，准备开始新的一天。"),
        EnglishChallenge("Small steps create meaningful progress.", "微小的步伐也能创造有意义的进步。"),
        EnglishChallenge("I choose focus over another five minutes.", "我选择专注，而不是再赖床五分钟。"),
        EnglishChallenge("Today is a good day to do difficult things.", "今天适合完成那些困难的事。"),
        EnglishChallenge("I can begin before I feel completely ready.", "即使还没完全准备好，我也可以开始。")
    )

    fun random(): EnglishChallenge = items.random()
}

object PronunciationJudge {
    fun score(expected: String, actual: String): Int {
        val a = normalize(expected)
        val b = normalize(actual)
        if (a.isBlank() || b.isBlank()) return 0
        val distance = levenshtein(a, b)
        return ((1.0 - distance.toDouble() / maxOf(a.length, b.length)) * 100).coerceIn(0.0, 100.0).roundToInt()
    }

    private fun normalize(text: String): String = text.lowercase().replace(Regex("[^a-z]"), "")

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[b.length]
    }
}
