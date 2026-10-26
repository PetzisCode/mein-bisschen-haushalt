package de.meinbisschen.haushalt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.HashSet;
import java.util.Set;

/**
 * Plant alle Erinnerungen über den System-Wecker (AlarmManager).
 * Bei jeder Änderung werden ALLE alten Wecker gelöscht und neu gesetzt –
 * so kann keine Erinnerung zu einer alten Uhrzeit weiterlaufen.
 */
public final class Scheduler {

    public static final String ACTION_FIRE = "de.meinbisschen.haushalt.FIRE";
    public static final String ACTION_SNOOZE_FIRE = "de.meinbisschen.haushalt.SNOOZE_FIRE";
    public static final String ACTION_DONE = "de.meinbisschen.haushalt.DONE";
    public static final String ACTION_SNOOZE = "de.meinbisschen.haushalt.SNOOZE";

    static final int SNOOZE_BASE = 1_000_000;
    static final int DONE_BASE = 2_000_000;
    static final int SNOOZE_ACTION_BASE = 3_000_000;
    static final int OPEN_BASE = 4_000_000;

    private Scheduler() { }

    private static Intent fireIntent(Context ctx) {
        return new Intent(ctx, ReminderReceiver.class).setAction(ACTION_FIRE);
    }

    public static synchronized void rescheduleAll(Context context) {
        Context ctx = context.getApplicationContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        SharedPreferences sp = ctx.getSharedPreferences("scheduler", Context.MODE_PRIVATE);

        // 1. Alle bisher gesetzten Wecker entfernen
        Set<String> old = new HashSet<>(sp.getStringSet("ids", new HashSet<>()));
        for (String s : old) {
            try {
                int id = Integer.parseInt(s);
                PendingIntent pi = PendingIntent.getBroadcast(ctx, id, fireIntent(ctx),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.cancel(pi);
                pi.cancel();
            } catch (Exception ignored) { }
        }

        // 2. Für jede aktive Erinnerung den nächsten Termin setzen
        Store st = Store.get(ctx);
        Set<String> scheduled = new HashSet<>();
        long now = System.currentTimeMillis();
        for (Reminder r : st.reminders) {
            long when = st.nextTrigger(r, now);
            if (when < 0) continue;
            Intent i = fireIntent(ctx);
            i.putExtra("rid", r.id);
            i.putExtra("h", r.hour);
            i.putExtra("m", r.minute);
            i.putExtra("when", when);
            PendingIntent pi = PendingIntent.getBroadcast(ctx, r.id, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            setAlarm(am, when, pi);
            scheduled.add(String.valueOf(r.id));
        }
        sp.edit().putStringSet("ids", scheduled).apply();
    }

    /** „Später erinnern“: einmaliger Wecker in X Minuten. */
    public static void snooze(Context context, int reminderId) {
        Context ctx = context.getApplicationContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Store st = Store.get(ctx);
        long when = System.currentTimeMillis() + st.snoozeMinutes * 60_000L;
        Intent i = new Intent(ctx, ReminderReceiver.class).setAction(ACTION_SNOOZE_FIRE);
        i.putExtra("rid", reminderId);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, SNOOZE_BASE + reminderId, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setAlarm(am, when, pi);
    }

    public static boolean canExact(Context ctx) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    private static void setAlarm(AlarmManager am, long when, PendingIntent pi) {
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    /** Nächste anstehende Erinnerung (für die Anzeige), oder null. */
    public static String nextInfo(Context ctx) {
        Store st = Store.get(ctx);
        long now = System.currentTimeMillis();
        long best = -1;
        Reminder bestR = null;
        for (Reminder r : st.reminders) {
            long w = st.nextTrigger(r, now);
            if (w > 0 && (best < 0 || w < best)) {
                best = w;
                bestR = r;
            }
        }
        if (bestR == null) return null;
        Task t = st.findTask(bestR.taskId);
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(best);
        String day = Dates.pretty(Dates.key(c));
        day = day.substring(0, 1).toUpperCase(java.util.Locale.GERMAN) + day.substring(1);
        return day + ", " + Dates.time(bestR.hour, bestR.minute) + " Uhr – " + (t != null ? t.title : "");
    }
}
