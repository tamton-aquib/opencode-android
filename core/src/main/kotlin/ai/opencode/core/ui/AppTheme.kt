package ai.opencode.core.ui

import ai.opencode.core.R
import ai.opencode.core.strings.localized
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The curated starter themes in Settings → Appearance. [System] is the default palette, not
 * Material You. Stored by raw value, so don't rename the values.
 */
@Serializable
enum class AppTheme(val rawValue: String, private val brandTitle: String?) {
    @SerialName("system") System("system", null),
    @SerialName("oc-2") OpenCode("oc-2", "OpenCode"),
    @SerialName("tokyonight") TokyoNight("tokyonight", "Tokyo Night"),
    @SerialName("catppuccin") Catppuccin("catppuccin", "Catppuccin"),
    @SerialName("dracula") Dracula("dracula", "Dracula"),
    @SerialName("rosepine") RosePine("rosepine", "Rosé Pine"),
    @SerialName("github") GitHub("github", "GitHub"),
    @SerialName("gruvbox-material") GruvboxMaterial("gruvbox-material", "Gruvbox Material"),
    ;

    val id: String get() = rawValue

    /** Brand names stay as they are; only System is translated. */
    val title: String get() = brandTitle ?: localized(R.string.core_theme_system_title)

    companion object {
        fun fromRawValue(rawValue: String): AppTheme? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

/** System / Light / Dark appearance override in Settings → Appearance. */
@Serializable
enum class AppearanceColorScheme(val rawValue: String, @StringRes private val titleRes: Int) {
    @SerialName("system") System("system", R.string.core_appearance_color_scheme_system_title),
    @SerialName("light") Light("light", R.string.core_appearance_color_scheme_light_title),
    @SerialName("dark") Dark("dark", R.string.core_appearance_color_scheme_dark_title),
    ;

    val title: String get() = localized(titleRes)

    /** `null` follows the system. */
    val isDark: Boolean? get() = when (this) {
        System -> null
        Light -> false
        Dark -> true
    }

    /** Resolves against the system's current dark-mode state. */
    fun resolveIsDark(systemIsDark: Boolean): Boolean = isDark ?: systemIsDark
}

/**
 * Pre-resolved sRGB colour components (`0..1`), with WCAG contrast, OKLab/OKLCH and gamut-mapping
 * maths (used for derived tones such as [ThemePalette.bodyText] and [highContrastForeground]).
 */
data class ThemeRGBA(val r: Double, val g: Double, val b: Double, val a: Double = 1.0) {
    val color: Color get() = Color(r.toFloat(), g.toFloat(), b.toFloat(), a.toFloat())

    /** Blends `this` over an opaque [background] using `a * opacity`. */
    fun composited(background: ThemeRGBA, opacity: Double = 1.0): ThemeRGBA {
        val alpha = (a * opacity).coerceIn(0.0, 1.0)
        if (alpha >= 0.9999) return ThemeRGBA(r, g, b, 1.0)
        return ThemeRGBA(
            r * alpha + background.r * (1 - alpha),
            g * alpha + background.g * (1 - alpha),
            b * alpha + background.b * (1 - alpha),
            1.0,
        )
    }

    /** WCAG 2.1 relative luminance (`0..1`). */
    val relativeLuminance: Double
        get() = 0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)

    /** WCAG 2.1 contrast ratio (`1..21`) against [background]. */
    fun contrastRatio(against: ThemeRGBA, opacity: Double = 1.0): Double {
        val l1 = composited(against, opacity).relativeLuminance
        val l2 = against.relativeLuminance
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    /** Björn Ottosson's perceptual OKLab `(L, a, b)`. */
    val oklab: Triple<Double, Double, Double>
        get() {
            val lr = linearize(r)
            val lg = linearize(g)
            val lb = linearize(b)
            val l = cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
            val m = cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
            val s = cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
            return Triple(
                0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s,
            )
        }

    /** Cylindrical OKLCH `(L, C, h°)` with `h` in `0 until 360`. */
    val oklch: Triple<Double, Double, Double>
        get() {
            val (l, labA, labB) = oklab
            val c = hypot(labA, labB)
            val rawH = if (c < 1e-6) 0.0 else atan2(labB, labA) * 180 / PI
            return Triple(l, c, ((rawH % 360) + 360) % 360)
        }

    /** Perceptually uniform interpolation in Cartesian OKLab toward [other]. */
    fun mixedInOklab(toward: ThemeRGBA, weight: Double, alpha: Double = 1.0): ThemeRGBA {
        val t = weight.coerceIn(0.0, 1.0)
        val c1 = oklab
        val c2 = toward.oklab
        val l = c1.first + (c2.first - c1.first) * t
        val labA = c1.second + (c2.second - c1.second) * t
        val labB = c1.third + (c2.third - c1.third) * t
        val c = hypot(labA, labB)
        val h = if (c < 1e-6) 0.0 else atan2(labB, labA) * 180 / PI
        return fromOklch(l, c, h, alpha)
    }

    /** Crisp dark or white foreground for badges and the Send button over `this`. */
    val highContrastForeground: ThemeRGBA
        get() {
            val dark = ThemeRGBA(0.0667, 0.0667, 0.0784)
            val light = ThemeRGBA(1.0, 1.0, 1.0)
            return if (light.contrastRatio(this) >= dark.contrastRatio(this)) light else dark
        }

    fun withAlpha(alpha: Double): ThemeRGBA = copy(a = alpha)

    companion object {
        private fun linearize(channel: Double): Double {
            val c = channel.coerceIn(0.0, 1.0)
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }

        /**
         * OKLCH → sRGB with CSS Color 4 binary-search chroma gamut mapping, holding lightness and
         * hue constant.
         */
        fun fromOklch(l: Double, c: Double, h: Double, a: Double = 1.0): ThemeRGBA {
            val clampedL = l.coerceIn(0.0, 1.0)
            if (clampedL <= 0) return ThemeRGBA(0.0, 0.0, 0.0, a)
            if (clampedL >= 1) return ThemeRGBA(1.0, 1.0, 1.0, a)
            val rad = h * PI / 180
            val cosH = cos(rad)
            val sinH = sin(rad)

            fun gamma(x: Double): Double =
                if (x <= 0.0031308) 12.92 * x else 1.055 * max(x, 0.0).pow(1 / 2.4) - 0.055

            fun toSrgb(chroma: Double): DoubleArray {
                val labA = chroma * cosH
                val labB = chroma * sinH
                val ll = clampedL + 0.3963377774 * labA + 0.2158037573 * labB
                val mm = clampedL - 0.1055613458 * labA - 0.0638541728 * labB
                val ss = clampedL - 0.0894841775 * labA - 1.2914855480 * labB
                val l3 = ll * ll * ll
                val m3 = mm * mm * mm
                val s3 = ss * ss * ss
                return doubleArrayOf(
                    gamma(4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3),
                    gamma(-1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3),
                    gamma(-0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3),
                )
            }

            fun inGamut(rgb: DoubleArray, eps: Double = 1e-4): Boolean = rgb.all { it >= -eps && it <= 1 + eps }

            fun clamp(rgb: DoubleArray) = ThemeRGBA(
                rgb[0].coerceIn(0.0, 1.0),
                rgb[1].coerceIn(0.0, 1.0),
                rgb[2].coerceIn(0.0, 1.0),
                a,
            )

            val targetC = max(0.0, c)
            val direct = toSrgb(targetC)
            if (inGamut(direct)) return clamp(direct)
            var low = 0.0
            var high = targetC
            repeat(24) {
                val mid = 0.5 * (low + high)
                if (inGamut(toSrgb(mid))) low = mid else high = mid
            }
            return clamp(toSrgb(low))
        }
    }
}

/**
 * One resolved light or dark palette for a theme. Derived surfaces, prose mixes and swatch tokens
 * are precomputed in OKLab/OKLCH.
 */
data class ThemeVariant(
    val canvas: ThemeRGBA,
    val canvasWeak: ThemeRGBA,
    val codeBand: ThemeRGBA,
    val userBubble: ThemeRGBA,
    val surfaceStrong: ThemeRGBA,
    val text: ThemeRGBA,
    val bodyText: ThemeRGBA,
    val textStrong: ThemeRGBA,
    val textWeak: ThemeRGBA,
    val textTertiary: ThemeRGBA,
    val border: ThemeRGBA,
    val borderStrong: ThemeRGBA,
    val accent: ThemeRGBA,
    val link: ThemeRGBA,
    val syntaxKeyword: ThemeRGBA,
    val syntaxString: ThemeRGBA,
    val syntaxNumber: ThemeRGBA,
    val syntaxComment: ThemeRGBA,
    val syntaxType: ThemeRGBA,
    val diffAddedFill: ThemeRGBA,
    val diffRemovedFill: ThemeRGBA,
    val glassDiffAddedFill: ThemeRGBA,
    val glassDiffRemovedFill: ThemeRGBA,
    val diffAddedText: ThemeRGBA,
    val diffRemovedText: ThemeRGBA,
    val swatchCanvas: ThemeRGBA,
    val swatchRing: ThemeRGBA,
    val swatchAccent: ThemeRGBA,
)

/** Light and dark variants for one [AppTheme]. */
data class ThemeDefinition(
    val id: AppTheme,
    val title: String,
    val light: ThemeVariant,
    val dark: ThemeVariant,
) {
    fun variant(isDark: Boolean): ThemeVariant = if (isDark) dark else light
}

/**
 * Every theme's palette. Resolve semantic colours for the UI through [OpenCodeColors]
 * (`OpenCodeTheme.colors`), not these raw variants; raw variants are for swatches and contrast
 * checks.
 */
object ThemePalette {
    /** Reference palette for [AppTheme.System]. */
    val system = ThemeDefinition(
        id = AppTheme.System,
        title = "System",
        light = ThemeVariant(
            canvas = ThemeRGBA(1.0, 1.0, 1.0),
            canvasWeak = ThemeRGBA(0.949, 0.949, 0.9686),
            codeBand = ThemeRGBA(0.975, 0.975, 0.975),
            userBubble = ThemeRGBA(0.949, 0.949, 0.9686),
            surfaceStrong = ThemeRGBA(0.898, 0.898, 0.9176),
            text = ThemeRGBA(0.0, 0.0, 0.0),
            bodyText = ThemeRGBA(0.08, 0.08, 0.08),
            textStrong = ThemeRGBA(0.0, 0.0, 0.0),
            textWeak = ThemeRGBA(0.2353, 0.2353, 0.2627, 0.6),
            textTertiary = ThemeRGBA(0.2353, 0.2353, 0.2627, 0.3),
            border = ThemeRGBA(0.2353, 0.2353, 0.2627, 0.29),
            borderStrong = ThemeRGBA(0.2353, 0.2353, 0.2627, 0.45),
            accent = ThemeRGBA(0.0, 0.4784, 1.0),
            link = ThemeRGBA(0.0, 0.4784, 1.0),
            syntaxKeyword = ThemeRGBA(200.0 / 255, 61.0 / 255, 139.0 / 255),
            syntaxString = ThemeRGBA(25.0 / 255, 139.0 / 255, 67.0 / 255),
            syntaxNumber = ThemeRGBA(200.0 / 255, 61.0 / 255, 139.0 / 255),
            syntaxComment = ThemeRGBA(0.38, 0.40, 0.43),
            syntaxType = ThemeRGBA(82.0 / 255, 48.0 / 255, 194.0 / 255),
            diffAddedFill = ThemeRGBA(0.2039, 0.7804, 0.349, 0.16),
            diffRemovedFill = ThemeRGBA(1.0, 0.2314, 0.1882, 0.14),
            glassDiffAddedFill = ThemeRGBA(0.2039, 0.7804, 0.349, 0.16),
            glassDiffRemovedFill = ThemeRGBA(1.0, 0.2314, 0.1882, 0.14),
            diffAddedText = ThemeRGBA(0.0, 0.0, 0.0),
            diffRemovedText = ThemeRGBA(0.0, 0.0, 0.0),
            swatchCanvas = ThemeRGBA(0.9803, 0.9803, 0.9803),
            swatchRing = ThemeRGBA(0.7745, 0.7745, 0.7745),
            swatchAccent = ThemeRGBA(0.1095, 0.4017, 0.7717),
        ),
        dark = ThemeVariant(
            canvas = ThemeRGBA(0.0, 0.0, 0.0),
            canvasWeak = ThemeRGBA(0.1098, 0.1098, 0.1176),
            codeBand = ThemeRGBA(0.035, 0.035, 0.035),
            userBubble = ThemeRGBA(0.1098, 0.1098, 0.1176),
            surfaceStrong = ThemeRGBA(0.1725, 0.1725, 0.1804),
            text = ThemeRGBA(1.0, 1.0, 1.0),
            bodyText = ThemeRGBA(0.92, 0.92, 0.92),
            textStrong = ThemeRGBA(1.0, 1.0, 1.0),
            textWeak = ThemeRGBA(0.9216, 0.9216, 0.9608, 0.6),
            textTertiary = ThemeRGBA(0.9216, 0.9216, 0.9608, 0.3),
            border = ThemeRGBA(0.3294, 0.3294, 0.3451, 0.6),
            borderStrong = ThemeRGBA(0.45, 0.45, 0.48, 0.8),
            accent = ThemeRGBA(0.0392, 0.5176, 1.0),
            link = ThemeRGBA(0.0392, 0.5176, 1.0),
            syntaxKeyword = ThemeRGBA(247.0 / 255, 153.0 / 255, 198.0 / 255),
            syntaxString = ThemeRGBA(150.0 / 255, 227.0 / 255, 166.0 / 255),
            syntaxNumber = ThemeRGBA(247.0 / 255, 153.0 / 255, 198.0 / 255),
            syntaxComment = ThemeRGBA(0.55, 0.58, 0.62),
            syntaxType = ThemeRGBA(158.0 / 255, 153.0 / 255, 247.0 / 255),
            diffAddedFill = ThemeRGBA(0.1882, 0.8196, 0.3451, 0.16),
            diffRemovedFill = ThemeRGBA(1.0, 0.2706, 0.2275, 0.14),
            glassDiffAddedFill = ThemeRGBA(0.1882, 0.8196, 0.3451, 0.16),
            glassDiffRemovedFill = ThemeRGBA(1.0, 0.2706, 0.2275, 0.14),
            diffAddedText = ThemeRGBA(1.0, 1.0, 1.0),
            diffRemovedText = ThemeRGBA(1.0, 1.0, 1.0),
            swatchCanvas = ThemeRGBA(0.0566, 0.0566, 0.0566),
            swatchRing = ThemeRGBA(0.2747, 0.2747, 0.2747),
            swatchAccent = ThemeRGBA(0.4494, 0.7429, 1.0),
        ),
    )

    val opencode: ThemeDefinition get() = GeneratedThemePalettes.opencode
    val tokyonight: ThemeDefinition get() = GeneratedThemePalettes.tokyonight
    val catppuccin: ThemeDefinition get() = GeneratedThemePalettes.catppuccin
    val dracula: ThemeDefinition get() = GeneratedThemePalettes.dracula
    val rosepine: ThemeDefinition get() = GeneratedThemePalettes.rosepine
    val github: ThemeDefinition get() = GeneratedThemePalettes.github
    val gruvboxMaterial: ThemeDefinition get() = GruvboxMaterialPalette.gruvboxMaterial

    fun definition(theme: AppTheme): ThemeDefinition = when (theme) {
        AppTheme.System -> system
        AppTheme.OpenCode -> BrandPalette.opencode
        AppTheme.TokyoNight -> tokyonight
        AppTheme.Catppuccin -> catppuccin
        AppTheme.Dracula -> dracula
        AppTheme.RosePine -> rosepine
        AppTheme.GitHub -> github
        AppTheme.GruvboxMaterial -> gruvboxMaterial
    }

    /** Windows crossfade for this long when the theme or appearance changes. */
    const val THEME_CHANGE_CROSSFADE_SECONDS = 0.22

    /** Never read on Android. Kept for the key list. */
    const val LEGACY_TRANSCRIPT_RHYTHM_STORAGE_KEY = "appearance-transcript-rhythm"
}
