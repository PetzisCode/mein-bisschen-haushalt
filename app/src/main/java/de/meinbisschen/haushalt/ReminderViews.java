package de.meinbisschen.haushalt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** Baut die Bedienelemente für eine einzelne Erinnerung (Uhrzeit, Tage, an/aus, aussetzen, löschen). */
public final class ReminderViews {

    private ReminderViews() { }

    /**
     * @param changed wird nach jeder Änderung aufgerufen (speichern, neu planen, neu zeichnen).
     */
    public static LinearLayout build(Activity a, Store st, Task task, Reminder r, Runnable changed) {
        LinearLayout box = Ui.column(a);
        int p = Ui.dp(a, 12);
        box.setPadding(p, Ui.dp(a, 8), p, Ui.dp(a, 8));
        box.setBackground(Ui.rounded(r.enabled ? Ui.SAGE_LIGHT : Ui.SAND, Ui.dp(a, 16), 0, 0));
        box.setLayoutParams(Ui.full(a, 8));

        // Zeile 1: Uhrzeit + Schalter
        LinearLayout top = Ui.row(a);
        TextView time = Ui.text(a, "🔔 " + Dates.time(r.hour, r.minute), 26, r.enabled ? Ui.TEXT : Ui.MUTED, true);
        time.setPadding(0, Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 4));
        time.setOnClickListener(v -> new TimePickerDialog(a, (view, h, m) -> {
            r.hour = h;
            r.minute = m;
            changed.run();
            Toast.makeText(a, "Erinnerung auf " + Dates.time(h, m) + " Uhr gestellt", Toast.LENGTH_SHORT).show();
        }, r.hour, r.minute, true).show());
        top.addView(time);
        TextView edit = Ui.text(a, "ändern", 13, Ui.SAGE_DARK, false);
        edit.setOnClickListener(v -> time.performClick());
        top.addView(edit);
        top.addView(Ui.flex(a));
        Switch sw = new Switch(a);
        sw.setChecked(r.enabled);
        sw.setContentDescription("Erinnerung aktiv");
        sw.setOnCheckedChangeListener((b, checked) -> {
            r.enabled = checked;
            changed.run();
        });
        top.addView(sw);
        box.addView(top);

        // Zeile 2: Tage
        String daysLabel;
        if (task.type == Task.ONCE && task.date != null) {
            daysLabel = "Am geplanten Tag (" + Dates.pretty(task.date) + ")";
        } else {
            daysLabel = "Tage: " + Dates.daysText(r.days) + "  ✎";
        }
        TextView days = Ui.text(a, daysLabel, 14, Ui.TEXT, false);
        days.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 4));
        if (!(task.type == Task.ONCE && task.date != null)) {
            days.setOnClickListener(v -> chooseDays(a, r, changed));
        }
        box.addView(days);
        if (task.type == Task.REPEAT && (task.days & 0x7F) != 0x7F) {
            TextView hint = Ui.text(a, "Erinnert nur an Tagen, an denen die Aufgabe geplant ist ("
                    + Dates.daysText(task.days) + ").", 12, Ui.MUTED, false);
            box.addView(hint);
        }

        // Ausgesetzte Tage
        List<String> paused = new ArrayList<>(r.pausedDates);
        Collections.sort(paused);
        String today = Dates.todayKey();
        for (String k : paused) {
            if (k.compareTo(today) < 0) continue;
            TextView chip = Ui.text(a, "⏸ ausgesetzt: " + Dates.pretty(k) + "   ✕ fortsetzen", 13, Ui.SAGE_DARK, false);
            chip.setPadding(Ui.dp(a, 10), Ui.dp(a, 6), Ui.dp(a, 10), Ui.dp(a, 6));
            chip.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(a, 12), Ui.LINE, Ui.dp(a, 1)));
            chip.setLayoutParams(Ui.wrap(a, 6));
            chip.setOnClickListener(v -> {
                r.pausedDates.remove(k);
                changed.run();
            });
            box.addView(chip);
        }

        // Zeile 3: Aktionen
        LinearLayout actions = Ui.row(a);
        actions.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        boolean pausedToday = r.pausedDates.contains(today);
        actions.addView(Ui.smallButton(a, pausedToday ? "Heute fortsetzen" : "Heute aussetzen", Ui.SAGE_DARK, v -> {
            if (pausedToday) r.pausedDates.remove(today);
            else r.pausedDates.add(today);
            changed.run();
        }));
        actions.addView(Ui.smallButton(a, "Tag aussetzen…", Ui.SAGE_DARK, v -> pickPauseDay(a, r, changed)));
        actions.addView(Ui.flex(a));
        actions.addView(Ui.smallButton(a, "Löschen", Ui.CLAY, v -> new AlertDialog.Builder(a)
                .setMessage("Diese Erinnerung (" + Dates.time(r.hour, r.minute) + " Uhr) löschen?")
                .setPositiveButton("Löschen", (d, w) -> {
                    st.deleteReminder(r.id);
                    Notifier.cancel(a, r.id);
                    changed.run();
                })
                .setNegativeButton("Behalten", null)
                .show()));
        box.addView(actions);
        return box;
    }

    /** Knopf „+ Erinnerung hinzufügen“ für eine Aufgabe. */
    public static TextView addButton(Activity a, Store st, long taskId, Runnable changed) {
        TextView b = Ui.smallButton(a, "+ Erinnerung hinzufügen", Ui.SAGE_DARK, v -> {
            int h = 12, m = 0;
            List<Reminder> existing = st.remindersFor(taskId);
            if (!existing.isEmpty()) {
                Reminder last = existing.get(existing.size() - 1);
                h = Math.min(23, last.hour + 2);
                m = last.minute;
            }
            new TimePickerDialog(a, (view, hh, mm) -> {
                st.addReminder(taskId, hh, mm);
                changed.run();
                Toast.makeText(a, "Erinnerung um " + Dates.time(hh, mm) + " Uhr hinzugefügt", Toast.LENGTH_SHORT).show();
            }, h, m, true).show();
        });
        b.setLayoutParams(Ui.wrap(a, 4));
        return b;
    }

    private static void chooseDays(Activity a, Reminder r, Runnable changed) {
        boolean[] checked = new boolean[7];
        for (int i = 0; i < 7; i++) checked[i] = ((r.days >> i) & 1) == 1;
        new AlertDialog.Builder(a)
                .setTitle("An welchen Tagen erinnern?")
                .setMultiChoiceItems(Dates.LONG, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Übernehmen", (d, w) -> {
                    int mask = 0;
                    for (int i = 0; i < 7; i++) if (checked[i]) mask |= (1 << i);
                    if (mask == 0) {
                        Toast.makeText(a, "Bitte mindestens einen Tag wählen – oder die Erinnerung ausschalten.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    r.days = mask;
                    changed.run();
                })
                .setNeutralButton("Täglich", (d, w) -> {
                    r.days = 0x7F;
                    changed.run();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private static void pickPauseDay(Activity a, Reminder r, Runnable changed) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(a, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d, 12, 0, 0);
            String k = Dates.key(sel);
            r.pausedDates.add(k);
            changed.run();
            Toast.makeText(a, "Erinnerung " + Dates.pretty(k) + " ausgesetzt", Toast.LENGTH_SHORT).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }
}
