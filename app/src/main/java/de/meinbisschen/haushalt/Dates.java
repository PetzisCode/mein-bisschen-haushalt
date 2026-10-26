package de.meinbisschen.haushalt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Kleine Datums-Helfer. Tage werden als "yyyy-MM-dd" gespeichert, Wochentage als Index (0 = Montag). */
public final class Dates {

    public static final String[] SHORT = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    public static final String[] LONG = {"Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"};
    private static final String[] MONTHS = {"Januar", "Februar", "März", "April", "Mai", "Juni", "Juli",
            "August", "September", "Oktober", "November", "Dezember"};

    private Dates() { }

    public static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String todayKey() {
        return key(Calendar.getInstance());
    }

    public static String keyPlusDays(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, n);
        return key(c);
    }

    public static Calendar parse(String k) {
        Calendar c = Calendar.getInstance();
        String[] p = k.split("-");
        c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 12, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** 0 = Montag … 6 = Sonntag. */
    public static int idx(Calendar c) {
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
    }

    /** "heute", "morgen" oder z. B. "Mi, 28.10." */
    public static String pretty(String k) {
        if (k.equals(todayKey())) return "heute";
        if (k.equals(keyPlusDays(1))) return "morgen";
        if (k.equals(keyPlusDays(-1))) return "gestern";
        Calendar c = parse(k);
        return SHORT[idx(c)] + ", " + c.get(Calendar.DAY_OF_MONTH) + "." + (c.get(Calendar.MONTH) + 1) + ".";
    }

    /** z. B. "Montag, 26. Oktober" */
    public static String longToday() {
        Calendar c = Calendar.getInstance();
        return LONG[idx(c)] + ", " + c.get(Calendar.DAY_OF_MONTH) + ". " + MONTHS[c.get(Calendar.MONTH)];
    }

    public static String time(int h, int m) {
        return String.format(Locale.GERMAN, "%02d:%02d", h, m);
    }

    public static String daysText(int mask) {
        mask &= 0x7F;
        if (mask == 0x7F) return "täglich";
        if (mask == 0) return "an keinem Tag";
        if (mask == 0x1F) return "Mo–Fr";
        if (mask == 0x60) return "Sa & So";
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < 7; i++) if (((mask >> i) & 1) == 1) parts.add(SHORT[i]);
        return String.join(", ", parts);
    }
}
