package ae.etisalat.crmportalbluetoothapp;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// AppCompatActivity
public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private BroadcastReceiver blueReceiver;
    //Switch bluetoothSwitch;
    BluetoothAdapter BA;
    String deviceNameFound;

    private boolean isNataveLaunch = false;
    private Map<String, String> crmDataMap;
    private String crmEndPointURl = "";
    private String beaconDeviceID;
    private long waitTime = 25;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
       // setTheme(android.R.style.Theme_NoDisplay);


        pd = new ProgressDialog(this);
        pd.setTitle("Please wait....");
        pd.setMessage("Verifying your POS location.");
        pd.setCancelable(false);
        pd.show();

        //crmEndPointURl = "https://salesportal.etisalat4retailer.ae/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices";
        crmDataMap = new HashMap<String, String>();
        if(!isNataveLaunch) {
            //crmEndPointURl = "https://salesportal.etisalat4retailer.ae/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices";
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                Log.i("CRMSignIntent", action);
                Uri data = intent.getData();
                if (data != null) {
                    // data.getQuery() -> redirecturl=http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices&user_id=CSSQA17&waitTime=10&beaconID=9797hjh88&deviceIMEI=56456465465464
                    // data.getQueryParameterNames().toString() -> [redirectURL, user_id, waitTime, deviceIMEI,beaconID]
                    // data.getQueryParameter("redirecturl")  -->>  http://195.229.186.224:4443/SalesPortal/MobileAppResponseReceiverServlet?AUTH_ID=BluetoothDevices

                    this.crmEndPointURl = data.getQueryParameter("redirectURL");
                    Set<String> queryParameters = data.getQueryParameterNames();
                    //Toast.makeText(this, "crmEndPointURl - "+ this.crmEndPointURl, Toast.LENGTH_LONG).show();
                    for (String s : queryParameters) {
                        System.out.println(s);
                        if (s.equalsIgnoreCase("redirectURL")) {
                            continue;
                        }
                        if (s.equalsIgnoreCase("beaconID")) {
                            this.beaconDeviceID = data.getQueryParameter("beaconID");
                        }
                        //Toast.makeText(this, "CRM Beacon ID "+ this.beaconDeviceID, Toast.LENGTH_LONG).show();
                        try {
                            if (s.equalsIgnoreCase("waitTime")) {
                                this.waitTime = Long.parseLong(data.getQueryParameter("waitTime"));
                            }
                        }catch (Exception e){e.printStackTrace();}

                        crmDataMap.put(s, data.getQueryParameter(s));
                    }
                }
            }
        }

        BA = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (BA == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            pd.dismiss();
            finish();
            return;
        }

        if (!BA.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
           // Toast.makeText(this, "Bluetooth is enabled!", Toast.LENGTH_LONG).show();
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},0
                   );


        }
       //BluetoothLeAdvertiser mBluetoothLeScanner = BA.getBluetoothLeAdvertiser();

        //ListView  visibleDevices = (ListView)findViewById(R.id.listView);
       // bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        //bluetoothSwitch.setOnCheckedChangeListener(this);
       // if(BA.isEnabled()){
          // bluetoothSwitch.setChecked(true);
         //  BA.startDiscovery();
       // }

        final ArrayList<String> devices = new ArrayList<>();
       // final ArrayAdapter<String> theAdapter = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_list_item_1, devices);
       // visibleDevices.setAdapter(theAdapter);
        blueReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_ON) {
                        BA.startDiscovery();
                        //bluetoothSwitch.setChecked(true);
                        // Toast.makeText(context, "Scanning new devices.", Toast.LENGTH_LONG).show();
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        devices.clear();
                       // theAdapter.notifyDataSetChanged();
                       // bluetoothSwitch.setChecked(false);
                        // Toast.makeText(context, "Bluetooth is OFF", Toast.LENGTH_LONG).show();
                    }
                }
                else if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                    Boolean repeated = false;
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceInfo = "";

                    if (device != null && device.getName() != null && device.getAddress() != null) {
                        deviceInfo = device.getName() + "\n" + device.getAddress();

                    }


                    if(isNataveLaunch) {
                        if (device.getAddress() != null && !"".equalsIgnoreCase(device.getAddress())) {
                            if (!devices.contains(device.getAddress())) {
                                devices.add(device.getAddress());
                                Toast.makeText(context, device.getAddress(), Toast.LENGTH_LONG).show();
                                // theAdapter.notifyDataSetChanged();
                                deviceNameFound = device.getAddress();
                            }
                        }
                    }else{

                        if(device.getName() != null && "KFAF_ETSLT_OFC2".equalsIgnoreCase(device.getName())){
                            Toast.makeText(context, "MAC Address of KFAF_ETSLT_OFC2 is "+device.getAddress(), Toast.LENGTH_LONG).show();
                        }
                        if (device.getAddress() != null && !"".equalsIgnoreCase(device.getAddress()) && beaconDeviceID.equalsIgnoreCase(device.getAddress()) ) {
                            if (!devices.contains(device.getAddress())) {
                                devices.add(device.getAddress());
                                Toast.makeText(context, "Beacon Found "+device.getAddress(), Toast.LENGTH_LONG).show();
                                // theAdapter.notifyDataSetChanged();
                                deviceNameFound = device.getAddress();

                                /*
                                ParcelUuid prceluuid[] = device.getUuids();
                                for(ParcelUuid uuid: prceluuid){
                                    UUID uuidObj = uuid.getUuid();
                                    uuidObj.
                                }
                                */
                                handler.removeCallbacksAndMessages(null);
                                prepareHttpPostReq("BluetoohBeaconInRange","Y");

                                pd.dismiss();
                                finish();
                            }
                        }
                    }
