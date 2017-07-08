package de.digineo.wifitools;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {


    private static final String TAG = "WifiTools";

    private static long gpsMinTime = 1000; // milliseconds
    private static float gpsMinDistance = 1; // meters


    private final String[] permissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
    };


    WifiManager wifiManager;
    WifiBroadcastReceiver broadcastReceiver;
    ArrayAdapter<String> wifiListAdapter;

    // GPS stuff
    LocationManager locationManager;
    double longitudeGPS, latitudeGPS;
    TextView longitudeValueGPS, latitudeValueGPS;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }

        // GPS stuff
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        longitudeValueGPS = (TextView) findViewById(R.id.longitudeValueGPS);
        latitudeValueGPS = (TextView) findViewById(R.id.latitudeValueGPS);

        // Wifi list view
        wifiListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        ListView listView = (ListView) findViewById(R.id.wifiList);
        listView.setAdapter(wifiListAdapter);

        // Enable wifi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        // Set wifi receiver for scans
        broadcastReceiver = new WifiBroadcastReceiver();
        registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }


    public class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "wifi broadcast received");
            WifiManager wifiManager = ((MainActivity) context).getCurrentWifiManager();

            List<ScanResult> results = wifiManager.getScanResults();
            wifiListAdapter.clear();

            for(ScanResult s : results){
                if (s.capabilities.equals("[ESS]")) {
                    // unprotected wifi
                    Log.i(TAG, s.toString());
                    wifiListAdapter.add(s.toString());
                }
            }

        }
    }


    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean checkLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    public void scanNow(View view) {
        Log.i(TAG, "button clicked");
        wifiManager.startScan();
    }

    public void toggleGPSUpdates(View view) {
        if(!checkLocation())
            return;

        String pauseText = getResources().getString(R.string.gps_pause);
        String resumeText = getResources().getString(R.string.gps_resume);

        Button button = (Button) view;
        if(button.getText().equals(pauseText)) {
            locationManager.removeUpdates(locationListenerGPS);
            button.setText(resumeText);
            Log.i(TAG, "GPS stopped");
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsMinTime, gpsMinDistance, locationListenerGPS);
            button.setText(pauseText);
            Log.i(TAG, "GPS started");
        }
    }



    private final LocationListener locationListenerGPS = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeGPS = location.getLongitude();
            latitudeGPS = location.getLatitude();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    longitudeValueGPS.setText(longitudeGPS + "");
                    latitudeValueGPS.setText(latitudeGPS + "");
                    Toast.makeText(MainActivity.this, "GPS Provider update", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
        }

        @Override
        public void onProviderEnabled(String s) {
        }

        @Override
        public void onProviderDisabled(String s) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "permission granted");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.i(TAG, "permission denied");
                }
                break;
            }
            default:
                Log.i(TAG, "unknown permission request code: " + requestCode);
                // other 'case' lines to check for other
                // permissions this app might request
        }
    }

    public WifiManager getCurrentWifiManager() {
        return wifiManager;
    }

}
