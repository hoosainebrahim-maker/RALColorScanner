package com.ralscanner.colordetector

/**
 * Represents a single RAL Classic color.
 *
 * @param code the RAL Classic code, e.g. "RAL 5015"
 * @param name the standard English color name, e.g. "Sky blue"
 * @param hex the color's hex value, e.g. "#007CAF"
 */
data class RalColor(val code: String, val name: String, val hex: String) {
    val r: Int = Integer.parseInt(hex.substring(1, 3), 16)
    val g: Int = Integer.parseInt(hex.substring(3, 5), 16)
    val b: Int = Integer.parseInt(hex.substring(5, 7), 16)
}
