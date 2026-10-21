package megalodonte.roku;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Executes a Java screen at build time and writes a Roku sideload ZIP. */
public final class RokuExporter {
    private RokuExporter() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: RokuExporter <screen-class> <title> <zip>");
        Class<?> type = Class.forName(args[0]);
        RokuScreen screen = (RokuScreen) type.getDeclaredConstructor().newInstance();
        export(screen.render(), args[1], Path.of(args[2]));
    }

    public static void export(RokuScene scene, String title, Path zip) throws IOException {
        if (title == null || title.isBlank() || title.contains("\n") || title.contains("\r") || title.contains("="))
            throw new IllegalArgumentException("Invalid Roku application title");
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("manifest", ("title=" + title + "\nmajor_version=0\nminor_version=1\nbuild_version=00001\n"
                + "ui_resolutions=fhd\nmm_icon_focus_hd=pkg:/images/icon.png\n"
                + "mm_icon_focus_sd=pkg:/images/icon.png\n").getBytes(StandardCharsets.UTF_8));
        files.put("source/main.brs", """
                sub Main()
                    screen = CreateObject("roSGScreen")
                    port = CreateObject("roMessagePort")
                    screen.SetMessagePort(port)
                    scene = screen.CreateScene("MainScene")
                    screen.Show()
                    while true
                        msg = wait(0, port)
                        if type(msg) = "roSGScreenEvent" and msg.IsScreenClosed() then return
                    end while
                end sub
                """.getBytes(StandardCharsets.UTF_8));
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <component name="MainScene" extends="Scene">
                  <children>
                    <Rectangle width="1920" height="1080" color="0x142536FF" />
                    <Label text="%s" translation="[180, 340]" width="1560" height="100" horizAlign="center" color="0x7CE0D6FF" />
                    <Label text="%s" translation="[180, 470]" width="1560" height="180" horizAlign="center" color="0xFFFFFFFF" />
                  </children>
                </component>
                """.formatted(xml(scene.heading()), xml(scene.message()));
        files.put("components/MainScene.xml", xml.getBytes(StandardCharsets.UTF_8));
        try (var icon = RokuExporter.class.getResourceAsStream("/megalodonte/roku/icon.png")) {
            if (icon == null) throw new IOException("Missing Roku icon resource");
            files.put("images/icon.png", icon.readAllBytes());
        }
        Path parent = zip.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        try (OutputStream output = Files.newOutputStream(zip); ZipOutputStream archive = new ZipOutputStream(output)) {
            for (var file : files.entrySet()) {
                archive.putNextEntry(new ZipEntry(file.getKey()));
                archive.write(file.getValue());
                archive.closeEntry();
            }
        }
    }

    private static String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
