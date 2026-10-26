package de.meinbisschen.haushalt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private static final int TAB_TODAY = 0, TAB_TASKS = 1, TAB_REMINDERS = 2;
    private static final int REQ_NOTIF = 7;

    private int tab = TAB_TODAY;
    private Store st;
    private LinearLayout content;
    private ScrollView scroll;
    private final TextView[] tabViews = new TextView[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        st = Store.get(this);
        if (savedInstanceState != null) tab = savedInstanceState.getInt("tab", TAB_TODAY);

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = Ui.column(this);
        int p = Ui.dp(this, 18);
        content.setPadding(p, Ui.dp(this, 20), p, Ui.dp(this, 32));
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Untere Navigation
        LinearLayout nav = Ui.row(this);
        nav.setBackgroundColor(Ui.SURFACE);
        nav.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        String[] labels = {"🌿\nHeute", "📋\nAufgaben", "🔔\nErinnerungen"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = Ui.text(this, labels[i], 13, Ui.MUTED, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            t.setOnClickListener(v -> {
                tab = idx;
                render();
                scroll.scrollTo(0, 0);
            });
            tabViews[i] = t;
            nav.addView(t, Ui.weight(this, 1, 4));
        }
        root.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        Notifier.ensureChannel(this);
        askNotificationPermission();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Scheduler.rescheduleAll(this);
        render();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        render();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
        }
    }

    /** Nach jeder Änderung: speichern, Erinnerungen neu planen, neu zeichnen. */
    private void changed() {
        st.save();
        Scheduler.rescheduleAll(this);
        render();
    }

    private void render() {
        content.removeAllViews();
        for (int i = 0; i < 3; i++) {
            boolean sel = i == tab;
            tabViews[i].setTextColor(sel ? Ui.SAGE_DARK : Ui.MUTED);
            tabViews[i].setBackground(sel ? Ui.rounded(Ui.SAGE_LIGHT, Ui.dp(this, 16), 0, 0) : null);
        }
        if (tab == TAB_TODAY) renderToday();
        else if (tab == TAB_TASKS) renderTasks();
        else renderReminders();
    }

    private void openEditor(long taskId) {
        Intent i = new Intent(this, TaskEditActivity.class);
        i.putExtra("id", taskId);
        startActivity(i);
    }

    // =====================================================================================
    // HEUTE
    // =====================================================================================

    private void renderToday() {
        Calendar now = Calendar.getInstance();
        String key = Dates.key(now);
        int idx = Dates.idx(now);
        String in7 = Dates.keyPlusDays(7);

        List<Task> today = new ArrayList<>();
        List<Task> flexible = new ArrayList<>();
        List<Task> upcoming = new ArrayList<>();
        int resting = 0;

        for (Task t : st.sortedTasks()) {
            boolean energyOk = t.energy <= st.energy;
            if (t.type == Task.REPEAT) {
                if (t.isOnDay(key, idx)) {
                    if (energyOk || t.isDoneOn(key)) today.add(t);
                    else resting++;
                }
            } else {
                if (t.done) {
                    if (key.equals(t.doneDate)) today.add(t);
                    continue;
                }
                if (t.date != null && t.date.equals(key)) {
                    if (energyOk) today.add(t);
                    else resting++;
                } else if (t.date == null || t.date.compareTo(key) < 0) {
                    if (energyOk) flexible.add(t);
                    else resting++;
                } else if (t.date.compareTo(in7) <= 0) {
                    upcoming.add(t);
                }
            }
        }

        int done = 0;
        for (Task t : today) if (t.isDoneOn(key)) done++;

        TextView date = Ui.text(this, Dates.longToday(), 14, Ui.MUTED, false);
        content.addView(date);
        content.addView(Ui.title(this, "Mein bisschen Haushalt"));
        TextView hello = Ui.text(this, Messages.todayHeader(done, today.size()), 16, Ui.TEXT, false);
        hello.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 14));
        content.addView(hello);

        // Energie-Auswahl
        LinearLayout energyCard = Ui.card(this);
        energyCard.addView(Ui.text(this, "Wie viel Energie hast du heute?", 16, Ui.TEXT, true));
        LinearLayout chips = Ui.row(this);
        chips.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
        String[] labels = {"🌙 Wenig", "🌿 Normal", "☀️ Viel"};
        for (int i = 0; i < 3; i++) {
            final int level = i + 1;
            chips.addView(Ui.chip(this, labels[i], st.energy == level, v -> {
                st.energy = level;
                changed();
            }), Ui.weight(this, 1, 3));
        }
        energyCard.addView(chips);
        String[] desc = {
                "Nur das Wichtigste. Alles andere darf warten.",
                "Deine üblichen Aufgaben – ganz in deinem Tempo.",
                "Wenn du Lust hast: zusätzliche Aufgaben und kleine Projekte."
        };
        energyCard.addView(Ui.text(this, desc[st.energy - 1], 14, Ui.MUTED, false));
        content.addView(energyCard);

        // Heute
        content.addView(Ui.section(this, "Heute"));
        if (today.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Für heute steht nichts an. Genieß den Tag. 🌿", 15, Ui.TEXT, false));
            content.addView(c);
        } else {
            for (Task t : today) content.addView(taskRow(t, key, null));
        }
        if (resting > 0) {
            String r = resting == 1
                    ? "1 Aufgabe ruht heute – das ist völlig in Ordnung."
                    : resting + " Aufgaben ruhen heute – das ist völlig in Ordnung.";
            TextView rest = Ui.text(this, r, 13, Ui.MUTED, false);
            rest.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), 0, Ui.dp(this, 2));
            content.addView(rest);
        }

        // Ohne festen Tag
        if (!flexible.isEmpty()) {
            content.addView(Ui.section(this, "Wenn du magst – ohne festen Tag"));
            for (Task t : flexible) {
                String info = null;
                if (t.date != null) info = "war für " + Dates.pretty(t.date) + " gedacht – ganz ohne Eile";
                content.addView(taskRow(t, key, info));
            }
        }

        // Demnächst
        if (!upcoming.isEmpty()) {
            Collections.sort(upcoming, (a, b) -> a.date.compareTo(b.date));
            content.addView(Ui.section(this, "Demnächst"));
            for (Task t : upcoming) {
                LinearLayout c = Ui.card(this);
                c.addView(Ui.text(this, t.title, 15, Ui.TEXT, false));
                c.addView(Ui.text(this, Dates.pretty(t.date), 13, Ui.MUTED, false));
                c.setOnClickListener(v -> openEditor(t.id));
                content.addView(c);
            }
        }

        TextView add = Ui.button(this, "+ Aufgabe hinzufügen", true, v -> openEditor(-1));
        add.setLayoutParams(Ui.full(this, 18));
        content.addView(add);
    }

    /** Eine Aufgabe mit Abhak-Kreis und „⋯“-Menü. */
    private LinearLayout taskRow(Task t, String key, String extraInfo) {
        boolean done = t.isDoneOn(key);
        LinearLayout card = Ui.card(this);
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12));
        if (done) card.setBackground(Ui.rounded(Ui.SAGE_LIGHT, Ui.dp(this, 20), Ui.LINE, Ui.dp(this, 1)));

        LinearLayout row = Ui.row(this);

        TextView check = Ui.text(this, done ? "✓" : "", 18, 0xFFFFFFFF, true);
        check.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(done ? Ui.SAGE : Ui.SURFACE);
        circle.setStroke(Ui.dp(this, 2), Ui.SAGE);
        check.setBackground(circle);
        int size = Ui.dp(this, 32);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(size, size);
        clp.rightMargin = Ui.dp(this, 14);
        row.addView(check, clp);

        LinearLayout texts = Ui.column(this);
        TextView title = Ui.text(this, t.title, 16, done ? Ui.MUTED : Ui.TEXT, !done);
        if (done) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        texts.addView(title);

        List<String> parts = new ArrayList<>();
        if (t.notify && st.remindersOn) {
            for (Reminder r : st.remindersFor(t.id)) {
                if (r.enabled) parts.add("🔔 " + Dates.time(r.hour, r.minute));
            }
        }
        if (t.energy == Task.ENERGY_HIGH) parts.add("☀️ Bonus");
        if (extraInfo != null) parts.add(extraInfo);
        else if (t.note != null && !t.note.isEmpty()) parts.add(t.note);
        if (!parts.isEmpty()) texts.addView(Ui.text(this, String.join("  ·  ", parts), 13, Ui.MUTED, false));
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView more = Ui.smallButton(this, "⋯", Ui.MUTED, v -> taskMenu(t, key));
        more.setTextSize(22);
        more.setContentDescription("Weitere Aktionen");
        row.addView(more);

        card.addView(row);
        card.setOnClickListener(v -> {
            boolean nowDone = !t.isDoneOn(key);
            t.setDone(key, nowDone);
            if (nowDone) {
                for (Reminder r : st.remindersFor(t.id)) Notifier.cancel(this, r.id);
            }
            changed();
            if (nowDone) Toast.makeText(this, pickDoneMessage(), Toast.LENGTH_SHORT).show();
        });
        return card;
    }

    private String pickDoneMessage() {
        String[] m = {"Schön gemacht. 🌿", "Geschafft – das zählt.", "Ein Häkchen mehr. ✨",
                "Gut gemacht. Du darfst stolz sein.", "Erledigt. Gönn dir eine kleine Pause."};
        return m[(int) (Math.random() * m.length)];
    }

    private void taskMenu(Task t, String key) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        labels.add("Bearbeiten");
        actions.add(() -> openEditor(t.id));

        labels.add("Auf morgen verschieben");
        actions.add(() -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            moveTask(t, key, c);
        });

        labels.add("Auf einen anderen Tag verschieben…");
        actions.add(() -> pickDate(c -> moveTask(t, key, c)));

        if (t.type == Task.REPEAT && t.isOnDay(key, Dates.idx(Calendar.getInstance()))) {
            labels.add("Heute auslassen");
            actions.add(() -> {
                t.skipDates.add(key);
                t.extraDates.remove(key);
                changed();
                Toast.makeText(this, "Heute ausgelassen – völlig in Ordnung.", Toast.LENGTH_SHORT).show();
            });
        }

        labels.add("Löschen");
        actions.add(() -> confirmDelete(t));

        new AlertDialog.Builder(this)
                .setTitle(t.title)
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void moveTask(Task t, String fromKey, Calendar target) {
        String tk = Dates.key(target);
        if (t.type == Task.ONCE) {
            t.date = tk;
            t.done = false;
            t.doneDate = null;
        } else {
            if (fromKey != null && !fromKey.equals(tk)) {
                t.skipDates.add(fromKey);
                t.extraDates.remove(fromKey);
                t.doneDates.remove(fromKey);
            }
            t.extraDates.add(tk);
            t.skipDates.remove(tk);
        }
        changed();
        Toast.makeText(this, "Verschoben auf " + Dates.pretty(tk), Toast.LENGTH_SHORT).show();
    }

    interface DateCallback {
        void onDate(Calendar c);
    }

    private void pickDate(DateCallback cb) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d, 12, 0, 0);
            cb.onDate(sel);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private void confirmDelete(Task t) {
        new AlertDialog.Builder(this)
                .setTitle("„" + t.title + "“ löschen?")
                .setMessage("Die zugehörigen Erinnerungen werden ebenfalls entfernt.")
                .setPositiveButton("Löschen", (d, w) -> {
                    for (Reminder r : st.remindersFor(t.id)) Notifier.cancel(this, r.id);
                    st.deleteTask(t.id);
                    changed();
                })
                .setNegativeButton("Behalten", null)
                .show();
    }

    // =====================================================================================
    // AUFGABEN
    // =====================================================================================

    private void renderTasks() {
        content.addView(Ui.title(this, "Deine Aufgaben"));
        TextView sub = Ui.text(this, "Tippe auf eine Aufgabe, um sie zu bearbeiten, zu verschieben oder zu löschen.",
                14, Ui.MUTED, false);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        content.addView(sub);

        TextView add = Ui.button(this, "+ Neue Aufgabe", true, v -> openEditor(-1));
        add.setLayoutParams(Ui.full(this, 0));
        content.addView(add);

        List<Task> daily = new ArrayList<>(), weekly = new ArrayList<>(), dated = new ArrayList<>(),
                undated = new ArrayList<>(), doneOnce = new ArrayList<>();
        for (Task t : st.sortedTasks()) {
            if (t.type == Task.REPEAT) {
                if (t.isDaily()) daily.add(t);
                else weekly.add(t);
            } else if (t.done) doneOnce.add(t);
            else if (t.date != null) dated.add(t);
            else undated.add(t);
        }
        Collections.sort(dated, (a, b) -> a.date.compareTo(b.date));

        addTaskGroup("Täglich", daily);
        addTaskGroup("An bestimmten Wochentagen", weekly);
        addTaskGroup("Einmalig mit Termin", dated);
        addTaskGroup("Einmalig – Termin legst du selbst fest", undated);
        addTaskGroup("Erledigt (einmalig)", doneOnce);

        if (st.tasks.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Noch keine Aufgaben. Leg los, wann immer du magst. 🌿", 15, Ui.TEXT, false));
            content.addView(c);
        }
    }

    private void addTaskGroup(String title, List<Task> list) {
        if (list.isEmpty()) return;
        content.addView(Ui.section(this, title));
        for (Task t : list) {
            LinearLayout c = Ui.card(this);
            c.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
            c.addView(Ui.text(this, t.title, 16, t.done ? Ui.MUTED : Ui.TEXT, true));

            List<String> parts = new ArrayList<>();
            if (t.type == Task.REPEAT) parts.add(capitalize(Dates.daysText(t.days)));
            else if (t.date != null) parts.add(capitalize(Dates.pretty(t.date)));
            else if (t.done && t.doneDate != null) parts.add("erledigt " + Dates.pretty(t.doneDate));
            else parts.add("ohne Datum");
            parts.add(energyLabel(t.energy));
            List<Reminder> rs = st.remindersFor(t.id);
            if (!t.notify) parts.add("🔕 keine Benachrichtigung");
            else if (!rs.isEmpty()) {
                List<String> times = new ArrayList<>();
                for (Reminder r : rs) if (r.enabled) times.add(Dates.time(r.hour, r.minute));
                if (!times.isEmpty()) parts.add("🔔 " + String.join(", ", times));
            }
            c.addView(Ui.text(this, String.join("  ·  ", parts), 13, Ui.MUTED, false));
            c.setOnClickListener(v -> openEditor(t.id));
            content.addView(c);
        }
    }

    static String energyLabel(int e) {
        if (e == Task.ENERGY_LOW) return "🌙 wichtig";
        if (e == Task.ENERGY_HIGH) return "☀️ Bonus";
        return "🌿 normal";
    }

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(java.util.Locale.GERMAN) + s.substring(1);
    }

    // =====================================================================================
    // ERINNERUNGEN (Einstellungsseite)
    // =====================================================================================

    private void renderReminders() {
        content.addView(Ui.title(this, "Erinnerungen"));
        TextView sub = Ui.text(this, "Alle Zeiten kannst du jederzeit ändern. Änderungen gelten sofort.",
                14, Ui.MUTED, false);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        content.addView(sub);

        // Hauptschalter
        LinearLayout master = Ui.card(this);
        LinearLayout mrow = Ui.row(this);
        LinearLayout mtexts = Ui.column(this);
        mtexts.addView(Ui.text(this, "Erinnerungen aktiv", 16, Ui.TEXT, true));
        String next = st.remindersOn ? Scheduler.nextInfo(this) : null;
        mtexts.addView(Ui.text(this,
                !st.remindersOn ? "Alle Erinnerungen sind pausiert."
                        : next != null ? "Nächste: " + next : "Gerade ist keine Erinnerung geplant.",
                13, Ui.MUTED, false));
        mrow.addView(mtexts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch msw = new Switch(this);
        msw.setChecked(st.remindersOn);
        msw.setOnCheckedChangeListener((b, c) -> {
            st.remindersOn = c;
            changed();
        });
        mrow.addView(msw);
        master.addView(mrow);
        content.addView(master);

        // Später-Dauer
        LinearLayout snooze = Ui.card(this);
        snooze.addView(Ui.text(this, "„Später“ in der Benachrichtigung erinnert nach …", 15, Ui.TEXT, true));
        LinearLayout srow = Ui.row(this);
        srow.setPadding(0, Ui.dp(this, 10), 0, 0);
        int[] mins = {10, 15, 30, 60};
        for (int m : mins) {
            srow.addView(Ui.chip(this, m + " Min.", st.snoozeMinutes == m, v -> {
                st.snoozeMinutes = m;
                changed();
            }), Ui.weight(this, 1, 3));
        }
        snooze.addView(srow);
        content.addView(snooze);

        // Aufgaben mit ihren Erinnerungen
        content.addView(Ui.section(this, "Erinnerungen je Aufgabe"));
        Runnable onChange = this::changed;
        List<Task> withoutReminders = new ArrayList<>();
        for (Task t : st.sortedTasks()) {
            List<Reminder> rs = st.remindersFor(t.id);
            if (rs.isEmpty()) {
                withoutReminders.add(t);
                continue;
            }
            content.addView(reminderTaskCard(t, rs, onChange));
        }
        if (!withoutReminders.isEmpty()) {
            content.addView(Ui.section(this, "Aufgaben ohne Erinnerung"));
            for (Task t : withoutReminders) {
                LinearLayout c = Ui.card(this);
                c.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
                LinearLayout r = Ui.row(this);
                r.addView(Ui.text(this, t.title, 15, Ui.TEXT, false),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                TextView add = ReminderViews.addButton(this, st, t.id, () -> {
                    t.notify = true;
                    changed();
                });
                add.setText("+ Erinnerung");
                r.addView(add);
                c.addView(r);
                content.addView(c);
            }
        }

        renderReliability();
    }

    private LinearLayout reminderTaskCard(Task t, List<Reminder> rs, Runnable onChange) {
        LinearLayout c = Ui.card(this);
        LinearLayout head = Ui.row(this);
        LinearLayout ht = Ui.column(this);
        ht.addView(Ui.text(this, t.title, 16, Ui.TEXT, true));
        ht.addView(Ui.text(this, t.notify ? "Benachrichtigung an" : "Keine Benachrichtigung für diese Aufgabe",
                13, Ui.MUTED, false));
        head.addView(ht, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(t.notify);
        sw.setOnCheckedChangeListener((b, checked) -> {
            t.notify = checked;
            changed();
        });
        head.addView(sw);
        c.addView(head);
        if (t.notify) {
            for (Reminder r : rs) c.addView(ReminderViews.build(this, st, t, r, onChange));
            c.addView(ReminderViews.addButton(this, st, t.id, onChange));
        }
        return c;
    }

    private void renderReliability() {
        content.addView(Ui.section(this, "Damit Erinnerungen zuverlässig ankommen"));
        LinearLayout c = Ui.card(this);

        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean notifOk = nm == null || nm.areNotificationsEnabled();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean batteryOk = pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
        boolean exactOk = Scheduler.canExact(this);

        c.addView(statusLine(notifOk, "Benachrichtigungen erlaubt", "Benachrichtigungen sind ausgeschaltet"));
        if (!notifOk) {
            c.addView(Ui.smallButton(this, "Benachrichtigungen erlauben", Ui.SAGE_DARK, v -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, REQ_NOTIF);
                } else {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    safeStart(i);
                }
            }));
        }
        c.addView(statusLine(exactOk, "Pünktliche Wecker erlaubt", "Pünktliche Wecker nicht erlaubt"));
        if (!exactOk && Build.VERSION.SDK_INT >= 31) {
            c.addView(Ui.smallButton(this, "Pünktliche Wecker erlauben", Ui.SAGE_DARK, v ->
                    safeStart(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())))));
        }
        c.addView(statusLine(batteryOk, "Akku-Optimierung aus (empfohlen)", "Akku-Optimierung ist an"));
        if (!batteryOk) {
            c.addView(Ui.smallButton(this, "Akku-Optimierung ausschalten", Ui.SAGE_DARK, v -> {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName()));
                if (!safeStart(i)) safeStart(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }));
        }
        TextView hint = Ui.text(this, "Manche Handys (z. B. Xiaomi, Huawei, Samsung) beenden Apps im Hintergrund "
                + "sehr streng. Falls Erinnerungen ausbleiben, erlaube in den Handy-Einstellungen „Autostart“ "
                + "bzw. „Nicht optimieren“ für diese App.", 13, Ui.MUTED, false);
        hint.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        c.addView(hint);
        c.addView(Ui.smallButton(this, "Test-Erinnerung senden", Ui.SAGE_DARK, v -> {
            Notifier.showTest(this);
            Toast.makeText(this, "Test-Erinnerung gesendet", Toast.LENGTH_SHORT).show();
        }));
        content.addView(c);
    }

    private TextView statusLine(boolean ok, String okText, String badText) {
        TextView t = Ui.text(this, (ok ? "✓  " : "○  ") + (ok ? okText : badText), 14,
                ok ? Ui.SAGE_DARK : Ui.CLAY, false);
        t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 2));
        return t;
    }

    private boolean safeStart(Intent i) {
        try {
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
