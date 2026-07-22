package com.ralscanner.colordetector

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Finds the nearest RAL Classic color for a sampled RGB pixel.
 *
 * Matching is done in CIE Lab color space using the CIEDE2000 formula,
 * which approximates human color perception far better than a naive
 * RGB Euclidean distance.
 */
object ColorMatcher {

    /** Result of a match, exposing both the RAL entry and how close it was (lower = closer). */
    data class MatchResult(val ral: RalColor, val deltaE: Double)

    fun findNearestRal(r: Int, g: Int, b: Int): RalColor = findNearestRalWithScore(r, g, b).ral

    fun findNearestRalWithScore(r: Int, g: Int, b: Int): MatchResult {
        val targetLab = rgbToLab(r, g, b)

        var closest = RalColorDatabase.colors[0]
        var closestDistance = Double.MAX_VALUE

        for (ral in RalColorDatabase.colors) {
            val ralLab = rgbToLab(ral.r, ral.g, ral.b)
            val distance = deltaE2000(targetLab, ralLab)
            if (distance < closestDistance) {
                closestDistance = distance
                closest = ral
            }
        }
        return MatchResult(closest, closestDistance)
    }

    /** Converts sRGB (0-255 each channel) into CIE L*a*b* (D65 illuminant). */
    private fun rgbToLab(r: Int, g: Int, b: Int): DoubleArray {
        var rl = r / 255.0
        var gl = g / 255.0
        var bl = b / 255.0

        rl = if (rl > 0.04045) ((rl + 0.055) / 1.055).pow(2.4) else rl / 12.92
        gl = if (gl > 0.04045) ((gl + 0.055) / 1.055).pow(2.4) else gl / 12.92
        bl = if (bl > 0.04045) ((bl + 0.055) / 1.055).pow(2.4) else bl / 12.92

        rl *= 100.0
        gl *= 100.0
        bl *= 100.0

        val x = rl * 0.4124 + gl * 0.3576 + bl * 0.1805
        val y = rl * 0.2126 + gl * 0.7152 + bl * 0.0722
        val z = rl * 0.0193 + gl * 0.1192 + bl * 0.9505

        // D65 reference white
        val xn = 95.047
        val yn = 100.0
        val zn = 108.883

        var xr = x / xn
        var yr = y / yn
        var zr = z / zn

        xr = if (xr > 0.008856) xr.pow(1.0 / 3.0) else (7.787 * xr) + (16.0 / 116.0)
        yr = if (yr > 0.008856) yr.pow(1.0 / 3.0) else (7.787 * yr) + (16.0 / 116.0)
        zr = if (zr > 0.008856) zr.pow(1.0 / 3.0) else (7.787 * zr) + (16.0 / 116.0)

        val l = maxOf(0.0, (116.0 * yr) - 16.0)
        val a = 500.0 * (xr - yr)
        val labB = 200.0 * (yr - zr)

        return doubleArrayOf(l, a, labB)
    }

    /** Standard CIEDE2000 perceptual color-difference formula. */
    private fun deltaE2000(lab1: DoubleArray, lab2: DoubleArray): Double {
        val l1 = lab1[0]; val a1 = lab1[1]; val b1 = lab1[2]
        val l2 = lab2[0]; val a2 = lab2[1]; val b2 = lab2[2]

        val kL = 1.0
        val kC = 1.0
        val kH = 1.0

        val c1 = sqrt(a1 * a1 + b1 * b1)
        val c2 = sqrt(a2 * a2 + b2 * b2)
        val cBar = (c1 + c2) / 2.0

        val g = 0.5 * (1 - sqrt(cBar.pow(7) / (cBar.pow(7) + 25.0.pow(7))))

        val a1p = a1 * (1 + g)
        val a2p = a2 * (1 + g)

        val c1p = sqrt(a1p * a1p + b1 * b1)
        val c2p = sqrt(a2p * a2p + b2 * b2)

        var h1p = if (a1p == 0.0 && b1 == 0.0) 0.0 else atan2(b1, a1p)
        if (h1p < 0) h1p += 2 * PI
        var h2p = if (a2p == 0.0 && b2 == 0.0) 0.0 else atan2(b2, a2p)
        if (h2p < 0) h2p += 2 * PI

        val deltaLp = l2 - l1
        val deltaCp = c2p - c1p

        val deltahp: Double = if (c1p * c2p == 0.0) {
            0.0
        } else {
            var dh = h2p - h1p
            if (dh > PI) dh -= 2 * PI
            if (dh < -PI) dh += 2 * PI
            dh
        }
        val deltaHp = 2 * sqrt(c1p * c2p) * sin(deltahp / 2.0)

        val lBarp = (l1 + l2) / 2.0
        val cBarp = (c1p + c2p) / 2.0

        val hBarp: Double = if (c1p * c2p == 0.0) {
            h1p + h2p
        } else if (abs(h1p - h2p) > PI) {
            if (h1p + h2p < 2 * PI) (h1p + h2p + 2 * PI) / 2.0 else (h1p + h2p - 2 * PI) / 2.0
        } else {
            (h1p + h2p) / 2.0
        }

        val t = 1 - 0.17 * cos(hBarp - PI / 6.0) + 0.24 * cos(2 * hBarp) +
                0.32 * cos(3 * hBarp + PI / 30.0) - 0.20 * cos(4 * hBarp - 63.0 * PI / 180.0)

        val deltaTheta = (30.0 * PI / 180.0) * exp(-((hBarp * 180.0 / PI - 275.0) / 25.0).pow(2))
        val rC = 2 * sqrt(cBarp.pow(7) / (cBarp.pow(7) + 25.0.pow(7)))
        val sL = 1 + (0.015 * (lBarp - 50).pow(2)) / sqrt(20 + (lBarp - 50).pow(2))
        val sC = 1 + 0.045 * cBarp
        val sH = 1 + 0.015 * cBarp * t
        val rT = -sin(2 * deltaTheta) * rC

        val termL = deltaLp / (kL * sL)
        val termC = deltaCp / (kC * sC)
        val termH = deltaHp / (kH * sH)

        return sqrt(termL.pow(2) + termC.pow(2) + termH.pow(2) + rT * termC * termH)
    }
}
