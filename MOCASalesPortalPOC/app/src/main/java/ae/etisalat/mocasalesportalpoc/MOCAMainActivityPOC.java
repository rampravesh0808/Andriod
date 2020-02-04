package ae.etisalat.mocasalesportalpoc;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.innoquant.moca.MOCA;

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

public class MOCAMainActivityPOC extends AppCompatActivity {
    private NotificationReceiver notificationReceiver;
    public static final String ACTION_NOTIFICATION_RECEIVED = "ACTION_NOTIFICATION_RECEIVED";
    public static final String MESSAGE = "MESSAGE";

    private  NotificationOnPerformCustomActionReceiver notificationOnPerformCustomActionReceiver;
    public static final String CUSTOM_ACTION_NOTIFICATION_RECEIVED = "CUSTOM_ACTION_NOTIFICATION_RECEIVED";
    public static final String CUSTOM_MESSAGE = "CUSTOM_MESSAGE";

    private TextView textBeaconRangeMsg;
    private TextView textViewBeaconEnterMsg;
    private TextView textViewBeaconExitMsg;
    private TextView textViewBeaconNotificationMsg;
    private TextView textBeaconInRage;

    private String crmEndPointURl;
    private boolean nativeAPKMode=true;

    private boolean isDeviceInBeaconRange;
    private String deviceIMEI;
    private String beaconDeviceID;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mocamain_poc);

        this.isDeviceInBeaconRange = false;

        //
        if (notificationReceiver == null) {
            notificationReceiver = new NotificationReceiver();
            registerReceiver(notificationReceiver, IntentFilter.create(ACTION_NOTIFICATION_RECEIVED, "text/plain"));
        }

        //
        if (notificationOnPerformCustomActionReceiver == null) {
            notificationOnPerformCustomActionReceiver = new NotificationOnPerformCustomActionReceiver();
            registerReceiver(notificationOnPerformCustomActionReceiver, IntentFilter.create(CUSTOM_ACTION_NOTIFICATION_RECEIVED, "text/plain"));
        }

        crmEndPointURl = "http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices";

        String textViewBeaconInitMsgStr = "";
        if (MOCA.initialized()) {
            textViewBeaconInitMsgStr = "MOCA SDK Initilized.";
        } else {
            textViewBeaconInitMsgStr = "MOCA SDK is not Initilized.";
        }

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(this.TELEPHONY_SERVICE);
        String deviceIMEI = "";
        this.deviceIMEI = "";
        if (telephonyManager != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                    == PackageManager.PERMISSION_GRANTED) {
                deviceIMEI = "Device IMEI:            " + telephonyManager.getDeviceId();
                this.deviceIMEI = telephonyManager.getDeviceId();
            }
        }

        textViewBeaconEnterMsg = (TextView) findViewById(R.id.textViewBeaconEnterMsg);
        textViewBeaconExitMsg = (TextView) findViewById(R.id.textViewBeaconExitMsg);

        TextView textViewBeaconInitMsg = (TextView) findViewById(R.id.textViewBeaconInitMsg);
        textViewBeaconInitMsg.setText(textViewBeaconInitMsgStr);

        TextView deviceIMEIVal = (TextView) findViewById(R.id.textViewIMEINuber);
        deviceIMEIVal.setText(deviceIMEI);

        String beaconDeviceNameStr = "Beacon Device:       ";
        TextView textViewBeaconDeviceName = (TextView) findViewById(R.id.textViewBeaconDeviceName);
        textViewBeaconDeviceName.setText(beaconDeviceNameStr);

        textBeaconRangeMsg = (TextView) findViewById(R.id.textBeaconRangeMsg);
        textBeaconRangeMsg.setText("Beacon Range Status");

        String textBeaconInRageStr = "Waiting for Beacon Alert!";
        textBeaconInRage = (TextView) findViewById(R.id.textBeaconInRage);
        textBeaconInRage.setText(textBeaconInRageStr);

        textViewBeaconNotificationMsg = (TextView) findViewById(R.id.textViewBeaconNotificationMsg);

        if(!isDeviceInBeaconRange && !this.nativeAPKMode && false) {
            Date currentDateTime = new Date();
            Map<String, String> dataMap = new HashMap<String, String>();
            dataMap.put("beaconDeviceID", "BEACON_MOCA");
            dataMap.put("deviceIMEI", this.deviceIMEI);
            dataMap.put("InRange", "N");
            dataMap.put("eventRaisedDateTime", currentDateTime.toString());
            dataMap.put("mocaExprinceName", "APK_INIT");
            dataMap.put("mocaEvenDistance", "NA");
            dataMap.put("mocaEventName", "APK_INIT");

            String sendHttpRequestDataArry[] = {
                    crmEndPointURl,
                    createQueryStringForParameters(dataMap)
            };
            new SendHttpRequestTask().execute(sendHttpRequestDataArry);
        }

        // OnExit Handler Thread.
        handlerThread = new HandlerThread("MyBackThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        Log.i("exitCallRunnable", "OnExitpostDelayed");
        handler.postDelayed(exitCallRunnable,EXIT_TIME_MS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            unregisterReceiver(notificationReceiver);
        }
        if(notificationOnPerformCustomActionReceiver != null){
            unregisterReceiver(notificationOnPerformCustomActionReceiver);
        }
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

    private void updateUI(String message,String msgID) {

        if(msgID.equalsIgnoreCase(MESSAGE)) {
            if (textViewBeaconNotificationMsg != null) {
                Date currentDateTime = new Date();
                textViewBeaconNotificationMsg.setText(currentDateTime.toString() + "\n" + message);
                textBeaconInRage.setText("Beacon Notification Alert");
            }
        }
        else if(msgID.equalsIgnoreCase(CUSTOM_MESSAGE)) {
            if(textBeaconRangeMsg != null){
                Date currentDateTime = new Date();
                if("OnEnterPartnerBeacons".equalsIgnoreCase(message)){
                    textViewBeaconEnterMsg.setText("You entered into beacon range at "+currentDateTime.toString() +".");
                    textViewBeaconExitMsg.setText("");

                    if(!this.isDeviceInBeaconRange){
                        if(this.nativeAPKMode) {
                            Toast.makeText(getApplicationContext(), "Notify, CRM in Beacon Range.", Toast.LENGTH_LONG).show();
                        }else {
                            // Notify to CRM
                            Map<String, String> dataMap = new HashMap<String, String>();
                            dataMap.put("beaconDeviceID", "BEACON_MOCA");
                            dataMap.put("deviceIMEI", this.deviceIMEI);
                            dataMap.put("InRange", "Y");
                            dataMap.put("eventRaisedDateTime", currentDateTime.toString());
                            dataMap.put("mocaExprinceName", message);
                            dataMap.put("mocaEvenDistance", "NA");
                            dataMap.put("mocaEventName", message);

                            String sendHttpRequestDataArry[] = {
                                    crmEndPointURl,
                                    createQueryStringForParameters(dataMap)
                            };
                            new SendHttpRequestTask().execute(sendHttpRequestDataArry);

                        }
                    }
                    this.isDeviceInBeaconRange = true;
                } else if ("OnExitPartnerBeacons".equalsIgnoreCase(message)){
                    textViewBeaconExitMsg.setText("You exit from beacon range at "+currentDateTime.toString() +".");
                    if(this.isDeviceInBeaconRange){
                        if(this.nativeAPKMode) {
                            Toast.makeText(getApplicationContext(), "Notify, CRM exit from Beacon Range.", Toast.LENGTH_LONG).show();
                        }else {
                            // Notify to CRM
                            Map<String, String> dataMap = new HashMap<String, String>();
                            dataMap.put("beaconDeviceID", "BEACON_MOCA");
                            dataMap.put("deviceIMEI", this.deviceIMEI);
                            dataMap.put("InRange", "N");
                            dataMap.put("eventRaisedDateTime", currentDateTime.toString());
                            dataMap.put("mocaExprinceName", message);
                            dataMap.put("mocaEvenDistance", "NA");
                            dataMap.put("mocaEventName", message);

                            String sendHttpRequestDataArry[] = {
                                    crmEndPointURl,
                                    createQueryStringForParameters(dataMap)
                            };
                            new SendHttpRequestTask().execute(sendHttpRequestDataArry);
                        }
                    }
                    this.isDeviceInBeaconRange = false;
                }
             }
             finish();
        }
    }

    public class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(MESSAGE);
            updateUI(message,MESSAGE);
        }
    }

    public class NotificationOnPerformCustomActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(CUSTOM_MESSAGE);
            updateUI(message,CUSTOM_MESSAGE);
        }
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

    private long EXIT_TIME_MS = 45000;
    private Handler handler;
    private HandlerThread handlerThread;

    private Runnable exitCallRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i("exitCallRunnable", "Exit from beacon range!");
            finish();
        }
    };

}