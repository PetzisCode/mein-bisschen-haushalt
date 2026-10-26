package de.meinbisschen.haushalt;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Eine Haushaltsaufgabe – regelmäßig (an Wochentagen) oder einmalig (mit oder ohne Datum). */
public class Task {

    public static final int ONCE = 0;
    public static final int REPEAT = 1;

    /** 1 = wichtig (auch bei wenig Energie), 2 = normal, 3 = nur bei viel Energie. */
    public static final int ENERGY_LOW = 1;
    public static final int ENERGY_NORMAL = 2;
    public static final int ENERGY_HIGH = 3;

    public long id;
    public String title = "";
    public String note = "";
    public int type = REPEAT;
    /** Bit 0 = Montag … Bit 6 = Sonntag. */
    public int days = 0x7F;
    /** Nur für einmalige Aufgaben: geplantes Datum (yyyy-MM-dd) oder null = ohne Datum. */
    public String date = null;
    public int energy = ENERGY_NORMAL;
    public boolean notify = true;

    /** Einmalige Aufgaben: erledigt? */
    public boolean done = false;
    public String doneDate = null;

    /** Regelmäßige Aufgaben: an diesen Tagen erledigt. */
    public Set<String> doneDates = new HashSet<>();
    /** Regelmäßige Aufgaben: an diesen Tagen ausgelassen / weggeschoben. */
    public Set<String> skipDates = new HashSet<>();
    /** Regelmäßige Aufgaben: zusätzlich an diesen Tagen (z. B. nach dem Verschieben). */
    public Set<String> extraDates = new HashSet<>();

    public boolean isDaily() {
        return type == REPEAT && (days & 0x7F) == 0x7F;
    }

    /** Ist die Aufgabe an diesem Tag eingeplant? */
    public boolean isOnDay(String key, int weekdayIdx) {
        if (type == REPEAT) {
            if (skipDates.contains(key)) return false;
            if (extraDates.contains(key)) return true;
            return ((days >> weekdayIdx) & 1) == 1;
        }
        return date != null && date.equals(key);
    }

    public boolean isDoneOn(String key) {
        if (type == REPEAT) return doneDates.contains(key);
        return done;
    }

    public void setDone(String key, boolean value) {
        if (type == REPEAT) {
            if (value) doneDates.add(key);
            else doneDates.remove(key);
        } else {
            done = value;
            doneDate = value ? key : null;
        }
    }

    public Task copy() {
        Task t = new Task();
        t.applyFrom(this);
        t.id = id;
        return t;
    }

    /** Übernimmt alle Inhalte (außer der ID) aus einer anderen Aufgabe. */
    public void applyFrom(Task o) {
        title = o.title;
        note = o.note;
        type = o.type;
        days = o.days;
        date = o.date;
        energy = o.energy;
        notify = o.notify;
        done = o.done;
        doneDate = o.doneDate;
        doneDates = new HashSet<>(o.doneDates);
        skipDates = new HashSet<>(o.skipDates);
        extraDates = new HashSet<>(o.extraDates);
    }

    /** Entfernt alte Einträge, damit die Datei klein bleibt. */
    public void prune(String oldestKeep) {
        doneDates.removeIf(k -> k.compareTo(oldestKeep) < 0);
        skipDates.removeIf(k -> k.compareTo(oldestKeep) < 0);
        extraDates.removeIf(k -> k.compareTo(oldestKeep) < 0);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("note", note);
        o.put("type", type);
        o.put("days", days);
        if (date != null) o.put("date", date);
        o.put("energy", energy);
        o.put("notify", notify);
        o.put("done", done);
        if (doneDate != null) o.put("doneDate", doneDate);
        o.put("doneDates", Store.setToJson(doneDates));
        o.put("skipDates", Store.setToJson(skipDates));
        o.put("extraDates", Store.setToJson(extraDates));
        return o;
    }

    public static Task fromJson(JSONObject o) throws JSONException {
        Task t = new Task();
        t.id = o.getLong("id");
        t.title = o.optString("title", "");
        t.note = o.optString("note", "");
        t.type = o.optInt("type", REPEAT);
        t.days = o.optInt("days", 0x7F);
        t.date = o.has("date") && !o.isNull("date") ? o.getString("date") : null;
        t.energy = o.optInt("energy", ENERGY_NORMAL);
        t.notify = o.optBoolean("notify", true);
        t.done = o.optBoolean("done", false);
        t.doneDate = o.has("doneDate") && !o.isNull("doneDate") ? o.getString("doneDate") : null;
        t.doneDates = Store.jsonToSet(o.optJSONArray("doneDates"));
        t.skipDates = Store.jsonToSet(o.optJSONArray("skipDates"));
        t.extraDates = Store.jsonToSet(o.optJSONArray("extraDates"));
        return t;
    }
}
