package de.meinbisschen.haushalt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Empfängt die Wecker und die Knöpfe „Erledigt“ / „Später“ aus der Benachrichtigung. */
public class ReminderReceiver extends BroadcastReceiver {

    private static final long LATE_TOLERANCE = 3L * 60 * 60 * 1000;   // 3 Stunden
    private static final long EARLY_TOLERANCE = 10L * 60 * 1000;      // 10 Minuten

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        Store st = Store.get(ctx);
        int rid = intent.getIntExtra("rid", -1);
        Reminder r = st.findReminder(rid);

        switch (action) {
            case Scheduler.ACTION_FIRE: {
                // Nur auslösen, wenn die Erinnerung noch genau so eingestellt ist wie beim Planen.
                if (r != null && r.hour == intent.getIntExtra("h", -1) && r.minute == intent.getIntExtra("m", -1)) {
                    long when = intent.getLongExtra("when", 0);
                    long now = System.currentTimeMillis();
                    Calendar day = Calendar.getInstance();
                    day.setTimeInMillis(when);
                    Task t = st.findTask(r.taskId);
                    if (now - when < LATE_TOLERANCE && when - now < EARLY_TOLERANCE && st.reminderFiresOn(r, t, day)) {
                        Notifier.show(ctx, r, t);
                    }
                }
                Scheduler.rescheduleAll(ctx);
                break;
            }
            case Scheduler.ACTION_SNOOZE_FIRE: {
                if (r == null) break;
                Task t = st.findTask(r.taskId);
                Calendar today = Calendar.getInstance();
                String key = Dates.key(today);
                if (t != null && st.remindersOn && r.enabled && t.notify
                        && !t.isDoneOn(key) && !r.pausedDates.contains(key)) {
                    Notifier.show(ctx, r, t);
                }
                break;
            }
            case Scheduler.ACTION_DONE: {
                Task t = st.findTask(intent.getLongExtra("tid", -1));
                if (t != null) {
                    t.setDone(Dates.todayKey(), true);
                    st.save();
                }
                Notifier.cancel(ctx, rid);
                Scheduler.rescheduleAll(ctx);
                break;
            }
            case Scheduler.ACTION_SNOOZE: {
                Notifier.cancel(ctx, rid);
                Scheduler.snooze(ctx, rid);
                break;
            }
            default:
                break;
        }
    }
}
