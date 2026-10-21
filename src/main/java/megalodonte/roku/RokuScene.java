package megalodonte.roku;

/** A deliberately small, portable view model for the first Roku target. */
public record RokuScene(String heading, String message) {
    public RokuScene {
        if (heading == null || heading.isBlank() || message == null || message.isBlank())
            throw new IllegalArgumentException("A Roku scene needs a heading and message");
    }
}