/*
                    if(isNataveLaunch) {
                        if (!repeated && device.getName() != null && !"".equalsIgnoreCase(device.getName())) {
                            if (!devices.contains(device.getName())) {
                                devices.add(device.getName());
                                Toast.makeText(context, device.getName(), Toast.LENGTH_LONG).show();
                               // theAdapter.notifyDataSetChanged();
                                deviceNameFound = device.getName();
                            }
                        }
                    }else{
                        if (!repeated && device.getName() != null && !"".equalsIgnoreCase(device.getName()) && beaconDeviceID.equalsIgnoreCase(device.getName()) ) {
                            if (!devices.contains(device.getName())) {
                                devices.add(device.getName());
                                Toast.makeText(context, "Beacon Found "+device.getName(), Toast.LENGTH_LONG).show();
                               // theAdapter.notifyDataSetChanged();
                                deviceNameFound = device.getName();

                                *//*
                                ParcelUuid prceluuid[] = device.getUuids();
                                for(ParcelUuid uuid: prceluuid){
                                    UUID uuidObj = uuid.getUuid();
                                    uuidObj.
                                }
                                *//*

                                prepareHttpPostReq("BluetoohBeaconInRange","Y");

                                pd.dismiss();
                                finish();
                            }
                        }
                    }
*/

                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                    devices.clear();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(blueReceiver, filter);

        if(BA.isEnabled()){
            // bluetoothSwitch.setChecked(true);
            BA.startDiscovery();
        }

        // OnExit Handler Thread.
        handlerThread = new HandlerThread("MyBackThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        Log.i("exitCallRunnable", "OnEnterpostDelayed");
        EXIT_TIME_MS = waitTime * 1000;
        handler.postDelayed(exitCallRunnable, EXIT_TIME_MS);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            BA.enable();
        } else {
            BA.disable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(blueReceiver);
        Log.i("unregisterReceiver", "Un-Register Receiver");
        //Toast.makeText(this, "Un-Register Receiver", Toast.LENGTH_LONG).show();
    }


    private long EXIT_TIME_MS = 30;
    private Handler handler;
    private HandlerThread handlerThread;

    private Runnable exitCallRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i("exitCallRunnable", "Exit from beacon range, No Beacon Event!");
            // Notify to CRM
            if(!isNataveLaunch)
                prepareHttpPostReq("APK_INIT","N");

            pd.dismiss();
            finish();
        }
    };

    private void prepareHttpPostReq(String msg,String inRange)
    {
        Date currentDateTime = new Date();
        // Map<String, String> dataMap = new HashMap<String, String>();
        crmDataMap.put("beaconDeviceID", beaconDeviceID);
        //dataMap.put("deviceIMEI", deviceIMEI);
        crmDataMap.put("InRange", inRange);
        crmDataMap.put("eventRaisedDateTime", currentDateTime.toString());
        crmDataMap.put("mocaExprinceName", "NA");
        crmDataMap.put("mocaEvenDistance", "NA");
        crmDataMap.put("mocaEventName", "NA");

        String sendHttpRequestDataArry[] = {
                crmEndPointURl,
                createQueryStringForParameters(crmDataMap)
        };
        new MainActivity.SendHttpRequestTask().execute(sendHttpRequestDataArry);
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
