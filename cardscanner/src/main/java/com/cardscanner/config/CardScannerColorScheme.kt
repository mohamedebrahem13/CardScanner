package com.cardscanner.config

import java.io.Serializable

data class CardScannerColorScheme(
    /**
     * Background shown behind the camera preview while
     * CameraX is starting or if the preview is unavailable.
     */
    val screenBackgroundColor: String,

    /**
     * Background behind the top scanner controls.
     */
    val topBarBackgroundColor: String,

    /**
     * Title color in the top bar.
     */
    val topBarContentColor: String,

    /**
     * Background of inactive controls, such as the close
     * button and flashlight-off button.
     */
    val controlBackgroundColor: String,

    /**
     * Icon or text color of inactive controls.
     */
    val controlContentColor: String,

    /**
     * Background of an active control, such as when the
     * flashlight is enabled.
     */
    val activeControlBackgroundColor: String,

    /**
     * Icon color of an active control.
     */
    val activeControlContentColor: String,

    /**
     * Scanner frame and frame label color.
     */
    val frameColor: String,

    /**
     * Background of the instruction panel.
     */
    val instructionBackgroundColor: String,

    /**
     * Text color inside the instruction panel.
     */
    val instructionContentColor: String
) : Serializable {

    init {
        validateColor(
            name = "screenBackgroundColor",
            value = screenBackgroundColor
        )

        validateColor(
            name = "topBarBackgroundColor",
            value = topBarBackgroundColor
        )

        validateColor(
            name = "topBarContentColor",
            value = topBarContentColor
        )

        validateColor(
            name = "controlBackgroundColor",
            value = controlBackgroundColor
        )

        validateColor(
            name = "controlContentColor",
            value = controlContentColor
        )

        validateColor(
            name = "activeControlBackgroundColor",
            value = activeControlBackgroundColor
        )

        validateColor(
            name = "activeControlContentColor",
            value = activeControlContentColor
        )

        validateColor(
            name = "frameColor",
            value = frameColor
        )

        validateColor(
            name = "instructionBackgroundColor",
            value = instructionBackgroundColor
        )

        validateColor(
            name = "instructionContentColor",
            value = instructionContentColor
        )
    }

    companion object {

        /**
         * Default light appearance.
         *
         * Eight-character Android colors use:
         * #AARRGGBB
         */
        @JvmStatic
        fun lightDefaults(): CardScannerColorScheme {
            return CardScannerColorScheme(
                screenBackgroundColor = "#FFFFFFFF",

                // 90% opaque white.
                topBarBackgroundColor = "#E6FFFFFF",
                topBarContentColor = "#FF111111",

                // 10% opaque black.
                controlBackgroundColor = "#1A000000",
                controlContentColor = "#FF111111",

                activeControlBackgroundColor = "#FF111111",
                activeControlContentColor = "#FFFFFFFF",

                frameColor = "#FFFFFFFF",

                // 90% opaque white.
                instructionBackgroundColor = "#E6FFFFFF",
                instructionContentColor = "#FF111111"
            )
        }

        /**
         * Default dark appearance.
         */
        @JvmStatic
        fun darkDefaults(): CardScannerColorScheme {
            return CardScannerColorScheme(
                screenBackgroundColor = "#FF000000",

                // 70% opaque black.
                topBarBackgroundColor = "#B3000000",
                topBarContentColor = "#FFFFFFFF",

                // 40% opaque black.
                controlBackgroundColor = "#66000000",
                controlContentColor = "#FFFFFFFF",

                activeControlBackgroundColor = "#FFFFFFFF",
                activeControlContentColor = "#FF000000",

                frameColor = "#FFFFFFFF",

                // 70% opaque black.
                instructionBackgroundColor = "#B3000000",
                instructionContentColor = "#FFFFFFFF"
            )
        }

        private val androidHexColorRegex =
            Regex(
                pattern = "^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$"
            )

        private fun validateColor(
            name: String,
            value: String
        ) {
            require(
                androidHexColorRegex.matches(value)
            ) {
                "$name must use #RRGGBB or #AARRGGBB format. " +
                        "Received: $value"
            }
        }
    }
}