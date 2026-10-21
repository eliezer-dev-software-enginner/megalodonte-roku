package megalodonte.roku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import megalodonte.components.Button;
import megalodonte.components.Counter;
import megalodonte.components.Text;
import megalodonte.components.Image;
import megalodonte.components.Slideshow;
import megalodonte.components.RokuExtension;
import megalodonte.theme.Theme;
import megalodonte.application.Router;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

final class RokuExporterTest {
    @TempDir Path temporary;

    @Test void exportsNavigablePagesAndEscapedText() throws Exception {
        Path zip = temporary.resolve("test.zip");
        RokuExporter.export(List.of(
                new RokuScene("home", "A & B", "Choose", List.of(new RokuButton("Open", "counter"))),
                new RokuScene("counter", "Count", "Use OK", List.of(RokuButton.increment("Add")),
                        new RokuCounter("Clicks", 2))), "Test", zip);
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            assertNotNull(archive.getEntry("manifest"));
            assertNotNull(archive.getEntry("source/main.brs"));
            var xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(archive.getInputStream(archive.getEntry("components/MainScene.xml")));
            assertEquals(2, xml.getElementsByTagName("Group").getLength());
            assertEquals("A & B", xml.getElementsByTagName("Label").item(0)
                    .getAttributes().getNamedItem("text").getNodeValue());
            String script = new String(archive.getInputStream(archive.getEntry("components/MainScene.brs")).readAllBytes());
            assertTrue(script.contains("m.history.Push(m.pageIndex)"));
            assertTrue(script.contains("m.counterValues[m.pageIndex] = m.counterValues[m.pageIndex] + 1"));
        }
    }

    @Test void rejectsInvalidRouteGraphsBeforeWritingZip() {
        Path zip = temporary.resolve("invalid.zip");
        assertThrows(IllegalArgumentException.class, () -> RokuExporter.export(List.of(
                new RokuScene("home", "Home", "Choose", List.of(new RokuButton("Missing", "other")))),
                "Test", zip));
        assertFalse(zip.toFile().exists());
        assertThrows(IllegalArgumentException.class, () -> new RokuScene("home", "Home", "Choose",
                List.of(RokuButton.increment("Add"))));
    }

    @Test void packagesImageAndAdditionalTextAsSceneGraphNodes() throws Exception {
        Path source = temporary.resolve("photo.png");
        Files.write(source, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg=="));
        Path zip = temporary.resolve("image.zip");
        RokuScene scene = new RokuScene("home", "Home", "Message", List.of(), null,
                List.of(new RokuText("A & B", 10, 20, 300, 40)),
                List.of(new RokuImage(source, 20, 70, 400, 250)));
        RokuExporter.export(scene, "Image test", zip);
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            assertNotNull(archive.getEntry("images/content-0.png"));
            var xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(archive.getInputStream(archive.getEntry("components/MainScene.xml")));
            assertEquals("pkg:/images/content-0.png", xml.getElementsByTagName("Poster").item(0)
                    .getAttributes().getNamedItem("uri").getNodeValue());
            assertEquals("A & B", xml.getElementsByTagName("Label").item(0)
                    .getAttributes().getNamedItem("text").getNodeValue());
        }
    }

    @Test void acceptsClientComponentsAndAppliesTheme() throws Exception {
        Path source = temporary.resolve("asset.png");
        Files.write(source, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg=="));
        RokuScene scene = new RokuScene("home", "Home", "Message",
                new Image(source, 10, 20, 100, 100), new Text("Extra", 10, 130, 200, 30),
                new Counter("Clicks", 0), Button.increment("Add"));
        Theme theme = new Theme("0x101010FF", "0x202020FF", "0x303030FF", "0x404040FF", "0x505050FF");
        Path zip = temporary.resolve("themed.zip");
        RokuExporter.export(List.of(scene), "Themed", zip, theme);
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            String xml = new String(archive.getInputStream(archive.getEntry("components/MainScene.xml")).readAllBytes());
            String script = new String(archive.getInputStream(archive.getEntry("components/MainScene.brs")).readAllBytes());
            assertTrue(xml.contains("<Poster uri=\"pkg:/images/content-0.png\""));
            assertTrue(xml.contains("text=\"Extra\""));
            assertTrue(xml.contains("color=\"0x101010FF\""));
            assertTrue(script.contains("0x505050FF"));
            assertTrue(script.contains("m.counterValues[m.pageIndex] = m.counterValues[m.pageIndex] + 1"));
        }
    }

    @Test void routerRejectsDuplicateRoutes() {
        RokuScene page = new RokuScene("home", "Home", "Welcome");
        Router router = new Router().route(page);
        assertEquals(List.of(page), router.pages());
        assertThrows(IllegalArgumentException.class, () -> router.route(page));
    }

    @Test void exportsTimedSlideshowAndItsImages() throws Exception {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg==");
        Path first = temporary.resolve("one.png");
        Path second = temporary.resolve("two.png");
        Files.write(first, png);
        Files.write(second, png);
        RokuScene ads = new RokuScene("ads", "Ads", "Rotating images",
                new Slideshow(List.of(first, second), 320, 185, 1280, 720, 5),
                new Button("Home", "home"));
        Path zip = temporary.resolve("slides.zip");
        RokuExporter.export(List.of(new RokuScene("home", "Home", "Start", new Button("Ads", "ads")), ads),
                "Slides", zip);
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            assertNotNull(archive.getEntry("images/content-0.png"));
            assertNotNull(archive.getEntry("images/content-1.png"));
            String xml = new String(archive.getInputStream(archive.getEntry("components/MainScene.xml")).readAllBytes());
            String script = new String(archive.getInputStream(archive.getEntry("components/MainScene.brs")).readAllBytes());
            assertTrue(xml.contains("<Timer id=\"slideshow_timer_1\" repeat=\"true\" duration=\"5\""));
            assertTrue(xml.contains("id=\"slide_1_0\""));
            assertTrue(xml.contains("id=\"slide_1_1\""));
            assertTrue(script.contains("timer.ObserveField(\"fire\", \"onSlideshowTick\")"));
            assertTrue(script.contains("m.slideTimers[m.pageIndex].control = \"stop\""));
            assertTrue(script.contains("m.slideTimers[index].control = \"start\""));
            assertTrue(script.contains("m.slideIndexes[index] = (m.slideIndexes[index] + 1) mod slides.Count()"));
        }
    }

    @Test void packagesProjectOwnedExtensionWithoutPuttingItInTheLibrary() throws Exception {
        Path scriptFile = temporary.resolve("LocalAction.brs");
        Files.writeString(scriptFile, "function localHandle(key as String) as Boolean\n    return key = \"OK\"\nend function\n");
        RokuExtension extension = new RokuExtension() {
            @Override public String sceneXml(Theme theme) {
                return "<Label id=\"local_label\" text=\"From project\" />";
            }
            @Override public Map<String, Path> files() {
                return Map.of("components/LocalAction.brs", scriptFile);
            }
            @Override public String initialize(int pageIndex, Theme theme) { return "m.localPage = " + pageIndex; }
            @Override public String enter() { return "m.localEntered = true"; }
            @Override public String leave() { return "m.localEntered = false"; }
            @Override public String handleKey() { return "localHandle(key)"; }
        };
        Path zip = temporary.resolve("extension.zip");
        RokuExporter.export(List.of(new RokuScene("home", "Home", "Open",
                new Button("Local", "local")),
                new RokuScene("local", "Local", "Project behavior", extension)), "Extension", zip);
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            assertNotNull(archive.getEntry("components/LocalAction.brs"));
            String xml = new String(archive.getInputStream(archive.getEntry("components/MainScene.xml")).readAllBytes());
            String script = new String(archive.getInputStream(archive.getEntry("components/MainScene.brs")).readAllBytes());
            assertTrue(xml.contains("pkg:/components/LocalAction.brs"));
            assertTrue(xml.contains("From project"));
            assertTrue(script.contains("m.localPage = 1"));
            assertTrue(script.contains("m.localEntered = true"));
            assertTrue(script.contains("return localHandle(key)"));
        }
    }
}
