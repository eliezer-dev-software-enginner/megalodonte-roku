package megalodonte.roku;

/** Build-time description of one Roku SceneGraph screen. */
public interface RokuScreen {
    RokuScene render();

    /** Override to expose multiple routes; the first page is the start page. */
    default java.util.List<RokuScene> pages() { return java.util.List.of(render()); }
}
