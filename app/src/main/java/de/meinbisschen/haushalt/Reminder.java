package de.meinbisschen.haushalt;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/** Eine Erinnerung zu einer Aufgabe – Uhrzeit, Wochentage, an/aus und ausgesetzte Tage. */
public class Reminder {
    public int id;
    public long taskId;
    public int hour;
    public int minute;
    /** Bit 0 = Montag … Bit 6 = Sonntag. */
    public int days = 0x7F;
    public boolean enabled = true;
    /** Tage (yyyy-MM-dd), an denen diese Erinnerung ausgesetzt ist. */
    public Set<String> pausedDates = new HashSet<>();

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("taskId", taskId);
        o.put("hour", hour);
        o.put("minute", minute);
        o.put("days", days);
        o.put("enabled", enabled);
        o.put("pausedDates", Store.setToJson(pausedDates));
        return o;
    }

    public static Reminder fromJson(JSONObject o) throws JSONException {
        Reminder r = new Reminder();
        r.id = o.getInt("id");
        r.taskId = o.getLong("taskId");
        r.hour = o.optInt("hour", 12);
        r.minute = o.optInt("minute", 0);
        r.days = o.optInt("days", 0x7F);
        r.enabled = o.optBoolean("enabled", true);
        r.pausedDates = Store.jsonToSet(o.optJSONArray("pausedDates"));
        return r;
    }
}
