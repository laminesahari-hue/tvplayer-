package com.example.tvplayer.core.design

import android.graphics.Color

/** Jeu de couleurs d'un theme (sombre ou clair). */
data class Palette(
    val bg: Int,
    val surface: Int,
    val surfaceFocus: Int,
    val surfaceActive: Int,
    val text: Int,
    val textSub: Int
)

/** Design system : couleurs, espacements, rayons et echelle typographique. */
object Design {
    val dark = Palette(
        bg = Color.parseColor("#0E1117"),
        surface = Color.parseColor("#181C25"),
        surfaceFocus = Color.parseColor("#2B3342"),
        surfaceActive = Color.parseColor("#1D2B47"),
        text = Color.parseColor("#E8EBF0"),
        textSub = Color.parseColor("#98A0AE")
    )

    val light = Palette(
        bg = Color.parseColor("#F6F7F9"),
        surface = Color.parseColor("#FFFFFF"),
        surfaceFocus = Color.parseColor("#E4E8F0"),
        surfaceActive = Color.parseColor("#DCE8FF"),
        text = Color.parseColor("#11151C"),
        textSub = Color.parseColor("#5B6472")
    )

    /** mode : 0 = automatique, 1 = sombre, 2 = clair. */
    fun palette(mode: Int, systemNight: Boolean): Palette = when (mode) {
        1 -> dark
        2 -> light
        else -> if (systemNight) dark else light
    }

    /** Grille d'espacement (dp). */
    object Space {
        const val XS = 4
        const val S = 8
        const val M = 12
        const val L = 16
        const val XL = 24
        const val XXL = 32
    }

    /** Rayons d'angle (dp). */
    object Radius {
        const val CHIP = 8
        const val CARD = 12
        const val PILL = 20
    }

    /** Echelle typographique (sp). */
    object Type {
        const val TITLE = 22f
        const val SUBTITLE = 18f
        const val BODY = 16f
        const val SECONDARY = 13f
        const val CAPTION = 12f
    }
}
