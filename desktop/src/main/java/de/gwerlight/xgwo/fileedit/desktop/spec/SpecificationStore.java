package de.gwerlight.xgwo.fileedit.desktop.spec;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Hält die eingerichteten Spezifikationen in einer lesbaren Datei im Benutzerverzeichnis (Properties, UTF-8).
 * Gespeichert werden nur die Pfade, nicht die Dateien.
 */
public final class SpecificationStore {

    private static final String COUNT = "count";

    private final Path file;

    public SpecificationStore(Path file) {
        this.file = file;
    }

    /** Standardablage: {@code %APPDATA%\XGWO-FileEditor} unter Windows, sonst {@code ~/.config/xgwo-fileedit}. */
    public static SpecificationStore standard() {
        String appData = System.getenv("APPDATA");
        Path dir = appData != null && !appData.isBlank()
                ? Path.of(appData, "XGWO-FileEditor")
                : Path.of(System.getProperty("user.home"), ".config", "xgwo-fileedit");
        return new SpecificationStore(dir.resolve("specifications.properties"));
    }

    public Path file() {
        return file;
    }

    /** @return die gespeicherten Spezifikationen; leer, wenn noch nichts eingerichtet wurde */
    public List<Specification> load() {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Spezifikationen konnten nicht gelesen werden: " + file, e);
        }
        List<Specification> specs = new ArrayList<>();
        int count = parseCount(props.getProperty(COUNT));
        for (int i = 1; i <= count; i++) {
            String xsd = props.getProperty(i + ".xsd");
            String xsl = props.getProperty(i + ".xsl");
            if (xsd == null || xsl == null) {
                continue; // unvollständiger Eintrag, z. B. von Hand bearbeitet
            }
            specs.add(new Specification(props.getProperty(i + ".name", ""), props.getProperty(i + ".version", ""),
                    Path.of(xsd), Path.of(xsl)));
        }
        return List.copyOf(specs);
    }

    public void save(List<Specification> specs) {
        Properties props = new Properties();
        props.setProperty(COUNT, Integer.toString(specs.size()));
        for (int i = 0; i < specs.size(); i++) {
            Specification s = specs.get(i);
            int n = i + 1;
            props.setProperty(n + ".name", s.name());
            props.setProperty(n + ".version", s.version() == null ? "" : s.version());
            props.setProperty(n + ".xsd", s.xsd().toString());
            props.setProperty(n + ".xsl", s.xsl().toString());
        }
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(writer, "XGWO-FileEditor: eingerichtete Spezifikationen");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Spezifikationen konnten nicht gespeichert werden: " + file, e);
        }
    }

    private static int parseCount(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
