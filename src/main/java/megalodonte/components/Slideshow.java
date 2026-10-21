package megalodonte.components;

import java.nio.file.Path;
import java.util.List;

/**
 * Local images displayed in a loop by a Roku SceneGraph Timer.
 * @param sources PNG or JPEG files on the build machine
 * @param x horizontal position
 * @param y vertical position
 * @param width poster width
 * @param height poster height
 * @param intervalSeconds time each image remains visible
 */
public record Slideshow(List<Path> sources, int x, int y, int width, int height, int intervalSeconds)
        implements Component {
    /** Validates image paths, bounds, and interval. */
    public Slideshow {
        sources = List.copyOf(sources);
        if (sources.isEmpty() || sources.stream().anyMatch(path -> path == null))
            throw new IllegalArgumentException("Slideshow requires images");
        if (x < 0 || y < 0 || width <= 0 || height <= 0 || intervalSeconds < 1)
            throw new IllegalArgumentException("Slideshow bounds and interval must be positive");
    }
}
