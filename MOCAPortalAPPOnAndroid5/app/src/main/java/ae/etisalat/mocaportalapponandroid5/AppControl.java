package ae.etisalat.mocaportalapponandroid5;

import android.Manifest;
import android.app.Application;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.innoquant.moca.MOCA;
import com.innoquant.moca.MOCAAction;
import com.innoquant.moca.MOCAProximityService;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AppControl extends Application {

    private long EXIT_TIME_MS = 50000;
    private Handler handler;
    private HandlerThread handlerThread;

    private boolean isServiceBased = false;
    private String crmEndPointURl = "";
    private boolean isDeviceInBeaconRange;
    private String deviceIMEI;
    private String beaconDeviceID;


    private Runnable exitCallRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i("exitCallRunnable", "Exit from beacon range!");
            if(isServiceBased) {
                Log.i("exitCallRunnable", "Exit from beacon range! isServiceBased");
               // prepareHttpPostReq("OnExitPartnerBeacons", "N");
            }else {
                Intent intent = new Intent(MOCAMainActivity.CUSTOM_ACTION_NOTIFICATION_RECEIVED);
                intent.setType("text/plain");
                intent.putExtra(MOCAMainActivity.CUSTOM_MESSAGE, "OnExitPartnerBeacons");
                sendBroadcast(intent);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        MOCA.initializeSDK(this);

        // OnExit Handler Thread.
        handlerThread = new HandlerThread("MyBackThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        if(isServiceBased) {
            crmEndPointURl = "http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices";
            // Fetch Device IMEI number
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
            this.deviceIMEI = "";
            if (telephonyManager != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    this.deviceIMEI = telephonyManager.getDeviceId();
                }
            }

            isDeviceInBeaconRange = false;

            Intent crmPortalMOCAProximityServiceIntent = new Intent(this, CRMPortalMOCAProximityService.class);
            startService(crmPortalMOCAProximityServiceIntent);
        }
        else if (MOCA.initialized()) {
            MOCAProximityService proxServiceActionListener = MOCA.getProximityService();
            if (proxServiceActionListener != null) {
                proxServiceActionListener.setActionListener(new MOCAProximityService.ActionListener() {

                    @Override
                    public boolean displayNotificationAlert(MOCAAction mocaAction, String s) {
                        Log.i("NotificationAlert", s);
                        return true;
                    }

                    @Override
                    public boolean openUrl(MOCAAction mocaAction, String s) {
                        return true;
                    }

                    @Override
                    public boolean showHtmlWithString(MOCAAction mocaAction, String s) {
                        return true;
                    }

                    @Override
                    public boolean playVideoFromUrl(MOCAAction mocaAction, String s) {
                        return true;
                    }

                    @Override
                    public boolean displayImageFromUrl(MOCAAction mocaAction, String s) {
                        return true;
                    }

                    @Override
                    public boolean displayPassFromUrl(MOCAAction mocaAction, String s) {
                        return true;
                    }

                    @Override
                    public boolean addTag(MOCAAction mocaAction, String s, String s1) {
                        return true;
                    }

                    @Override
                    public boolean playNotificationSound(MOCAAction mocaAction, String s) {
                        return true;
                    }

                    @Override
                    public boolean performCustomAction(MOCAAction mocaAction, String s) {

                        if ("OnExitPartnerBeacons".equalsIgnoreCase(s)) {

                        } else if ("OnEnterPartnerBeacons".equalsIgnoreCase(s)) {
                        }
                        Intent intent = new Intent(MOCAMainActivity.CUSTOM_ACTION_NOTIFICATION_RECEIVED);
                        intent.setType("text/plain");
                        intent.putExtra(MOCAMainActivity.CUSTOM_MESSAGE, "OnEnterPartnerBeacons");
                        sendBroadcast(intent);

                        handler.removeCallbacksAndMessages(null);
                        Log.i("performCustomAction", "removeCallbacksAndMessages");
                        Log.i("performCustomAction", "postDelayed");
                        handler.postDelayed(exitCallRunnable, EXIT_TIME_MS);
                        return true;
                    }
                });

            }
        }

    }

}
