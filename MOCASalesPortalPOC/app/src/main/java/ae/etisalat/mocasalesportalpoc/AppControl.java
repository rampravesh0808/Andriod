package ae.etisalat.mocasalesportalpoc;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.innoquant.moca.MOCA;
import com.innoquant.moca.MOCAAction;
import com.innoquant.moca.MOCAProximityService;

import org.altbeacon.beacon.startup.StartupBroadcastReceiver;

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
import java.util.logging.LogRecord;

public class AppControl extends Application {
    private int exitCount;

    private String crmEndPointURl;
    private boolean nativeAPKMode=true;

    private boolean isDeviceInBeaconRange;
    private String deviceIMEI;
    private String beaconDeviceID;

    private long EXIT_TIME_MS = 45000;
    private Handler handler;
    private HandlerThread handlerThread;

    private Runnable exitCallRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i("exitCallRunnable", "Exit from beacon range!");

            if (!nativeAPKMode) {
                prepareHttpPostReq("OnExitPartnerBeacons","N");
            }
            isDeviceInBeaconRange = false;

            Intent intent = new Intent(MOCAMainActivityPOC.CUSTOM_ACTION_NOTIFICATION_RECEIVED);
            intent.setType("text/plain");
            intent.putExtra(MOCAMainActivityPOC.CUSTOM_MESSAGE, "OnExitPartnerBeacons");
            sendBroadcast(intent);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        // OnExit Handler Thread.
        handlerThread = new HandlerThread("MyBackThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        MOCA.initializeSDK(this);
        this.exitCount = 0;

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        this.deviceIMEI = "";
        if (telephonyManager != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                this.deviceIMEI = telephonyManager.getDeviceId();
            }
        }

        //Log.i("KeepMOCARunningSerivice","Start");
        //startService(new Intent(this,KeepMOCARunningSerivice.class));

        crmEndPointURl = "http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices";

