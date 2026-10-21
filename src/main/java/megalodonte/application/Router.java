package megalodonte.application;

import java.util.ArrayList;
import java.util.List;
import megalodonte.roku.RokuScene;

/** Ordered route registry for a Roku screen; the first route opens on launch. */
public final class Router {
    private final List<RokuScene> pages = new ArrayList<>();

    /** Creates an empty route registry. */
    public Router() { }

    /**
     * Adds a page and rejects duplicate paths.
     * @param page page to register
     * @return this router
     */
    public Router route(RokuScene page) {
        if (page == null) throw new IllegalArgumentException("Page is required");
        if (pages.stream().anyMatch(existing -> existing.route().equals(page.route())))
            throw new IllegalArgumentException("Duplicate route: " + page.route());
        pages.add(page);
        return this;
    }

    /**
     * Returns an immutable page list for {@code RokuScreen.pages()}.
     * @return registered pages in start-to-last order
     */
    public List<RokuScene> pages() { return List.copyOf(pages); }
}
