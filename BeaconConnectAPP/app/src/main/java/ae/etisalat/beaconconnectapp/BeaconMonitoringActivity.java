package ae.etisalat.beaconconnectapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;


public class BeaconMonitoringActivity extends AppCompatActivity implements BeaconConsumer {

    private BeaconManager beaconManager;
    protected static final String TAG = "MonitoringBeacon";


    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_monitoring);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Toast.makeText(this, "inside ver", Toast.LENGTH_LONG).show();
        }

        beaconManager = BeaconManager.getInstanceForApplication(this);
        Log.i(TAG, "beaconManager stated!");

        beaconManager.bind(this);
        Log.i(TAG, "beaconManager bind!");
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