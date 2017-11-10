package appinsightor.com.sdk_appinsightor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

/**
 * Created by yoonsh on 17. 10. 19.
 */

public class GPSTracker implements LocationListener{

    private Context mContext;

    private Location mLocation;
    private LocationManager mLocationManager;

    private boolean mGPSEnabled;
    private boolean mNetworkEnabled;

    private double latitude;
    private double longitude;

    public GPSTracker(Context context) {
        this.mContext = context;
        this.mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.mGPSEnabled = this.mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        this.mNetworkEnabled = this.mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public Location getLocation() {
        if (ContextCompat.checkSelfPermission( this.mContext, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {

            if (!this.mGPSEnabled && !this.mNetworkEnabled) {

            } else {
                if (this.mNetworkEnabled) {
                    this.mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                    if (this.mLocationManager != null) {
                        this.mLocation = this.mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (this.mLocation != null) {
                            this.latitude = this.mLocation.getLatitude();
                            this.longitude = this.mLocation.getLongitude();
                        }
                    }
                }

                if (this.mGPSEnabled) {
                    if (this.mLocation == null) {
                        this.mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                        if (this.mLocationManager != null) {
                            this.mLocation = this.mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (this.mLocation != null) {
                                this.latitude = this.mLocation.getLatitude();
                                this.longitude = this.mLocation.getLongitude();
                            }
                        }
                    }
                }
            }
        }
        return this.mLocation;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "latitude : " + location.getLatitude() + " longitude : " + location.getLongitude());
        this.mLocationManager.removeUpdates(this);
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.d("onStatusChanged", "2");
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d("onProviderEnabled", "2");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d("onProviderDisabled", "2");
    }

}
