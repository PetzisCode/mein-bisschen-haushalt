package de.meinbisschen.haushalt;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Liebevolle, wechselnde Texte – niemals vorwurfsvoll. */
public final class Messages {

    private Messages() { }

    private static final String[] GENERAL = {
            "Ein kleiner Schritt reicht für heute.",
            "Du darfst es langsam angehen lassen.",
            "Was du heute schaffst, ist genug.",
            "Eine kleine Runde durchs Zuhause? Du musst nicht alles perfekt machen.",
            "Nur wenn es gerade passt – später ist auch okay.",
            "Fünf Minuten zählen genauso.",
            "Dein Zuhause muss nicht perfekt sein, nur ein bisschen gemütlich.",
            "Ganz in deinem Tempo.",
            "Auch halb erledigt macht es schon ein Stück leichter.",
            "Magst du kurz anfangen? Aufhören darfst du jederzeit.",
            "Du machst das gut – auch an langsamen Tagen.",
    };

    private static final String[] CATS = {
            "Zeit für die Katzenklos – danach hast du es geschafft.",
            "Deine Katzen sagen schon mal danke. 🐾",
            "Kurz die Katzenklos – und dann ist Pause.",
    };

    private static final String[] EVENING = {
            "Eine kleine Runde durchs Zuhause? Du musst nicht alles perfekt machen.",
            "Ein bisschen Katzenhaar weniger – mehr braucht es heute nicht.",
            "Die Abendrunde darf auch kurz sein.",
            "Ein paar Minuten, dann ist Feierabend. 🌙",
    };

    private static final Random RANDOM = new Random();

    public static String pick(Context ctx, String title, int hour) {
        String t = title == null ? "" : title.toLowerCase(Locale.GERMAN);
        List<String> pool = new ArrayList<>(Arrays.asList(GENERAL));
        if (t.contains("katze")) {
            // Passende Texte doppelt gewichten
            pool.addAll(Arrays.asList(CATS));
            pool.addAll(Arrays.asList(CATS));
        } else if (hour >= 17 && (t.contains("staub") || t.contains("runde") || t.contains("abend"))) {
            pool.addAll(Arrays.asList(EVENING));
            pool.addAll(Arrays.asList(EVENING));
        }
        SharedPreferences sp = ctx.getSharedPreferences("messages", Context.MODE_PRIVATE);
        String last = sp.getString("last", "");
        String msg = pool.get(RANDOM.nextInt(pool.size()));
        for (int i = 0; i < 5 && msg.equals(last); i++) msg = pool.get(RANDOM.nextInt(pool.size()));
        sp.edit().putString("last", msg).apply();
        return msg;
    }

    public static String any() {
        return GENERAL[RANDOM.nextInt(GENERAL.length)];
    }

    /** Begrüßung auf der Heute-Seite. */
    public static String todayHeader(int done, int total) {
        if (total == 0) return "Für heute ist nichts geplant. Genieß den Tag. 🌿";
        if (done == 0) return "Schön, dass du da bist. Fang an, wo es sich leicht anfühlt.";
        if (done >= total) return "Alles erledigt für heute – gönn dir eine Pause. 🌿";
        if (done == 1) return "Eine Sache ist schon geschafft. Das zählt.";
        return "Du hast schon " + done + " Dinge geschafft. Das zählt.";
    }
}
