package de.meinbisschen.haushalt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Nach Neustart, App-Update oder Zeitumstellung alle Erinnerungen neu setzen. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        try {
            Scheduler.rescheduleAll(ctx);
        } catch (Exception ignored) {
            // z. B. Direct-Boot vor dem Entsperren: beim nächsten Signal klappt es.
        }
    }
}
