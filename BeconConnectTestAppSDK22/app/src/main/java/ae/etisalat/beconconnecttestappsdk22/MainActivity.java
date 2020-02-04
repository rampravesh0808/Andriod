package ae.etisalat.beconconnecttestappsdk22;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements BeaconConsumer{

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    protected static final String TAG = "MonitoringBeacon";
    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Toast.makeText(this, "inside ver", Toast.LENGTH_LONG).show();
        }
        else{
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
           // Toast.makeText(this, permissionCheck+"", Toast.LENGTH_LONG).show();
            if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
                // ask permissions here using below code
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }


        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);

        BluetoothAdapter BA = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner mBluetoothLeScanner = BA.getBluetoothLeScanner();

        setScanFilter();
        setScanSettings();

        List<ScanFilter> listScanFilter = new ArrayList<ScanFilter>();
        listScanFilter.add(this.mScanFilter);
        try {
           // mBluetoothLeScanner.
            Toast.makeText(this, "startScan", Toast.LENGTH_LONG).show();
            mBluetoothLeScanner.startScan(listScanFilter, mScanSettings, mScanCallback);
           // mBluetoothLeScanner.startScan(mScanCallback);
            Toast.makeText(this, "scanning in progress", Toast.LENGTH_LONG).show();

            mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
        }
        catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }


       // mBluetoothLeScanner.
    }


    private ScanFilter mScanFilter;

    private ScanSettings mScanSettings;

    protected ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Toast.makeText(getApplicationContext(), "onScanResult Device Found!", Toast.LENGTH_LONG).show();
            if(result != null) {
                ScanRecord mScanRecord = result.getScanRecord();
                if (mScanRecord != null) {
                    byte[] manufacturerData = mScanRecord.getManufacturerSpecificData(224);
                    int mRssi = result.getRssi();

                    Toast.makeText(getApplicationContext(), mRssi, Toast.LENGTH_LONG).show();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Toast.makeText(getApplicationContext(), "BLE// onBatchScanResults!", Toast.LENGTH_LONG).show();
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            System.out.println("BLE// onScanFailed");
            Toast.makeText(getApplicationContext(), "BLE// onScanFailed!", Toast.LENGTH_LONG).show();
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private void setScanFilter() {
        try {

            ScanFilter.Builder mBuilder = new ScanFilter.Builder();
            ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
            ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);
            byte[] uuid = getIdAsByte(UUID.fromString("0CF052C297CA407C84F8B62AAC4E9020"));
            mManufacturerData.put(0, (byte) 0xBE);
            mManufacturerData.put(1, (byte) 0xAC);
            for (int i = 2; i <= 17; i++) {
                mManufacturerData.put(i, uuid[i - 2]);
            }
            for (int i = 0; i <= 17; i++) {
                mManufacturerDataMask.put((byte) 0x01);
            }
            mBuilder.setManufacturerData(224, mManufacturerData.array(), mManufacturerDataMask.array());
            this.mScanFilter = mBuilder.build();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setScanSettings() {
        ScanSettings.Builder mBuilder = new ScanSettings.Builder();
        mBuilder.setReportDelay(0);
        mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        //mBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        this.mScanSettings = mBuilder.build();
    }

    public static byte[] getIdAsByte(java.util.UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
                Toast.makeText(getApplicationContext(), "I just saw an beacon for the first time!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
                Toast.makeText(getApplicationContext(), "I no longer see an beacon!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
                Toast.makeText(getApplicationContext(), "I have just switched from seeing/not seeing beacons!", Toast.LENGTH_LONG).show();
            }
        });

        try {
            Toast.makeText(getApplicationContext(), "startMonitoringBeaconsInRegion starting!", Toast.LENGTH_LONG).show();
            beaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
            Log.i(TAG, "startMonitoringBeaconsInRegion started!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
    }
}
