package ae.etisalat.mocaportalapponandroid5;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class CRMPortalMOCAProximityService extends IntentService {

    public CRMPortalMOCAProximityService() {
        super("CRMPortalMOCAProximityService");
        Log.i("CRMPortalService", "Constructor");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        {
            {
                Log.i("CRMPortalService", "onHandleIntent");
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
                            Log.i("CRMPortalService", "performCustomAction");
                            return true;
                        }
                    });

                }
            }
        }
    }
}
