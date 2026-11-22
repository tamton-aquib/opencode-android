package ai.opencode.core.ui

import ai.opencode.core.testing.TestIsolationRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Android resolves the same rules in [OpenCodeColors.resolve] (theme, dark, high contrast), so the
 * trait-collection tests assert through that.
 */
class ThemePaletteTests {
    @get:Rule val isolation = TestIsolationRule()

    private val defaultBodyTone = 0.92

    @Test
    fun allCuratedThemesResolveLightAndDarkPalettes() {
        assertEquals(
            listOf("system", "oc-2", "tokyonight", "catppuccin", "dracula", "rosepine", "github", "gruvbox-material"),
            AppTheme.entries.map { it.rawValue },
        )

        for (theme in AppTheme.entries) {
            val definition = ThemePalette.definition(theme)
            assertEquals(theme, definition.id)
            assertTrue(definition.title.isNotEmpty())

            // Light canvas is lighter than dark canvas in every theme.
            assertTrue(
                "Expected ${theme.title} light canvas to have higher luminance than dark canvas",
                definition.light.canvas.relativeLuminance > definition.dark.canvas.relativeLuminance,
            )
        }
    }

    @Test
    fun bodyTextContrastAgainstCanvasExceedsWCAGAAInEveryThemeAndVariant() {
        for (theme in AppTheme.entries) {
            val definition = ThemePalette.definition(theme)
            for ((mode, variant) in listOf("light" to definition.light, "dark" to definition.dark)) {
                val fullContrast = variant.text.contrastRatio(variant.canvas)
                assertTrue("${theme.title} ($mode) full body text contrast $fullContrast is below 4.5:1", fullContrast >= 4.5)

                val tonedContrast = variant.text.contrastRatio(variant.canvas, opacity = defaultBodyTone)
                assertTrue("${theme.title} ($mode) toned body text ($defaultBodyTone) contrast $tonedContrast is below 4.5:1", tonedContrast >= 4.5)

                val oklabBodyContrast = variant.bodyText.contrastRatio(variant.canvas)
                assertTrue("${theme.title} ($mode) OKLab-mixed bodyText contrast $oklabBodyContrast is below 4.5:1", oklabBodyContrast >= 4.5)

                val strongContrast = variant.textStrong.contrastRatio(variant.canvas)
                assertTrue("${theme.title} ($mode) strong text contrast $strongContrast is below 4.5:1", strongContrast >= 4.5)

                val bubbleContrast = variant.text.contrastRatio(variant.userBubble)
                assertTrue("${theme.title} ($mode) user bubble text contrast $bubbleContrast is below 4.5:1", bubbleContrast >= 4.5)

                val bandContrast = variant.text.contrastRatio(variant.codeBand)
                assertTrue("${theme.title} ($mode) code band text contrast $bandContrast is below 4.5:1", bandContrast >= 4.5)
            }
        }
    }

