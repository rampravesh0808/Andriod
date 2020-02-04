package ae.etisalat.crmportalbluetoothapp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BeconBluetoothReceiver extends BroadcastReceiver {

    public String deviceName;
    public String deviceHardwareAddress;

    public String getDeviceName(){
        return deviceName;
    }
    public String getAddress(){
        return deviceHardwareAddress;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                this.deviceName = deviceName;
                this.deviceHardwareAddress = deviceHardwareAddress;

                String deviceDtls = "Discovery has found a device, deviceName: "+deviceName + ", MAC: "+deviceHardwareAddress;
                Toast.makeText(context, deviceDtls, Toast.LENGTH_LONG).show();

            }
        }
    }
}
