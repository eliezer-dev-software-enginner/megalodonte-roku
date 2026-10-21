package megalodonte.roku;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.List;
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
}
