package ae.etisalat.myfirstapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NotificationCompat.getExtras
import android.content.Intent
import android.util.Log


class MainActivity : AppCompatActivity() {

    private val LAUNCH_FROM_URL = "etisalat.crm_portal.bluetooh"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        val intent = intent
        if (intent != null && intent.action == LAUNCH_FROM_URL) {
            val bundle = intent.extras
            if (bundle != null) {
                val msgFromBrowserUrl = bundle.getString("msg_from_browser")
               // launchInfo.setText(msgFromBrowserUrl)

                Log.d("AndroidSRC", msgFromBrowserUrl);
            }
        } else {
            Log.d("AndroidSRC", "Normal Application Launch");
        }

    }
}
