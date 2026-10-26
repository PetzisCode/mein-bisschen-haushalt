package de.meinbisschen.haushalt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

/** Zeigt die freundlichen Benachrichtigungen an. */
public final class Notifier {

    public static final String CHANNEL = "sanfte_erinnerungen";
    private static final int TEST_ID = 999_999;

    private Notifier() { }

    public static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Sanfte Erinnerungen",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Freundliche Erinnerungen an deine Haushaltsaufgaben");
            nm.createNotificationChannel(ch);
        }
    }

    private static PendingIntent openApp(Context ctx, int requestCode) {
        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(ctx, requestCode, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void show(Context ctx, Reminder r, Task t) {
        ensureChannel(ctx);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        Store st = Store.get(ctx);
        String msg = Messages.pick(ctx, t.title, r.hour);

        Intent done = new Intent(ctx, ReminderReceiver.class).setAction(Scheduler.ACTION_DONE);
        done.putExtra("rid", r.id);
        done.putExtra("tid", t.id);
        PendingIntent donePi = PendingIntent.getBroadcast(ctx, Scheduler.DONE_BASE + r.id, done,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent later = new Intent(ctx, ReminderReceiver.class).setAction(Scheduler.ACTION_SNOOZE);
        later.putExtra("rid", r.id);
        PendingIntent laterPi = PendingIntent.getBroadcast(ctx, Scheduler.SNOOZE_ACTION_BASE + r.id, later,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Icon icon = Icon.createWithResource(ctx, R.drawable.ic_notif);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(Ui.SAGE)
                .setContentTitle(t.title)
                .setContentText(msg)
                .setStyle(new Notification.BigTextStyle().bigText(msg))
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx, Scheduler.OPEN_BASE + r.id))
                .addAction(new Notification.Action.Builder(icon, "Erledigt ✓", donePi).build())
                .addAction(new Notification.Action.Builder(icon, "Später (" + st.snoozeMinutes + " Min.)", laterPi).build())
                .build();
        try {
            nm.notify(r.id, n);
        } catch (SecurityException ignored) {
            // Benachrichtigungen nicht erlaubt
        }
    }

    public static void showTest(Context ctx) {
        ensureChannel(ctx);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(Ui.SAGE)
                .setContentTitle("Mein bisschen Haushalt")
                .setContentText(Messages.any())
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx, TEST_ID))
                .build();
        try {
            nm.notify(TEST_ID, n);
        } catch (SecurityException ignored) { }
    }

    public static void cancel(Context ctx, int id) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(id);
    }
}
