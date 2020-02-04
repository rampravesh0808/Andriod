package ae.etisalat.mocasalesportalpoc;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class KeepMOCARunningSerivice extends Service {

    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // do your jobs here
        Log.i("KeepMOCARunningSerivice","onStartCommand");
        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    private void startForeground() {
        Intent notificationIntent = new Intent(this, MOCAMainActivityPOC.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Log.i("KeepMOCARunningSerivice","startForeground");
        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                NOTIF_CHANNEL_ID) // don't forget create a notification channel first
                .setOngoing(true)
                //.setSmallIcon(R.drawable.ic_notification)
              //  .setContentTitle(getString(R.string.app_name))
                //.setContentText("Service is running background")
                .setContentIntent(pendingIntent)
                .build());
    }
}
