package it.emperor.animatedcheckbox.extension

import android.graphics.Color

fun Float.toRange(oldMin: Float, oldMax: Float, newMin: Float, newMax: Float): Float =
        (((this - oldMin) * (newMax - newMin)) / (oldMax - oldMin)) + newMin

fun Float.clamp(min: Float, max: Float): Float {
    if (this < min) return min
    if (this > max) return max
    return this
}

fun FloatArray.animateColor(colorFrom: FloatArray, colorTo: FloatArray, animatedFraction: Float): Int {
    this[0] = colorFrom[0] + (colorTo[0] - colorFrom[0]) * animatedFraction;
    this[1] = colorFrom[1] + (colorTo[1] - colorFrom[1]) * animatedFraction;
    this[2] = colorFrom[2] + (colorTo[2] - colorFrom[2]) * animatedFraction;
    return Color.HSVToColor(this)
}