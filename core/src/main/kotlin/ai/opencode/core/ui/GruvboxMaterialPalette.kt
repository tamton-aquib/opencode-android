package ai.opencode.core.ui

/**
 * sainnhe's Gruvbox Material at its defaults: medium background and the material foreground
 * palette. Link and diff text values are adjusted just enough for WCAG AA where the app draws
 * them as text; every background, surface and syntax value is the source palette's.
 */
internal object GruvboxMaterialPalette {
    private fun hex(value: String, alpha: Double = 1.0): ThemeRGBA {
        val v = value.removePrefix("#").toLong(16)
        return ThemeRGBA(((v shr 16) and 0xFF) / 255.0, ((v shr 8) and 0xFF) / 255.0, (v and 0xFF) / 255.0, alpha)
    }

    val gruvboxMaterial = ThemeDefinition(
        id = AppTheme.GruvboxMaterial,
        title = "Gruvbox Material",
        light = ThemeVariant(
            canvas = hex("#FBF1C7"),
            canvasWeak = hex("#F2E5BC"),
            codeBand = hex("#F4E8BE"),
            userBubble = hex("#EEE0B7"),
            surfaceStrong = hex("#DDCCAB"),
            text = hex("#654735"),
            bodyText = hex("#654735"),
            textStrong = hex("#4F3829"),
            textWeak = hex("#7C6F64"),
            textTertiary = hex("#928374"),
            border = hex("#E5D5AD"),
            borderStrong = hex("#A89984"),
            accent = hex("#C5600E"),
            link = hex("#5F6A1F"),
            syntaxKeyword = hex("#C14A4A"),
            syntaxString = hex("#6C782E"),
            syntaxNumber = hex("#945E80"),
            syntaxComment = hex("#928374"),
            syntaxType = hex("#B47109"),
            diffAddedFill = hex("#E6EABC"),
            diffRemovedFill = hex("#F9E0BB"),
            glassDiffAddedFill = hex("#6C782E", 0.14),
            glassDiffRemovedFill = hex("#C14A4A", 0.14),
            diffAddedText = hex("#5F6A1F"),
            diffRemovedText = hex("#B03A3C"),
            swatchCanvas = hex("#FBF1C7"),
            swatchRing = hex("#BDAE93"),
            swatchAccent = hex("#AA4700"),
        ),
        dark = ThemeVariant(
            canvas = hex("#282828"),
            canvasWeak = hex("#1B1B1B"),
            codeBand = hex("#32302F"),
            userBubble = hex("#45403D"),
            surfaceStrong = hex("#5A524C"),
            text = hex("#D4BE98"),
            bodyText = hex("#D4BE98"),
            textStrong = hex("#DDC7A1"),
            textWeak = hex("#A89984"),
            textTertiary = hex("#928374"),
            border = hex("#45403D"),
            borderStrong = hex("#7C6F64"),
            accent = hex("#E78A4E"),
            link = hex("#A9B665"),
            syntaxKeyword = hex("#EA6962"),
            syntaxString = hex("#A9B665"),
            syntaxNumber = hex("#D3869B"),
            syntaxComment = hex("#928374"),
            syntaxType = hex("#D8A657"),
            diffAddedFill = hex("#34381B"),
            diffRemovedFill = hex("#402120"),
            glassDiffAddedFill = hex("#A9B665", 0.16),
            glassDiffRemovedFill = hex("#EA6962", 0.16),
            diffAddedText = hex("#A9B665"),
            diffRemovedText = hex("#F6746C"),
            swatchCanvas = hex("#282828"),
            swatchRing = hex("#665C54"),
            swatchAccent = hex("#FB9D61"),
        ),
    )
}