    @Test
    fun oklchDerivedSwatchesAndDiffsHaveConsistentPerceptualLightnessAndHue() {
        for (theme in AppTheme.entries) {
            if (theme == AppTheme.System) continue
            val def = ThemePalette.definition(theme)

            if (theme == AppTheme.OpenCode) {
                // The OpenCode theme's accent is the desktop's monochrome contrast fill: near-black
                // in light, near-white in dark.
                assertTrue("${theme.title} dark swatchAccent L=${def.dark.swatchAccent.oklch.first}", def.dark.swatchAccent.oklch.first > 0.85)
                assertTrue("${theme.title} light swatchAccent L=${def.light.swatchAccent.oklch.first}", def.light.swatchAccent.oklch.first < 0.35)
            } else {
                // Dark swatch accent dot sits at L ~= 0.78; light swatch accent dot sits at L ~= 0.52.
                assertTrue("${theme.title} dark swatchAccent L=${def.dark.swatchAccent.oklch.first}", abs(def.dark.swatchAccent.oklch.first - 0.78) < 0.015)
                assertTrue("${theme.title} light swatchAccent L=${def.light.swatchAccent.oklch.first}", abs(def.light.swatchAccent.oklch.first - 0.52) < 0.015)
            }

            // Swatch ring maintains a crisp perceptual lightness step from swatchCanvas, and the
            // three ThemeSwatchTile palette dots (swatchCanvas, swatchAccent, syntaxKeyword) are distinct.
            assertTrue(def.dark.swatchRing.oklch.first - def.dark.swatchCanvas.oklch.first >= 0.18)
            assertTrue(def.light.swatchCanvas.oklch.first - def.light.swatchRing.oklch.first >= 0.14)
            for (variant in listOf(def.light, def.dark)) {
                assertNotEquals(variant.swatchCanvas, variant.swatchAccent)
                assertNotEquals(variant.swatchCanvas, variant.syntaxKeyword)
                assertNotEquals(variant.swatchAccent, variant.syntaxKeyword)
            }

            // Diff added/removed text maintains >= 4.5:1 contrast over both codeBand and diff fills.
            for ((mode, variant) in listOf("light" to def.light, "dark" to def.dark)) {
                assertTrue("${theme.title} ($mode) diffAddedText vs codeBand < 4.5:1", variant.diffAddedText.contrastRatio(variant.codeBand) >= 4.5)
                assertTrue("${theme.title} ($mode) diffRemovedText vs codeBand < 4.5:1", variant.diffRemovedText.contrastRatio(variant.codeBand) >= 4.5)
            }

            // OKLab mixing interpolates lightness linearly between canvas and text.
            val mixed = def.dark.canvas.mixedInOklab(def.dark.text, weight = 0.5)
            val expectedL = 0.5 * (def.dark.canvas.oklab.first + def.dark.text.oklab.first)
            assertTrue(abs(mixed.oklab.first - expectedL) < 0.005)
        }
    }

    @Test
    fun dynamicUIColorsResolveFromTraitCollectionAndRespectIncreaseContrast() {
        val systemDark = OpenCodeColors.resolve(AppTheme.System, isDark = true)
        assertEquals(ThemePalette.system.dark.canvas.color, systemDark.canvas)
        assertEquals(ThemePalette.system.dark.userBubble.color, systemDark.userBubble)

        val tokyoDark = OpenCodeColors.resolve(AppTheme.TokyoNight, isDark = true, highContrast = false)
        val tokyoDarkHighContrast = OpenCodeColors.resolve(AppTheme.TokyoNight, isDark = true, highContrast = true)

        val variant = ThemePalette.tokyonight.dark
        assertEquals(variant.canvas.color, tokyoDark.canvas)
        assertEquals(variant.userBubble.color, tokyoDark.userBubble)
        assertEquals(variant.surfaceStrong.color, tokyoDarkHighContrast.userBubble)
        assertEquals(variant.text.color, tokyoDark.text)
        assertEquals(variant.textStrong.color, tokyoDarkHighContrast.text)
    }

    @Test
    fun settingsThemeSurfaceUpdatesAcrossThemesModesAndContrast() {
        fun surface(theme: AppTheme, isDark: Boolean, highContrast: Boolean = false) =
            OpenCodeColors.resolve(theme, isDark, highContrast).let { it.groupedCanvas to it.groupedCard }
        val systemDark = surface(AppTheme.System, isDark = true)
        val tokyoDark = surface(AppTheme.TokyoNight, isDark = true)
        val tokyoLight = surface(AppTheme.TokyoNight, isDark = false)
        val tokyoHighContrast = surface(AppTheme.TokyoNight, isDark = true, highContrast = true)

        assertNotEquals(systemDark, tokyoDark)
        assertNotEquals(tokyoDark, tokyoLight)
        assertNotEquals(tokyoDark, tokyoHighContrast)
        assertEquals(ThemePalette.tokyonight.dark.canvas.color, tokyoDark.first)
        assertEquals(ThemePalette.tokyonight.dark.userBubble.color, tokyoDark.second)
        assertEquals(ThemePalette.tokyonight.dark.surfaceStrong.color, tokyoHighContrast.second)
        assertEquals(ThemePalette.tokyonight.light.canvasWeak.color, tokyoLight.first)
        assertEquals(ThemePalette.tokyonight.light.canvas.color, tokyoLight.second)
    }
}
