package megalodonte.roku;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import megalodonte.theme.Theme;
import megalodonte.components.RokuExtension;

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
        export(screen.pages(), args[1], Path.of(args[2]), screen.theme());
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
        export(pages, title, zip, Theme.defaultTheme());
    }

    /**
     * Exports pages with a custom build-time palette.
     * @param pages pages in navigation order
     * @param title title shown by Roku
     * @param zip destination ZIP path
     * @param theme color palette
     * @throws IOException if the ZIP cannot be written
     */
    public static void export(List<RokuScene> pages, String title, Path zip, Theme theme) throws IOException {
        if (theme == null) throw new IllegalArgumentException("Theme is required");
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
        int extensionPage = -1;
        for (int index = 0; index < pages.size(); index++) if (pages.get(index).extension() != null) {
            if (extensionPage >= 0) throw new IllegalArgumentException("Only one project extension is supported");
            extensionPage = index;
        }
        Map<String, byte[]> files = new LinkedHashMap<>();
        if (extensionPage >= 0) {
            RokuExtension extension = pages.get(extensionPage).extension();
            for (var file : extension.files().entrySet()) {
                String entry = file.getKey();
                if (!entry.matches("(components|media)/[A-Za-z0-9._/-]+") || entry.contains(".."))
                    throw new IllegalArgumentException("Invalid extension package path: " + entry);
                if (entry.equals("components/MainScene.xml") || entry.equals("components/MainScene.brs"))
                    throw new IllegalArgumentException("Reserved Roku package path: " + entry);
                files.put(entry, Files.readAllBytes(file.getValue()));
            }
        }
        Map<RokuImage, String> imageUris = new LinkedHashMap<>();
        for (RokuScene page : pages) for (RokuImage image : allImages(page)) {
            if (imageUris.containsKey(image)) continue;
            byte[] bytes = Files.readAllBytes(image.source());
            String extension;
            if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P'
                    && bytes[2] == 'N' && bytes[3] == 'G') extension = ".png";
            else if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
                    && bytes[2] == (byte) 0xff) extension = ".jpg";
            else throw new IllegalArgumentException("Roku images must be PNG or JPEG: " + image.source());
            String entry = "images/content-" + imageUris.size() + extension;
            imageUris.put(image, "pkg:/" + entry);
            files.put(entry, bytes);
        }
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
        files.put("components/MainScene.xml", sceneXml(pages, imageUris, theme, extensionPage).getBytes(StandardCharsets.UTF_8));
        files.put("components/MainScene.brs", sceneScript(pages, routes, theme, extensionPage).getBytes(StandardCharsets.UTF_8));
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

    private static List<RokuImage> allImages(RokuScene page) {
        List<RokuImage> images = new ArrayList<>(page.images());
        if (page.slideshow() != null) for (Path source : page.slideshow().sources())
            images.add(new RokuImage(source, page.slideshow().x(), page.slideshow().y(),
                    page.slideshow().width(), page.slideshow().height()));
        return images;
    }

    private static String sceneXml(List<RokuScene> pages, Map<RokuImage, String> imageUris, Theme theme, int extensionPage) {
        StringBuilder result = new StringBuilder("""
                <?xml version="1.0" encoding="utf-8"?>
                <component name="MainScene" extends="Scene">
                  <script type="text/brightscript" uri="pkg:/components/MainScene.brs" />
                  <children>
                    <Rectangle width="1920" height="1080" color="%s" />
                """);
        result = new StringBuilder(result.toString().formatted(theme.background()));
        if (extensionPage >= 0) for (String name : pages.get(extensionPage).extension().files().keySet())
            if (name.startsWith("components/") && name.endsWith(".brs"))
                result.insert(result.indexOf("  <children>"),
                        "  <script type=\"text/brightscript\" uri=\"pkg:/" + name + "\" />\n");
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            RokuScene page = pages.get(pageIndex);
            result.append("    <Group id=\"page_").append(pageIndex).append("\" visible=\"")
                    .append(pageIndex == 0).append("\">\n");
            if (page.extension() != null) {
                result.append(page.extension().sceneXml(theme));
                result.append("    </Group>\n");
                continue;
            }
            for (RokuImage image : page.images()) result.append("      <Poster uri=\"")
                    .append(xml(imageUris.get(image))).append("\" translation=\"[")
                    .append(image.x()).append(',').append(image.y()).append("]\" width=\"")
                    .append(image.width()).append("\" height=\"").append(image.height()).append("\" />\n");
            if (page.slideshow() != null) {
                int slideIndex = 0;
                for (Path source : page.slideshow().sources()) {
                    RokuImage slide = new RokuImage(source, page.slideshow().x(), page.slideshow().y(),
                            page.slideshow().width(), page.slideshow().height());
                    result.append("      <Poster id=\"slide_").append(pageIndex).append('_').append(slideIndex)
                            .append("\" uri=\"").append(xml(imageUris.get(slide))).append("\" translation=\"[")
                            .append(slide.x()).append(',').append(slide.y()).append("]\" width=\"")
                            .append(slide.width()).append("\" height=\"").append(slide.height())
                            .append("\" visible=\"").append(slideIndex == 0).append("\" />\n");
                    slideIndex++;
                }
                result.append("      <Timer id=\"slideshow_timer_").append(pageIndex)
                        .append("\" repeat=\"true\" duration=\"")
                        .append(page.slideshow().intervalSeconds()).append("\" />\n");
            }
            for (RokuText text : page.texts()) result.append("      <Label text=\"")
                    .append(xml(text.value())).append("\" translation=\"[")
                    .append(text.x()).append(',').append(text.y()).append("]\" width=\"")
                    .append(text.width()).append("\" height=\"").append(text.height())
                    .append("\" color=\"").append(theme.text()).append("\" />\n");
            int headingY = page.slideshow() == null ? 230 : 55;
            int messageY = page.slideshow() == null ? 370 : 125;
            result.append("      <Label text=\"").append(xml(page.heading()))
                    .append("\" translation=\"[180, ").append(headingY).append("]\" width=\"1560\" height=\"100\" horizAlign=\"center\" color=\"").append(theme.heading()).append("\" />\n")
                    .append("      <Label text=\"").append(xml(page.message()))
                    .append("\" translation=\"[180, ").append(messageY).append("]\" width=\"1560\" height=\"100\" horizAlign=\"center\" color=\"").append(theme.text()).append("\" />\n");
            if (page.counter() != null) result.append("      <Label id=\"counter_").append(pageIndex)
                    .append("\" text=\"").append(xml(page.counter().label())).append(": ")
                    .append(page.counter().initialValue())
                    .append("\" translation=\"[180, 520]\" width=\"1560\" height=\"100\" horizAlign=\"center\" color=\"").append(theme.text()).append("\" />\n");
            int top = page.slideshow() != null ? 925 : page.counter() == null ? 570 : 650;
            for (int buttonIndex = 0; buttonIndex < page.buttons().size(); buttonIndex++) {
                result.append("      <Rectangle id=\"button_bg_").append(pageIndex).append('_').append(buttonIndex)
                        .append("\" translation=\"[610, ").append(top + buttonIndex * 85)
                        .append("]\" width=\"700\" height=\"66\" color=\"").append(theme.button()).append("\" />\n")
                        .append("      <Label text=\"").append(xml(page.buttons().get(buttonIndex).label()))
                        .append("\" translation=\"[620, ").append(top + buttonIndex * 85 + 10)
                        .append("]\" width=\"680\" height=\"50\" horizAlign=\"center\" color=\"").append(theme.text()).append("\" />\n");
            }
            result.append("    </Group>\n");
        }
        return result.append("  </children>\n</component>\n").toString();
    }

    private static String sceneScript(List<RokuScene> pages, Map<String, Integer> routes, Theme theme, int extensionPage) {
        StringBuilder result = new StringBuilder("""
                sub init()
                    m.pages = []
                    m.buttonRects = []
                    m.targets = []
                    m.counterLabels = []
                    m.counterNames = []
                    m.counterValues = []
                    m.slideTimers = []
                    m.slides = []
                    m.slideIndexes = []
                    m.pageIndex = -1
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
            if (page.slideshow() == null) {
                result.append("    m.slideTimers.Push(invalid)\n    m.slides.Push([])\n    m.slideIndexes.Push(0)\n");
            } else {
                result.append("    timer = m.top.findNode(\"slideshow_timer_").append(pageIndex)
                        .append("\")\n    timer.ObserveField(\"fire\", \"onSlideshowTick\")\n")
                        .append("    m.slideTimers.Push(timer)\n    m.slides.Push([");
                for (int slideIndex = 0; slideIndex < page.slideshow().sources().size(); slideIndex++) {
                    if (slideIndex > 0) result.append(", ");
                    result.append("m.top.findNode(\"slide_").append(pageIndex).append('_').append(slideIndex).append("\")");
                }
                result.append("])\n    m.slideIndexes.Push(0)\n");
            }
        }
        RokuExtension extension = extensionPage < 0 ? null : pages.get(extensionPage).extension();
        if (extension != null) result.append("    ").append(extension.initialize(extensionPage, theme)).append('\n');
        String script = result.append("""
                    showPage(0)
                end sub

                sub showPage(index as Integer)
                    __EXTENSION_LEAVE__
                    if m.pageIndex >= 0
                        if m.slideTimers[m.pageIndex] <> invalid then m.slideTimers[m.pageIndex].control = "stop"
                    end if
                    for i = 0 to m.pages.Count() - 1
                        m.pages[i].visible = (i = index)
                    end for
                    m.pageIndex = index
                    m.selection = 0
                    updateFocus()
                    if m.slideTimers[index] <> invalid then m.slideTimers[index].control = "start"
                    __EXTENSION_ENTER__
                end sub

                sub onSlideshowTick()
                    index = m.pageIndex
                    if index < 0 then return
                    if m.slides[index].Count() = 0 then return
                    slides = m.slides[index]
                    slides[m.slideIndexes[index]].visible = false
                    m.slideIndexes[index] = (m.slideIndexes[index] + 1) mod slides.Count()
                    slides[m.slideIndexes[index]].visible = true
                end sub

                sub updateFocus()
                    buttons = m.buttonRects[m.pageIndex]
                    if buttons.Count() = 0 then return
                    for i = 0 to buttons.Count() - 1
                        if i = m.selection then buttons[i].color = "%s" else buttons[i].color = "%s"
                    end for
                end sub

                function onKeyEvent(key as String, press as Boolean) as Boolean
                    if not press then return false
                    __EXTENSION_KEY__
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
                """.formatted(theme.focusedButton(), theme.button())).toString();
        if (extension == null) return script.replace("__EXTENSION_LEAVE__", "")
                .replace("__EXTENSION_ENTER__", "").replace("__EXTENSION_KEY__", "");
        return script.replace("__EXTENSION_LEAVE__", "if m.pageIndex = " + extensionPage + " then " + extension.leave())
                .replace("__EXTENSION_ENTER__", "if index = " + extensionPage + " then " + extension.enter())
                .replace("__EXTENSION_KEY__", """
                        if m.pageIndex = %d
                            if key = "back"
                                if m.history.Count() > 0
                                    showPage(m.history.Pop())
                                    return true
                                end if
                                return false
                            end if
                            return %s
                        end if
                        """.formatted(extensionPage, extension.handleKey()));
    }

    private static String brs(String value) {
        return value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
    }
}
