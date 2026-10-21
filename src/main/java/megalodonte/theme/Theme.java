package megalodonte.theme;

/** Build-time palette for the generated Roku SceneGraph screen.
 * @param background scene background color
 * @param heading heading color
 * @param text body and button text color
 * @param button idle button color
 * @param focusedButton selected button color
 */
public record Theme(String background, String heading, String text, String button, String focusedButton) {
    /** Validates SceneGraph colors in {@code 0xRRGGBBAA} form. */
    public Theme {
        for (String color : new String[] {background, heading, text, button, focusedButton})
            if (color == null || !color.matches("0x[0-9a-fA-F]{8}"))
                throw new IllegalArgumentException("Theme colors must use 0xRRGGBBAA");
    }

    /** Returns the default Megalodonte Roku palette.
     * @return default palette
     */
    public static Theme defaultTheme() {
        return new Theme("0x142536FF", "0x7CE0D6FF", "0xFFFFFFFF", "0x31566EFF", "0x237E9DFF");
    }
}
