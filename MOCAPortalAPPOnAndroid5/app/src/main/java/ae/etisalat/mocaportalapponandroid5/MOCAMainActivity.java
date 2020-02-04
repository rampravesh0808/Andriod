package ae.etisalat.mocaportalapponandroid5;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Set;


    public class MOCAMainActivity extends Activity {

    private NotificationOnPerformCustomActionReceiver notificationOnPerformCustomActionReceiver;
    public static final String CUSTOM_ACTION_NOTIFICATION_RECEIVED = "CUSTOM_ACTION_NOTIFICATION_RECEIVED";
    public static final String CUSTOM_MESSAGE = "CUSTOM_MESSAGE";

    private Map<String, String> crmDataMap;
    private String crmEndPointURl = "";
    private boolean isDeviceInBeaconRange;
    private String deviceIMEI;
    private String beaconDeviceID;

    private boolean isNataveLaunch = true;

    private ProgressDialog pd;

    private boolean isServiceBased = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setTheme(android.R.style.Theme_NoDisplay);

        pd = new ProgressDialog(this);
        pd.setTitle("Please wait....");
        pd.setMessage("Verifying your POS location.");
        pd.setCancelable(false);
        pd.show();

        if(isServiceBased){
            pd.dismiss();
            finish();
        }
        else {

            //crmEndPointURl = "http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices";
            Intent intent = getIntent();
            if(intent != null) {
                String action = intent.getAction();
                Log.i("CRMSignIntent", action);
                Uri data = intent.getData();
                if(data != null) {
                    // data.getQuery() -> redirecturl=http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices&user_id=CSSQA17&ReferenceNo=10558928&ApplType=PREPAID_AN&deviceIMEI=56456465465464&TASK_ID=CSSQA17-1548238190598
                    // data.getQueryParameterNames().toString() -> [redirecturl, user_id, ReferenceNo, ApplType, TASK_ID, deviceIMEI]
                    // data.getQueryParameter("redirecturl")  -->>  http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices

                    this.crmEndPointURl = data.getQueryParameter("redirecturl");
                    Set<String> queryParameters = data.getQueryParameterNames();

                    crmDataMap = new HashMap<String, String>();
                    for (String s : queryParameters) {
                        System.out.println(s);
                        if(s.equalsIgnoreCase("redirecturl")){
                            continue;
                        }
                        if(s.equalsIgnoreCase("deviceIMEI")){
                            this.deviceIMEI = data.getQueryParameter("deviceIMEI");
                        }
                        crmDataMap.put(s,data.getQueryParameter(s));
                    }
                }
            }

                if (notificationOnPerformCustomActionReceiver == null) {
                    notificationOnPerformCustomActionReceiver = new NotificationOnPerformCustomActionReceiver();
                    registerReceiver(notificationOnPerformCustomActionReceiver, IntentFilter.create(CUSTOM_ACTION_NOTIFICATION_RECEIVED, "text/plain"));
                }
                // Fetch Device IMEI number
                /*
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
                this.deviceIMEI = "";
                if (telephonyManager != null) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        this.deviceIMEI = telephonyManager.getDeviceId();
                    }
                }
                */

                // OnExit Handler Thread.
                handlerThread = new HandlerThread("MyBackThread");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper());

                Log.i("exitCallRunnable", "OnEnterpostDelayed");
                handler.postDelayed(exitCallRunnable, EXIT_TIME_MS);
        }

    }

    private void updateUI(String message,String msgID) {

       if(msgID.equalsIgnoreCase(CUSTOM_MESSAGE)) {
           Date currentDateTime = new Date();
           if("OnEnterPartnerBeacons".equalsIgnoreCase(message)){
               if(!this.isDeviceInBeaconRange){
               }
               this.isDeviceInBeaconRange = true;

               handler.removeCallbacksAndMessages(null);
               Log.i("exitCallRunnable", "OnEnterremoveCallbacksAndMessages");

               // Notify to CRM
               if(isNataveLaunch) {
                   Toast.makeText(getApplicationContext(), "Notify, CRM in Beacon Range.", Toast.LENGTH_LONG).show();
                   //textViewBeaconMsg.setText("Notify, CRM in Beacon Range. \n " + currentDateTime.toString());
               }else {
                   prepareHttpPostReq(message,"Y");
               }
           } else if ("OnExitPartnerBeacons".equalsIgnoreCase(message)){
               if(this.isDeviceInBeaconRange){
               }
               this.isDeviceInBeaconRange = false;

               handler.removeCallbacksAndMessages(null);
               Log.i("exitCallRunnable", "OnEnterremoveCallbacksAndMessages");

               // Notify to CRM
               if(isNataveLaunch) {
                   Toast.makeText(getApplicationContext(), "Notify, CRM exit from Beacon Range.", Toast.LENGTH_LONG).show();
                   //textViewBeaconMsg.setText("Notify, CRM exit from Beacon Range. \n " + currentDateTime.toString());
               }else {
                   prepareHttpPostReq(message,"N");
               }
           }
            pd.dismiss();
            finish();
        }
    }

    public class NotificationOnPerformCustomActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(CUSTOM_MESSAGE);
            updateUI(message, CUSTOM_MESSAGE);
        }

    }

    private long EXIT_TIME_MS = 55000;
    private Handler handler;
    private HandlerThread handlerThread;

    private Runnable exitCallRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i("exitCallRunnable", "Exit from beacon range, No Beacon Event!");
            // Notify to CRM
            //prepareHttpPostReq("APK_INIT","N");
            pd.dismiss();
            finish();
        }
    };


    private void prepareHttpPostReq(String msg,String inRange)
    {
        Date currentDateTime = new Date();
       // Map<String, String> dataMap = new HashMap<String, String>();
        crmDataMap.put("beaconDeviceID", "BEACON_MOCA");
        //dataMap.put("deviceIMEI", deviceIMEI);
        crmDataMap.put("InRange", inRange);
        crmDataMap.put("eventRaisedDateTime", currentDateTime.toString());
        crmDataMap.put("mocaExprinceName", msg);
        crmDataMap.put("mocaEvenDistance", "NA");
        crmDataMap.put("mocaEventName", msg);

        String sendHttpRequestDataArry[] = {
                crmEndPointURl,
                createQueryStringForParameters(crmDataMap)
        };
        new MOCAMainActivity.SendHttpRequestTask().execute(sendHttpRequestDataArry);
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