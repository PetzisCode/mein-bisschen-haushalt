package de.meinbisschen.haushalt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Aufgabe anlegen oder bearbeiten – inklusive Termin, Wochentagen, Energie und Erinnerungen. */
public class TaskEditActivity extends Activity {

    private Store st;
    private Task original;   // null = neue Aufgabe
    private Task work;       // Arbeitskopie
    private boolean isNew;
    private boolean finished;

    private LinearLayout content;
    private EditText titleEdit;
    private EditText noteEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        st = Store.get(this);
        long id = getIntent().getLongExtra("id", -1);
        if (savedInstanceState != null) id = savedInstanceState.getLong("workId", id);
        original = id >= 0 ? st.findTask(id) : null;
        isNew = original == null;
        if (isNew) {
            work = new Task();
            work.id = id >= 0 ? id : st.nextTaskId++;
            st.save(); // ID reservieren
        } else {
            work = original.copy();
        }
        if (savedInstanceState != null && savedInstanceState.getString("work") != null) {
            try {
                Task restored = Task.fromJson(new org.json.JSONObject(savedInstanceState.getString("work")));
                work.applyFrom(restored);
            } catch (Exception ignored) { }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        content = Ui.column(this);
        int p = Ui.dp(this, 18);
        content.setPadding(p, Ui.dp(this, 20), p, Ui.dp(this, 40));
        scroll.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        titleEdit = makeEdit("z. B. Katzenklos reinigen", false);
        titleEdit.setText(work.title);
        noteEdit = makeEdit("Notiz (optional)", true);
        noteEdit.setText(work.note);
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        capture();
        out.putLong("workId", work.id);
        try {
            out.putString("work", work.toJson().toString());
        } catch (Exception ignored) { }
    }