        if (MOCA.initialized()) {

            if(!isDeviceInBeaconRange && !this.nativeAPKMode) {
                prepareHttpPostReq("APK_INIT","N");
            }

            MOCAProximityService proxServiceActionListener = MOCA.getProximityService();
            if (proxServiceActionListener != null) {
                proxServiceActionListener.setActionListener(new MOCAProximityService.ActionListener() {

                    @Override
                    public boolean displayNotificationAlert(MOCAAction mocaAction, String s) {

                        Log.i("NotificationAlert", s);
                        String msg = "ActionId-" + mocaAction.getActionId() + ",CampaignId-" + mocaAction.getCampaignId() + ",BackgroundAlert-" + mocaAction.getBackgroundAlert();
                        Log.i("NotificationAlert", msg);

                        Intent intent = new Intent(MOCAMainActivityPOC.ACTION_NOTIFICATION_RECEIVED);
                        intent.setType("text/plain");
                        intent.putExtra(MOCAMainActivityPOC.MESSAGE, s);
                        sendBroadcast(intent);
                        return true;
                    }

                    @Override
                    public boolean openUrl(MOCAAction mocaAction, String s) {
                        // Toast.makeText(getApplicationContext(), "openUrl.", Toast.LENGTH_SHORT).show();
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
                        Log.i("performCustomAction", s+"-"+exitCount);

                        if("OnExitPartnerBeacons".equalsIgnoreCase(s) ){

                            handler.removeCallbacksAndMessages(null);
                            Log.i("exitCallRunnable", "OnExitremoveCallbacksAndMessages");

                            Log.i("exitCallRunnable", "OnExitpostDelayed");
                            handler.postDelayed(exitCallRunnable,EXIT_TIME_MS);

                        }else if("OnEnterPartnerBeacons".equalsIgnoreCase(s)){

                            if(!isDeviceInBeaconRange){
                                if(!nativeAPKMode) {
                                    prepareHttpPostReq(s,"Y");
                                }
                            }
                            isDeviceInBeaconRange = true;

                            Intent intent = new Intent(MOCAMainActivityPOC.CUSTOM_ACTION_NOTIFICATION_RECEIVED);
                            intent.setType("text/plain");
                            intent.putExtra(MOCAMainActivityPOC.CUSTOM_MESSAGE, s);
                            sendBroadcast(intent);

                            handler.removeCallbacksAndMessages(null);
                            Log.i("exitCallRunnable", "OnEnterremoveCallbacksAndMessages");

                            Log.i("exitCallRunnable", "OnEnterpostDelayed");
                            handler.postDelayed(exitCallRunnable,EXIT_TIME_MS);
                        }
                        return true;
                    }
                });
            }
        }
    }

    private void prepareHttpPostReq(String msg,String inRange)
    {
        Date currentDateTime = new Date();
        Map<String, String> dataMap = new HashMap<String, String>();
        dataMap.put("beaconDeviceID", "BEACON_MOCA");
        dataMap.put("deviceIMEI", deviceIMEI);
        dataMap.put("InRange", inRange);
        dataMap.put("eventRaisedDateTime", currentDateTime.toString());
        dataMap.put("mocaExprinceName", msg);
        dataMap.put("mocaEvenDistance", "NA");
        dataMap.put("mocaEventName", msg);

        String sendHttpRequestDataArry[] = {
                crmEndPointURl,
                createQueryStringForParameters(dataMap)
        };
        new AppControl.SendHttpRequestTask().execute(sendHttpRequestDataArry);
    }

    private static final char PARAMETER_DELIMITER = '&';
    private static final char PARAMETER_EQUALS_CHAR = '=';

    public String createQueryStringForParameters(Map<String, String> parameters) {
        StringBuilder parametersAsQueryString = new StringBuilder();
        if (parameters != null) {
            boolean firstParameter = true;

            for (String parameterName : parameters.keySet()) {
                if (!firstParameter) {
                    parametersAsQueryString.append(PARAMETER_DELIMITER);
                }

                try {
                    parametersAsQueryString.append(parameterName)
                            .append(PARAMETER_EQUALS_CHAR)
                            .append(URLEncoder.encode(parameters.get(parameterName), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                firstParameter = false;
            }
        }
        return parametersAsQueryString.toString();
    }

    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String returnResponse="FAILED";
            try {
                Log.i("SendHttpRequestTask", "doInBackground-Start");
                String endPointURl = strings[0];
                String postParameters = strings[1];
                Log.i("SendHttpRequestTask", "endPointURl- "+endPointURl);
                Log.i("SendHttpRequestTask", "postParameters- "+postParameters);
                URL url = null;
                try {
                    url = new URL(endPointURl);
                    HttpURLConnection client = null;
                    Log.i("HttpURLConnection", "Start");
                    try {
                        client = (HttpURLConnection) url.openConnection();
                        client.setRequestMethod("POST");
                        client.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        client.setDoOutput(true);

                        client.setFixedLengthStreamingMode(postParameters.length());
                        //client.setChunkedStreamingMode(0);
                        Log.i("HttpURLConnection", "SetFixedLengthStreamingMode");

                        OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
                        Log.i("HttpURLConnection", "OutputStreamCreated");

                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputPost);
                        Log.i("HttpURLConnection", "OutputStreamWriterCreated");

                        outputStreamWriter.write(postParameters);
                        Log.i("HttpURLConnection", "Wrote Data on OutputStreamWriter!");

                        outputStreamWriter.flush();
                        Log.i("HttpURLConnection", "DataFlushed on OutputStreamWriter!");
                        outputStreamWriter.close();
                        Log.i("HttpURLConnection", "OutputStreamWriter CLOSED!");
                        outputPost.close();
                        Log.i("HttpURLConnection", "OutputStream CLOSED!");

                        int statusCode = client.getResponseCode();
                        Log.i("HttpURLConnection", "statusCode- "+statusCode);
                        if(statusCode == HttpURLConnection.HTTP_OK){
                            returnResponse="SUCCESS";
                            Log.i("HttpURLConnection", "Post parameter sent successfully!");
                        }
                        else{
                            returnResponse="FAILED";
                            Log.i("HttpURLConnection", "Failed to Post parameter to servers!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("HttpURLConnection", e.getMessage());
                        Log.i("SendHttpRequestTask", "ExceptionInnerTry");
                    } finally {
                        if (client != null)
                            client.disconnect();
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.i("SendHttpRequestTask", "MalformedURLException");
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.i("SendHttpRequestTask", "ExceptionMainTry");
            }
            Log.i("SendHttpRequestTask", "doInBackground-End");
            return returnResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
        }
    }
}
