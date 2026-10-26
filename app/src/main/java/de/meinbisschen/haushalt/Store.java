package de.meinbisschen.haushalt;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Speichert alle Aufgaben, Erinnerungen und Einstellungen dauerhaft in einer JSON-Datei. */
public class Store {

    private static Store instance;

    private final File file;
    public final List<Task> tasks = new ArrayList<>();
    public final List<Reminder> reminders = new ArrayList<>();
    public long nextTaskId = 1;
    public int nextReminderId = 1;
    /** Aktueller Energie-Modus: 1 = wenig, 2 = normal, 3 = viel. */
    public int energy = 2;
    /** Hauptschalter für alle Erinnerungen. */
    public boolean remindersOn = true;
    /** „Später erinnern“ nach so vielen Minuten. */
    public int snoozeMinutes = 30;

    public static synchronized Store get(Context ctx) {
        if (instance == null) instance = new Store(ctx.getApplicationContext());
        return instance;
    }

    private Store(Context ctx) {
        file = new File(ctx.getFilesDir(), "haushalt.json");
        if (file.exists()) {
            try {
                load();
            } catch (Exception e) {
                // Datei beschädigt: sichern statt überschreiben, dann neu starten.
                File broken = new File(ctx.getFilesDir(), "haushalt-defekt-" + System.currentTimeMillis() + ".json");
                //noinspection ResultOfMethodCallIgnored
                file.renameTo(broken);
                seed();
                save();
            }
        } else {
            seed();
            save();
        }
    }

    // ------------------------------------------------------------------ Laden & Speichern

    private void load() throws Exception {
        String s = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        JSONObject o = new JSONObject(s);
        tasks.clear();
        reminders.clear();
        nextTaskId = o.optLong("nextTaskId", 1);
        nextReminderId = o.optInt("nextReminderId", 1);
        energy = o.optInt("energy", 2);
        remindersOn = o.optBoolean("remindersOn", true);
        snoozeMinutes = o.optInt("snoozeMinutes", 30);
        JSONArray ta = o.optJSONArray("tasks");
        if (ta != null) {
            for (int i = 0; i < ta.length(); i++) tasks.add(Task.fromJson(ta.getJSONObject(i)));
        }
        JSONArray ra = o.optJSONArray("reminders");
        if (ra != null) {
            for (int i = 0; i < ra.length(); i++) reminders.add(Reminder.fromJson(ra.getJSONObject(i)));
        }
        // Erinnerungen ohne Aufgabe aufräumen.
        reminders.removeIf(r -> findTask(r.taskId) == null);
    }

