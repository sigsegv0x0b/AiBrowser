package com.aibrowser.agent

import android.webkit.WebView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object HumanEmulation {

    private val QWERTY = mapOf(
        '`' to Pair(0, 0), '1' to Pair(0, 1), '2' to Pair(0, 2), '3' to Pair(0, 3), '4' to Pair(0, 4), '5' to Pair(0, 5),
        '6' to Pair(0, 6), '7' to Pair(0, 7), '8' to Pair(0, 8), '9' to Pair(0, 9), '0' to Pair(0, 10), '-' to Pair(0, 11), '=' to Pair(0, 12),
        'q' to Pair(1, 1), 'w' to Pair(1, 2), 'e' to Pair(1, 3), 'r' to Pair(1, 4), 't' to Pair(1, 5), 'y' to Pair(1, 6),
        'u' to Pair(1, 7), 'i' to Pair(1, 8), 'o' to Pair(1, 9), 'p' to Pair(1, 10), '[' to Pair(1, 11), ']' to Pair(1, 12), '\\' to Pair(1, 13),
        'a' to Pair(2, 1), 's' to Pair(2, 2), 'd' to Pair(2, 3), 'f' to Pair(2, 4), 'g' to Pair(2, 5), 'h' to Pair(2, 6),
        'j' to Pair(2, 7), 'k' to Pair(2, 8), 'l' to Pair(2, 9), ';' to Pair(2, 10), "'" to Pair(2, 11),
        'z' to Pair(3, 1), 'x' to Pair(3, 2), 'c' to Pair(3, 3), 'v' to Pair(3, 4), 'b' to Pair(3, 5), 'n' to Pair(3, 6),
        'm' to Pair(3, 7), ',' to Pair(3, 8), '.' to Pair(3, 9), '/' to Pair(3, 10),
        ' ' to Pair(4, 6)
    )

    private val QWERTY_NEIGHBORS = mapOf(
        'q' to listOf('w', 'a'),
        'w' to listOf('q', 'e', 's', 'a'),
        'e' to listOf('w', 'r', 'd', 's'),
        'r' to listOf('e', 't', 'f', 'd'),
        't' to listOf('r', 'y', 'g', 'f'),
        'y' to listOf('t', 'u', 'h', 'g'),
        'u' to listOf('y', 'i', 'j', 'h'),
        'i' to listOf('u', 'o', 'k', 'j'),
        'o' to listOf('i', 'p', 'l', 'k'),
        'p' to listOf('o', '[', 'l'),
        'a' to listOf('q', 's', 'z'),
        's' to listOf('a', 'w', 'd', 'x', 'z'),
        'd' to listOf('s', 'e', 'f', 'c', 'x'),
        'f' to listOf('d', 'r', 'g', 'v', 'c'),
        'g' to listOf('f', 't', 'h', 'b', 'v'),
        'h' to listOf('g', 'y', 'j', 'n', 'b'),
        'j' to listOf('h', 'u', 'k', 'm', 'n'),
        'k' to listOf('j', 'i', 'l', 'm'),
        'l' to listOf('k', 'o', 'p'),
        'z' to listOf('a', 'x'),
        'x' to listOf('z', 's', 'c'),
        'c' to listOf('x', 'd', 'v'),
        'v' to listOf('c', 'f', 'b'),
        'b' to listOf('v', 'g', 'n'),
        'n' to listOf('b', 'h', 'm'),
        'm' to listOf('n', 'j', 'k')
    )

    private val cursorPositions = mutableMapOf<Int, Pair<Double, Double>>()

    data class PathPoint(val x: Double, val y: Double, val delayMs: Long)

    fun getCursorPosition(webView: WebView): Pair<Double, Double>? = cursorPositions[webView.hashCode()]

    fun setCursorPosition(webView: WebView, x: Double, y: Double) {
        cursorPositions[webView.hashCode()] = x to y
    }

    fun clearCursorPosition(webView: WebView) {
        cursorPositions.remove(webView.hashCode())
    }

    fun qwertyDistance(char1: Char, char2: Char): Double {
        val k1 = QWERTY[char1.lowercaseChar()]
        val k2 = QWERTY[char2.lowercaseChar()]
        if (k1 == null || k2 == null) return 5.0
        return sqrt((k1.first - k2.first).toDouble().pow(2.0) + (k1.second - k2.second).toDouble().pow(2.0))
    }

    private fun delayForDistance(dist: Double): Long {
        return when {
            dist <= 0.5 -> 15L
            dist <= 1.5 -> 30L
            dist <= 2.5 -> 55L
            dist <= 3.5 -> 80L
            dist <= 5.0 -> 105L
            else -> 130L
        }
    }

    fun gaussianRandom(mean: Double, stddev: Double): Double {
        var u = 0.0
        var v = 0.0
        while (u == 0.0) u = Math.random()
        while (v == 0.0) v = Math.random()
        val z = sqrt(-2.0 * kotlin.math.ln(u)) * cos(2.0 * Math.PI * v)
        return mean + z * stddev
    }

    fun clamp(value: Double, min: Double, max: Double): Double = max(min, min(max, value))

    fun computeHumanDelays(text: String, wpm: Int = 60): List<Long> {
        val meanDelay = 12000.0 / wpm
        val stddev = meanDelay * 0.3
        val delays = mutableListOf<Long>()
        var wordStart = 0

        for (i in text.indices) {
            val char = text[i]
            var delay = gaussianRandom(meanDelay, stddev)

            if (i > 0) {
                val dist = qwertyDistance(text[i - 1], char)
                delay += delayForDistance(dist)
            } else {
                delay += 200.0
            }

            when (char) {
                ' ' -> delay += gaussianRandom(120.0, 30.0)
                ',', ';', ':' -> delay += gaussianRandom(150.0, 40.0)
                '.', '!', '?' -> delay += gaussianRandom(400.0, 100.0)
                '\n' -> delay += gaussianRandom(600.0, 150.0)
            }

            if (i > 0 && text[i - 1] == ' ') {
                wordStart = i
            } else if (i - wordStart in 1..3) {
                delay *= 0.85
            }

            if (i > 80 && Math.random() < 0.015) {
                delay += gaussianRandom(700.0, 200.0)
            }

            if (i > 200) {
                val fatigue = (i - 200) / 800.0
                delay *= 1 + fatigue * 0.4
            }

            delays.add(clamp(delay, 30.0, 1200.0).toLong())
        }

        return delays
    }

    private fun cubicBezier(t: Double, p0: Double, p1: Double, p2: Double, p3: Double): Double {
        val u = 1 - t
        return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3
    }

    fun generateBezierPath(startX: Double, startY: Double, endX: Double, endY: Double): List<PathPoint> {
        val dist = sqrt((endX - startX).pow(2.0) + (endY - startY).pow(2.0))
        val numPoints = clamp((dist / 3.0).toInt().toDouble(), 25.0, 80.0).toInt()

        val controlOffset = dist * 0.15 + 10.0
        val cp1x = startX + (endX - startX) * 0.25 + gaussianRandom(0.0, controlOffset * 0.5)
        val cp1y = startY + (endY - startY) * 0.25 + gaussianRandom(0.0, controlOffset * 0.5)
        val cp2x = startX + (endX - startX) * 0.75 + gaussianRandom(0.0, controlOffset * 0.5)
        val cp2y = startY + (endY - startY) * 0.75 + gaussianRandom(0.0, controlOffset * 0.5)

        val points = mutableListOf<PathPoint>()

        for (i in 0..numPoints) {
            val t = i / numPoints.toDouble()
            val easedT = if (t < 0.5) 2 * t * t else 1 - (-2 * t + 2).pow(2.0) / 2.0

            val x = cubicBezier(easedT, startX, cp1x, cp2x, endX)
            val y = cubicBezier(easedT, startY, cp1y, cp2y, endY)

            val jitter = min(1.5, 0.5 + dist * 0.002)
            val jx = gaussianRandom(0.0, jitter)
            val jy = gaussianRandom(0.0, jitter)

            val progress = i / numPoints.toDouble()
            val baseDelay = 5.0 + sin(progress * Math.PI) * 10.0

            points.add(PathPoint(x + jx, y + jy, clamp(baseDelay, 3.0, 18.0).toLong()))
        }

        if (dist > 50 && Math.random() < 0.12) {
            val overshootPx = clamp(dist * 0.03, 3.0, 15.0)
            val angle = kotlin.math.atan2(endY - startY, endX - startX)
            val oosX = endX + cos(angle) * overshootPx + gaussianRandom(0.0, 3.0)
            val oosY = endY + sin(angle) * overshootPx + gaussianRandom(0.0, 3.0)
            val overshootPoints = generateBezierPath(endX, endY, oosX, oosY) + generateBezierPath(oosX, oosY, endX, endY)
            points.addAll(overshootPoints)
        }

        return points
    }

    fun clickDwellMs(): Long = clamp(gaussianRandom(130.0, 40.0), 60.0, 280.0).toLong()
    fun clickHoldMs(): Long = clamp(gaussianRandom(50.0, 20.0), 20.0, 120.0).toLong()
    fun doubleClickBetweenMs(): Long = clamp(gaussianRandom(60.0, 20.0), 30.0, 120.0).toLong()
}
