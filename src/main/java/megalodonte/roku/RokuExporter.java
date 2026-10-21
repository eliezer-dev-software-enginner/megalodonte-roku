package megalodonte.roku;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Converts Java page models into a Roku sideload ZIP at build time. */
public final class RokuExporter {
    private RokuExporter() { }

    /**
     * Exports a screen class from the command line.
     * @param args screen class name, application title, output ZIP path
     * @throws Exception if the class cannot be loaded or the ZIP cannot be written
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("Usage: RokuExporter <screen-class> <title> <zip>");
        Class<?> type = Class.forName(args[0]);
        RokuScreen screen = (RokuScreen) type.getDeclaredConstructor().newInstance();
        export(screen.pages(), args[1], Path.of(args[2]));
    }

    /**
     * Exports a single-page application.
     * @param scene page to export
     * @param title title shown by Roku
     * @param zip destination ZIP path
     * @throws IOException if the ZIP cannot be written
     */
    public static void export(RokuScene scene, String title, Path zip) throws IOException {
        export(List.of(scene), title, zip);
    }

    /**
     * Exports a multi-page application; the first page is the start page.
     * All navigation targets must name a route in {@code pages}.
     * @param pages pages in navigation order
     * @param title title shown by Roku
     * @param zip destination ZIP path
     * @throws IllegalArgumentException if routes or title are invalid
     * @throws IOException if the ZIP cannot be written
     */
    public static void export(List<RokuScene> pages, String title, Path zip) throws IOException {
        if (title == null || title.isBlank() || title.contains("\n") || title.contains("\r") || title.contains("="))
            throw new IllegalArgumentException("Invalid Roku application title");
        if (pages == null || pages.isEmpty()) throw new IllegalArgumentException("At least one page is required");
        pages = List.copyOf(pages);
        Map<String, Integer> routes = new LinkedHashMap<>();
        for (int index = 0; index < pages.size(); index++) {
            if (routes.putIfAbsent(pages.get(index).route(), index) != null)
                throw new IllegalArgumentException("Duplicate route: " + pages.get(index).route());
        }
        for (RokuScene page : pages) for (RokuButton button : page.buttons()) {
            if (button.action() == RokuButton.Action.NAVIGATE && !routes.containsKey(button.targetRoute()))
                throw new IllegalArgumentException("Unknown route: " + button.targetRoute());
        }
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
                    scene.SetFocus(true)
                    while true
                        msg = wait(0, port)
                        if type(msg) = "roSGScreenEvent" and msg.IsScreenClosed() then return
                    end while
                end sub
                """.getBytes(StandardCharsets.UTF_8));
        files.put("components/MainScene.xml", sceneXml(pages).getBytes(StandardCharsets.UTF_8));
        files.put("components/MainScene.brs", sceneScript(pages, routes).getBytes(StandardCharsets.UTF_8));
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

    private static String sceneXml(List<RokuScene> pages) {
        StringBuilder result = new StringBuilder("""
                <?xml version="1.0" encoding="utf-8"?>
                <component name="MainScene" extends="Scene">
                  <script type="text/brightscript" uri="pkg:/components/MainScene.brs" />
                  <children>
                    <Rectangle width="1920" height="1080" color="0x142536FF" />
                """);
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            RokuScene page = pages.get(pageIndex);
            result.append("    <Group id=\"page_").append(pageIndex).append("\" visible=\"")
                    .append(pageIndex == 0).append("\">\n")
                    .append("      <Label text=\"").append(xml(page.heading()))
                    .append("\" translation=\"[180, 230]\" width=\"1560\" height=\"100\" horizAlign=\"center\" color=\"0x7CE0D6FF\" />\n")
                    .append("      <Label text=\"").append(xml(page.message()))
                    .append("\" translation=\"[180, 370]\" width=\"1560\" height=\"150\" horizAlign=\"center\" color=\"0xFFFFFFFF\" />\n");
            if (page.counter() != null) result.append("      <Label id=\"counter_").append(pageIndex)
                    .append("\" text=\"").append(xml(page.counter().label())).append(": ")
                    .append(page.counter().initialValue())
                    .append("\" translation=\"[180, 520]\" width=\"1560\" height=\"100\" horizAlign=\"center\" color=\"0xFFFFFFFF\" />\n");
            int top = page.counter() == null ? 570 : 650;
            for (int buttonIndex = 0; buttonIndex < page.buttons().size(); buttonIndex++) {
                result.append("      <Rectangle id=\"button_bg_").append(pageIndex).append('_').append(buttonIndex)
                        .append("\" translation=\"[610, ").append(top + buttonIndex * 85)
                        .append("]\" width=\"700\" height=\"66\" color=\"0x31566EFF\" />\n")
                        .append("      <Label text=\"").append(xml(page.buttons().get(buttonIndex).label()))
                        .append("\" translation=\"[620, ").append(top + buttonIndex * 85 + 10)
                        .append("]\" width=\"680\" height=\"50\" horizAlign=\"center\" color=\"0xFFFFFFFF\" />\n");
            }
            result.append("    </Group>\n");
        }
        return result.append("  </children>\n</component>\n").toString();
    }

    private static String sceneScript(List<RokuScene> pages, Map<String, Integer> routes) {
        StringBuilder result = new StringBuilder("""
                sub init()
                    m.pages = []
                    m.buttonRects = []
                    m.targets = []
                    m.counterLabels = []
                    m.counterNames = []
                    m.counterValues = []
                    m.history = []
                """);
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            RokuScene page = pages.get(pageIndex);
            result.append("    m.pages.Push(m.top.findNode(\"page_").append(pageIndex).append("\"))\n")
                    .append("    m.buttonRects.Push([");
            for (int buttonIndex = 0; buttonIndex < page.buttons().size(); buttonIndex++) {
                if (buttonIndex > 0) result.append(", ");
                result.append("m.top.findNode(\"button_bg_").append(pageIndex).append('_').append(buttonIndex).append("\")");
            }
            result.append("])\n    m.targets.Push([");
            for (int buttonIndex = 0; buttonIndex < page.buttons().size(); buttonIndex++) {
                if (buttonIndex > 0) result.append(", ");
                RokuButton button = page.buttons().get(buttonIndex);
                result.append(button.action() == RokuButton.Action.INCREMENT ? -1 : routes.get(button.targetRoute()));
            }
            result.append("])\n");
            if (page.counter() == null) {
                result.append("    m.counterLabels.Push(invalid)\n    m.counterNames.Push(\"\")\n    m.counterValues.Push(0)\n");
            } else {
                result.append("    m.counterLabels.Push(m.top.findNode(\"counter_").append(pageIndex).append("\"))\n")
                        .append("    m.counterNames.Push(\"").append(brs(page.counter().label())).append("\")\n")
                        .append("    m.counterValues.Push(").append(page.counter().initialValue()).append(")\n");
            }
        }
        return result.append("""
                    showPage(0)
                end sub

                sub showPage(index as Integer)
                    for i = 0 to m.pages.Count() - 1
                        m.pages[i].visible = (i = index)
                    end for
                    m.pageIndex = index
                    m.selection = 0
                    updateFocus()
                end sub

                sub updateFocus()
                    buttons = m.buttonRects[m.pageIndex]
                    if buttons.Count() = 0 then return
                    for i = 0 to buttons.Count() - 1
                        if i = m.selection then buttons[i].color = "0x237E9DFF" else buttons[i].color = "0x31566EFF"
                    end for
                end sub

                function onKeyEvent(key as String, press as Boolean) as Boolean
                    if not press then return false
                    count = m.buttonRects[m.pageIndex].Count()
                    if key = "down" and count > 0
                        m.selection = (m.selection + 1) mod count
                        updateFocus()
                        return true
                    else if key = "up" and count > 0
                        m.selection = (m.selection + count - 1) mod count
                        updateFocus()
                        return true
                    else if key = "OK" and count > 0
                        target = m.targets[m.pageIndex][m.selection]
                        if target = -1
                            m.counterValues[m.pageIndex] = m.counterValues[m.pageIndex] + 1
                            m.counterLabels[m.pageIndex].text = m.counterNames[m.pageIndex] + ": " + m.counterValues[m.pageIndex].ToStr()
                        else
                            m.history.Push(m.pageIndex)
                            showPage(target)
                        end if
                        return true
                    else if key = "back" and m.history.Count() > 0
                        showPage(m.history.Pop())
                        return true
                    end if
                    return false
                end function
                """).toString();
    }

    private static String brs(String value) {
        return value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }
}
