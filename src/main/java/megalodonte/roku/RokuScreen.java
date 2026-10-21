package megalodonte.roku;

/** Supplies the Java model that the exporter converts to Roku SceneGraph files at build time. */
public interface RokuScreen {
    /** Returns the build-time color palette for the generated application.
     * @return application palette
     */
    default megalodonte.theme.Theme theme() { return megalodonte.theme.Theme.defaultTheme(); }
    /**
     * Returns the default page. Used by {@link #pages()} for a single-page application.
     * @return the page to export
     */
    RokuScene render();

    /**
     * Returns all pages in navigation order. Override for multiple routes; the first page starts the app.
     * @return the pages to export
     */
    default java.util.List<RokuScene> pages() { return java.util.List.of(render()); }
}
