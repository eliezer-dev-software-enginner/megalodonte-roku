package megalodonte.components;

import java.nio.file.Path;
import java.util.Map;
import megalodonte.theme.Theme;

/**
 * Project-owned SceneGraph behavior packaged by the generic Roku exporter.
 * Implementations and their BrightScript live in the application project.
 */
public interface RokuExtension extends Component {
    /** Provides SceneGraph children for this page.
     * @param theme build-time palette
     * @return XML children placed inside the extension's page Group
     */
    String sceneXml(Theme theme);

    /** Lists project-owned resources to include in the ZIP.
     * @return package-relative file names mapped to local source files
     */
    Map<String, Path> files();

    /** Initializes project behavior in the generated scene.
     * @param pageIndex page position in the router
     * @param theme build-time palette
     * @return BrightScript statement that initializes the extension
     */
    String initialize(int pageIndex, Theme theme);

    /** Activates project behavior.
     * @return BrightScript statement run when the page becomes visible
     */
    String enter();

    /** Deactivates project behavior.
     * @return BrightScript statement run before the page is hidden
     */
    String leave();

    /** Handles a remote key while the page is active.
     * @return BrightScript expression handling a remote key and returning Boolean
     */
    String handleKey();
}
