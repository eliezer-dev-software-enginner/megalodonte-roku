package megalodonte.roku;

import java.util.List;
import java.util.Arrays;
import megalodonte.components.Component;
import megalodonte.components.Button;
import megalodonte.components.Text;
import megalodonte.components.Image;
import megalodonte.components.Counter;
import megalodonte.components.Slideshow;
import megalodonte.components.RokuExtension;

/**
 * One named page with text and remote-control actions.
 *
 * @param route unique lowercase route used by navigation buttons
 * @param heading page heading
 * @param message page body text
 * @param buttons up to five focusable actions
 * @param counter optional counter, or {@code null} when absent
 * @param texts additional static labels
 * @param images local images to embed as posters
 * @param slideshow optional timed image sequence
 * @param extension optional project-owned SceneGraph component
 */
public record RokuScene(String route, String heading, String message, List<RokuButton> buttons,
                        RokuCounter counter, List<RokuText> texts, List<RokuImage> images,
                        Slideshow slideshow, RokuExtension extension) {
    /** Validates the route and actions, and copies the button list. */
    public RokuScene {
        if (route == null || !route.matches("[a-z][a-z0-9-]*"))
            throw new IllegalArgumentException("Route must be lowercase kebab-case");
        if (heading == null || heading.isBlank() || message == null || message.isBlank())
            throw new IllegalArgumentException("A Roku scene needs a heading and message");
        buttons = List.copyOf(buttons);
        texts = List.copyOf(texts);
        images = List.copyOf(images);
        if (buttons.size() > 5) throw new IllegalArgumentException("A Roku scene supports up to five buttons");
        if (counter == null && buttons.stream().anyMatch(button -> button.action() == RokuButton.Action.INCREMENT))
            throw new IllegalArgumentException("An increment button needs a counter on the same page");
        if (extension != null && (slideshow != null || counter != null || !buttons.isEmpty()))
            throw new IllegalArgumentException("An extension page owns its controls and cannot include buttons, a counter, or a slideshow");
    }

    /**
     * Creates a static page at the {@code home} route.
     * @param heading page heading
     * @param message page body text
     */
    public RokuScene(String heading, String message) {
        this("home", heading, message, List.of(), null, List.of(), List.of(), null, null);
    }

    /**
     * Creates a page without a counter.
     * @param route unique lowercase route
     * @param heading page heading
     * @param message page body text
     * @param buttons focusable actions
     */
    public RokuScene(String route, String heading, String message, List<RokuButton> buttons) {
        this(route, heading, message, buttons, null, List.of(), List.of(), null, null);
    }

    /**
     * Creates a page with a counter and no additional content.
     * @param route unique lowercase route
     * @param heading page heading
     * @param message page body text
     * @param buttons focusable actions
     * @param counter reactive counter
     */
    public RokuScene(String route, String heading, String message, List<RokuButton> buttons, RokuCounter counter) {
        this(route, heading, message, buttons, counter, List.of(), List.of(), null, null);
    }

    /**
     * Preserves the constructor used before slideshow support.
     * @param route unique lowercase route
     * @param heading page heading
     * @param message page body text
     * @param buttons focusable actions
     * @param counter optional counter
     * @param texts additional labels
     * @param images static posters
     */
    public RokuScene(String route, String heading, String message, List<RokuButton> buttons,
                     RokuCounter counter, List<RokuText> texts, List<RokuImage> images) {
        this(route, heading, message, buttons, counter, texts, images, null, null);
    }

    /** Preserves the constructor used before extension support.
     * @param route unique lowercase route
     * @param heading page heading
     * @param message page body text
     * @param buttons focusable actions
     * @param counter optional counter
     * @param texts additional labels
     * @param images static posters
     * @param slideshow optional timed image sequence
     */
    public RokuScene(String route, String heading, String message, List<RokuButton> buttons,
                     RokuCounter counter, List<RokuText> texts, List<RokuImage> images, Slideshow slideshow) {
        this(route, heading, message, buttons, counter, texts, images, slideshow, null);
    }

    /**
     * Creates a page from the client-facing Megalodonte component names.
     * Buttons are focusable; images and text are static content.
     * @param route unique lowercase route
     * @param heading page heading
     * @param message page body text
     * @param components supported Roku components
     */
    public RokuScene(String route, String heading, String message, Component... components) {
        this(route, heading, message,
                Arrays.stream(components).filter(Button.class::isInstance).map(Button.class::cast)
                        .map(button -> button.action() == Button.Action.INCREMENT
                                ? RokuButton.increment(button.label())
                                : new RokuButton(button.label(), button.targetRoute())).toList(),
                counterFrom(components),
                Arrays.stream(components).filter(Text.class::isInstance).map(Text.class::cast)
                        .map(text -> new RokuText(text.value(), text.x(), text.y(), text.width(), text.height())).toList(),
                Arrays.stream(components).filter(Image.class::isInstance).map(Image.class::cast)
                        .map(image -> new RokuImage(image.source(), image.x(), image.y(), image.width(), image.height())).toList(),
                slideshowFrom(components), extensionFrom(components));
    }

    private static RokuCounter counterFrom(Component[] components) {
        List<Counter> counters = Arrays.stream(components).filter(Counter.class::isInstance)
                .map(Counter.class::cast).toList();
        if (counters.size() > 1) throw new IllegalArgumentException("Only one counter is supported per page");
        return counters.isEmpty() ? null : new RokuCounter(counters.getFirst().label(), counters.getFirst().initialValue());
    }

    private static Slideshow slideshowFrom(Component[] components) {
        List<Slideshow> slideshows = Arrays.stream(components).filter(Slideshow.class::isInstance)
                .map(Slideshow.class::cast).toList();
        if (slideshows.size() > 1) throw new IllegalArgumentException("Only one slideshow is supported per page");
        return slideshows.isEmpty() ? null : slideshows.getFirst();
    }

    private static RokuExtension extensionFrom(Component[] components) {
        List<RokuExtension> extensions = Arrays.stream(components).filter(RokuExtension.class::isInstance)
                .map(RokuExtension.class::cast).toList();
        if (extensions.size() > 1) throw new IllegalArgumentException("Only one extension is supported per page");
        return extensions.isEmpty() ? null : extensions.getFirst();
    }
}