    private EditText makeEdit(String hint, boolean multi) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Ui.TEXT);
        e.setHintTextColor(Ui.MUTED);
        e.setTextSize(17);
        e.setBackground(Ui.rounded(Ui.SURFACE, Ui.dp(this, 14), Ui.SAND_DARK, Ui.dp(this, 1)));
        e.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(2);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        return e;
    }

    private void capture() {
        work.title = titleEdit.getText().toString().trim();
        work.note = noteEdit.getText().toString().trim();
    }

    private void detach(EditText e) {
        if (e.getParent() instanceof ViewGroup) ((ViewGroup) e.getParent()).removeView(e);
    }

    private void render() {
        capture();
        content.removeAllViews();
        detach(titleEdit);
        detach(noteEdit);

        content.addView(Ui.title(this, isNew ? "Neue Aufgabe" : "Aufgabe bearbeiten"));
        content.addView(Ui.space(this, 12));

        content.addView(label("Was möchtest du erledigen?"));
        titleEdit.setLayoutParams(Ui.full(this, 6));
        content.addView(titleEdit);
        noteEdit.setLayoutParams(Ui.full(this, 8));
        content.addView(noteEdit);

        // Wie oft?
        content.addView(label("Wie oft?"));
        LinearLayout typeRow = Ui.row(this);
        typeRow.setPadding(0, Ui.dp(this, 6), 0, 0);
        typeRow.addView(Ui.chip(this, "🔁 Regelmäßig", work.type == Task.REPEAT, v -> {
            work.type = Task.REPEAT;
            if ((work.days & 0x7F) == 0) work.days = 0x7F;
            render();
        }), Ui.weight(this, 1, 3));
        typeRow.addView(Ui.chip(this, "1× Einmalig", work.type == Task.ONCE, v -> {
            work.type = Task.ONCE;
            render();
        }), Ui.weight(this, 1, 3));
        content.addView(typeRow);

        LinearLayout plan = Ui.card(this);
        plan.setLayoutParams(Ui.full(this, 10));
        if (work.type == Task.REPEAT) {
            plan.addView(Ui.text(this, "An welchen Tagen?", 15, Ui.TEXT, true));
            LinearLayout days = Ui.row(this);
            days.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 6));
            for (int i = 0; i < 7; i++) {
                final int bit = 1 << i;
                TextView chip = Ui.chip(this, Dates.SHORT[i], (work.days & bit) != 0, v -> {
                    work.days ^= bit;
                    render();
                });
                chip.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
                days.addView(chip, Ui.weight(this, 1, 2));
            }
            plan.addView(days);
            LinearLayout quick = Ui.row(this);
            quick.addView(Ui.smallButton(this, "Täglich", Ui.SAGE_DARK, v -> {
                work.days = 0x7F;
                render();
            }));
            quick.addView(Ui.smallButton(this, "Mo–Fr", Ui.SAGE_DARK, v -> {
                work.days = 0x1F;
                render();
            }));
            quick.addView(Ui.smallButton(this, "Wochenende", Ui.SAGE_DARK, v -> {
                work.days = 0x60;
                render();
            }));
            plan.addView(quick);
            plan.addView(Ui.text(this, (work.days & 0x7F) == 0
                    ? "Bitte mindestens einen Tag auswählen."
                    : "Geplant: " + Dates.daysText(work.days), 13, Ui.MUTED, false));
        } else {
            plan.addView(Ui.text(this, "Wann?", 15, Ui.TEXT, true));
            TextView d = Ui.text(this, work.date == null
                    ? "Ohne festes Datum – erscheint unter „Wenn du magst“."
                    : "Geplant für " + Dates.pretty(work.date), 15, Ui.TEXT, false);
            d.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
            plan.addView(d);
            LinearLayout dr = Ui.row(this);
            dr.addView(Ui.smallButton(this, work.date == null ? "📅 Datum wählen" : "📅 Datum ändern",
                    Ui.SAGE_DARK, v -> pickDate()));
            if (work.date != null) {
                dr.addView(Ui.smallButton(this, "Datum entfernen", Ui.MUTED, v -> {
                    work.date = null;
                    render();
                }));
            }
            plan.addView(dr);
            if (work.done) {
                plan.addView(Ui.smallButton(this, "Wieder als offen markieren", Ui.SAGE_DARK, v -> {
                    work.done = false;
                    work.doneDate = null;
                    render();
                }));
            }
        }
        content.addView(plan);

        // Energie
        content.addView(label("Wann soll die Aufgabe erscheinen?"));
        String[][] levels = {
                {"🌙  Immer – auch bei wenig Energie", "Das Wichtigste, z. B. Katzenklos"},
                {"🌿  Bei normaler Energie", "Die üblichen Aufgaben"},
                {"☀️  Nur bei viel Energie", "Bonus-Aufgaben und freiwillige Projekte"}};
        for (int i = 0; i < 3; i++) {
            final int lvl = i + 1;
            boolean sel = work.energy == lvl;
            LinearLayout opt = Ui.column(this);
            opt.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
            opt.setBackground(Ui.ripple(Ui.rounded(sel ? Ui.SAGE_LIGHT : Ui.SURFACE, Ui.dp(this, 14),
                    sel ? Ui.SAGE : Ui.LINE, Ui.dp(this, sel ? 2 : 1))));
            opt.addView(Ui.text(this, levels[i][0], 15, Ui.TEXT, sel));
            opt.addView(Ui.text(this, levels[i][1], 13, Ui.MUTED, false));
            opt.setOnClickListener(v -> {
                work.energy = lvl;
                render();
            });
            opt.setLayoutParams(Ui.full(this, 6));
            content.addView(opt);
        }

        // Erinnerungen
        content.addView(label("Erinnerungen"));
        LinearLayout rc = Ui.card(this);
        rc.setLayoutParams(Ui.full(this, 6));
        LinearLayout nrow = Ui.row(this);
        nrow.addView(Ui.text(this, "Benachrichtigung für diese Aufgabe", 15, Ui.TEXT, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Switch sw = new Switch(this);
        sw.setChecked(work.notify);
        sw.setOnCheckedChangeListener((b, c) -> {
            work.notify = c;
            render();
        });
        nrow.addView(sw);
        rc.addView(nrow);
        if (work.notify) {
            Runnable onChange = () -> {
                st.save();
                if (!isNew) Scheduler.rescheduleAll(this);
                render();
            };
            List<Reminder> rs = st.remindersFor(work.id);
            if (rs.isEmpty()) {
                rc.addView(Ui.text(this, "Noch keine Erinnerung. Du kannst eine oder mehrere Uhrzeiten hinzufügen.",
                        13, Ui.MUTED, false));
            }
            for (Reminder r : rs) rc.addView(ReminderViews.build(this, st, work, r, onChange));
            rc.addView(ReminderViews.addButton(this, st, work.id, onChange));
        } else {
            rc.addView(Ui.text(this, "Diese Aufgabe meldet sich nicht.", 13, Ui.MUTED, false));
        }
        content.addView(rc);

        // Knöpfe
        TextView save = Ui.button(this, "Speichern", true, v -> save());
        save.setLayoutParams(Ui.full(this, 22));
        content.addView(save);
        LinearLayout bottom = Ui.row(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(0, Ui.dp(this, 8), 0, 0);
        bottom.addView(Ui.smallButton(this, "Abbrechen", Ui.MUTED, v -> cancel()));
        if (!isNew) bottom.addView(Ui.smallButton(this, "Aufgabe löschen", Ui.CLAY, v -> confirmDelete()));
        content.addView(bottom);
    }

    private TextView label(String s) {
        TextView t = Ui.section(this, s);
        t.setPadding(Ui.dp(this, 4), Ui.dp(this, 20), 0, Ui.dp(this, 2));
        return t;
    }

    private void pickDate() {
        Calendar c = work.date != null ? Dates.parse(work.date) : Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(y, m, d, 12, 0, 0);
            work.date = Dates.key(sel);
            render();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void save() {
        capture();
        if (work.title.isEmpty()) {
            Toast.makeText(this, "Gib der Aufgabe noch einen Namen. 🌿", Toast.LENGTH_SHORT).show();
            titleEdit.requestFocus();
            return;
        }
        if (work.type == Task.REPEAT && (work.days & 0x7F) == 0) {
            Toast.makeText(this, "Wähle mindestens einen Wochentag.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isNew) {
            st.tasks.add(work);
        } else {
            Task live = st.findTask(work.id);
            if (live != null) live.applyFrom(work);
            else st.tasks.add(work);
        }
        st.save();
        Scheduler.rescheduleAll(this);
        Toast.makeText(this, "Gespeichert", Toast.LENGTH_SHORT).show();
        finished = true;
        finish();
    }

    private void cancel() {
        if (isNew) {
            // Erinnerungen, die für die nie gespeicherte Aufgabe angelegt wurden, wieder entfernen.
            List<Reminder> rs = new ArrayList<>(st.remindersFor(work.id));
            for (Reminder r : rs) st.deleteReminder(r.id);
            st.save();
        }
        finished = true;
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("„" + work.title + "“ löschen?")
                .setMessage("Die zugehörigen Erinnerungen werden ebenfalls entfernt.")
                .setPositiveButton("Löschen", (d, w) -> {
                    for (Reminder r : st.remindersFor(work.id)) Notifier.cancel(this, r.id);
                    st.deleteTask(work.id);
                    st.save();
                    Scheduler.rescheduleAll(this);
                    finished = true;
                    finish();
                })
                .setNegativeButton("Behalten", null)
                .show();
    }

    /** Zurück-Taste: Mit Namen wird gespeichert, ohne Namen verworfen – nichts geht versehentlich verloren. */
    @Override
    public void onBackPressed() {
        capture();
        if (!work.title.isEmpty() && !(work.type == Task.REPEAT && (work.days & 0x7F) == 0)) {
            save();
        } else {
            cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!finished && isFinishing() && isNew) {
            List<Reminder> rs = new ArrayList<>(st.remindersFor(work.id));
            if (!rs.isEmpty() && st.findTask(work.id) == null) {
                for (Reminder r : rs) st.deleteReminder(r.id);
                st.save();
            }
        }
    }
}
