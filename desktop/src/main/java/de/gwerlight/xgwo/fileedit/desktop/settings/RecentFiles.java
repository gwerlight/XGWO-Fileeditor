package de.gwerlight.xgwo.fileedit.desktop.settings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Merkt sich die zuletzt verwendeten Dateien (neueste zuerst) und hält sie über Neustarts in den Benutzereinstellungen. */
public final class RecentFiles {

    private static final String KEY = "recentFiles";
    private static final String SEPARATOR = "\n";

    /** Fester Pfad (der frühere Packagename), damit die gemerkten Dateien auch nach Umbenennungen von Packages erhalten bleiben. */
    private static final String PREFS_PATH = "de/gwerlight/xgwo/fileedit/desktop";

    private final Preferences prefs;
    private final int max;
    private final List<Path> files = new ArrayList<>();

    public RecentFiles(int max) {
        this(max, Preferences.userRoot().node(PREFS_PATH));
    }

    /** Mit eigener Ablage der Einstellungen, z. B. für Tests, die die echten Benutzereinstellungen nicht berühren dürfen. */
    public RecentFiles(int max, Preferences prefs) {
        this.prefs = prefs;
        this.max = max;
        String stored = prefs.get(KEY, "");
        if (!stored.isEmpty()) {
            for (String s : stored.split(SEPARATOR)) {
                files.add(Path.of(s));
            }
        }
    }

    public List<Path> list() {
        return List.copyOf(files);
    }

    /** Stellt die Datei an die erste Stelle; ältere Einträge über dem Limit fallen heraus. */
    public void add(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        files.remove(normalized);
        files.add(0, normalized);
        while (files.size() > max) {
            files.remove(files.size() - 1);
        }
        prefs.put(KEY, String.join(SEPARATOR, files.stream().map(Path::toString).toList()));
    }

    public void remove(Path file) {
        if (files.remove(file)) {
            prefs.put(KEY, String.join(SEPARATOR, files.stream().map(Path::toString).toList()));
        }
    }
}