    public synchronized void save() {
        try {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -60);
            String oldest = Dates.key(c);
            String today = Dates.todayKey();
            for (Task t : tasks) t.prune(oldest);
            for (Reminder r : reminders) r.pausedDates.removeIf(k -> k.compareTo(today) < 0);

            JSONObject o = new JSONObject();
            o.put("version", 1);
            o.put("nextTaskId", nextTaskId);
            o.put("nextReminderId", nextReminderId);
            o.put("energy", energy);
            o.put("remindersOn", remindersOn);
            o.put("snoozeMinutes", snoozeMinutes);
            JSONArray ta = new JSONArray();
            for (Task t : tasks) ta.put(t.toJson());
            o.put("tasks", ta);
            JSONArray ra = new JSONArray();
            for (Reminder r : reminders) ra.put(r.toJson());
            o.put("reminders", ra);

            File tmp = new File(file.getParentFile(), "haushalt.json.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(o.toString(1).getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (!tmp.renameTo(file)) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(o.toString(1).getBytes(StandardCharsets.UTF_8));
                }
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        } catch (Exception ignored) {
            // Speichern ist best effort – die App läuft weiter.
        }
    }

    // ------------------------------------------------------------------ Startvorschläge

    private void seed() {
        tasks.clear();
        reminders.clear();
        nextTaskId = 1;
        nextReminderId = 1;

        Task cats = addSeed("Katzenklos reinigen", Task.REPEAT, 0x7F, Task.ENERGY_LOW, "");
        addReminder(cats.id, 13, 0);

        Task evening = addSeed("Staubsaugen & abendliche Haushaltsrunde", Task.REPEAT, 0x7F, Task.ENERGY_NORMAL,
                "Katzenhaare dürfen auch mal liegen bleiben.");
        addReminder(evening.id, 19, 30);

        addSeed("Küche aufräumen", Task.REPEAT, 0x7F, Task.ENERGY_LOW, "");
        addSeed("Oberflächen abwischen", Task.REPEAT, (1 << 2) | (1 << 5), Task.ENERGY_NORMAL,
                "Gegen Staub und Katzenhaare");
        addSeed("Bad putzen", Task.REPEAT, 1 << 5, Task.ENERGY_NORMAL, "");
        addSeed("Sofa & Decken von Katzenhaaren befreien", Task.REPEAT, 1 << 6, Task.ENERGY_HIGH,
                "Fusselrolle oder Gummihandschuh");
        addSeed("Fenster putzen", Task.ONCE, 0x7F, Task.ENERGY_HIGH, "Termin selbst festlegen, wenn es passt");
        addSeed("Kühlschrank reinigen", Task.ONCE, 0x7F, Task.ENERGY_HIGH, "");
        addSeed("Schränke auswischen", Task.ONCE, 0x7F, Task.ENERGY_HIGH, "");
    }

    private Task addSeed(String title, int type, int days, int energy, String note) {
        Task t = new Task();
        t.id = nextTaskId++;
        t.title = title;
        t.type = type;
        t.days = days;
        t.energy = energy;
        t.note = note;
        tasks.add(t);
        return t;
    }

    // ------------------------------------------------------------------ Zugriff

    public Task findTask(long id) {
        for (Task t : tasks) if (t.id == id) return t;
        return null;
    }

    public Reminder findReminder(int id) {
        for (Reminder r : reminders) if (r.id == id) return r;
        return null;
    }

    public List<Reminder> remindersFor(long taskId) {
        List<Reminder> list = new ArrayList<>();
        for (Reminder r : reminders) if (r.taskId == taskId) list.add(r);
        Collections.sort(list, (a, b) -> (a.hour * 60 + a.minute) - (b.hour * 60 + b.minute));
        return list;
    }

    public Reminder addReminder(long taskId, int hour, int minute) {
        Reminder r = new Reminder();
        r.id = nextReminderId++;
        r.taskId = taskId;
        r.hour = hour;
        r.minute = minute;
        reminders.add(r);
        return r;
    }

    public void deleteReminder(int id) {
        reminders.removeIf(r -> r.id == id);
    }

    public void deleteTask(long id) {
        tasks.removeIf(t -> t.id == id);
        reminders.removeIf(r -> r.taskId == id);
    }

    public List<Task> sortedTasks() {
        List<Task> list = new ArrayList<>(tasks);
        Collections.sort(list, (a, b) -> {
            if (a.energy != b.energy) return a.energy - b.energy;
            return a.title.compareToIgnoreCase(b.title);
        });
        return list;
    }

    // ------------------------------------------------------------------ Erinnerungslogik

    /** Soll diese Erinnerung an diesem Tag auslösen? */
    public boolean reminderFiresOn(Reminder r, Task t, Calendar day) {
        if (!remindersOn || r == null || t == null || !r.enabled || !t.notify) return false;
        String key = Dates.key(day);
        if (r.pausedDates.contains(key)) return false;
        int idx = Dates.idx(day);
        boolean maskOk = ((r.days >> idx) & 1) == 1;
        if (t.type == Task.REPEAT) {
            if (t.skipDates.contains(key) || t.doneDates.contains(key)) return false;
            if (t.extraDates.contains(key)) return true;
            return maskOk && ((t.days >> idx) & 1) == 1;
        } else {
            if (t.done) return false;
            if (t.date != null) return t.date.equals(key);
            return maskOk;
        }
    }

    /** Nächster Auslösezeitpunkt oder -1, wenn es keinen gibt. */
    public long nextTrigger(Reminder r, long now) {
        Task t = findTask(r.taskId);
        if (t == null) return -1;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        for (int i = 0; i < 400; i++) {
            c.set(Calendar.HOUR_OF_DAY, r.hour);
            c.set(Calendar.MINUTE, r.minute);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            if (c.getTimeInMillis() > now + 1000 && reminderFiresOn(r, t, c)) return c.getTimeInMillis();
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
        return -1;
    }

    // ------------------------------------------------------------------ JSON-Helfer

    static JSONArray setToJson(Set<String> set) {
        JSONArray a = new JSONArray();
        List<String> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        for (String s : sorted) a.put(s);
        return a;
    }

    static Set<String> jsonToSet(JSONArray a) throws JSONException {
        Set<String> set = new HashSet<>();
        if (a == null) return set;
        for (int i = 0; i < a.length(); i++) set.add(a.getString(i));
        return set;
    }
}
