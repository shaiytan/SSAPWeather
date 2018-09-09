package shaiytan.ssapweather.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import shaiytan.ssapweather.api.GeocodingAPI;
import shaiytan.ssapweather.model.Geopoint;

public class LocationViewModel extends AndroidViewModel implements Callback<Geopoint> {
    private SharedPreferences preferences;
    private GeocodingAPI api;
    private MutableLiveData<Geopoint> location;

    public LocationViewModel(@NonNull Application application) {
        super(application);
        location = new MutableLiveData<>();
        preferences = application.getSharedPreferences("location", Context.MODE_PRIVATE);
        api = GeocodingAPI.create();
        if (preferences.contains("city")) {
            String name = preferences.getString("city", "unknown location");
            double lat = preferences.getFloat("lat", 0.0F);
            double lng = preferences.getFloat("lng", 0.0F);
            location.setValue(new Geopoint(name, lat, lng));
        } else {
            LocationManager locationManager = (LocationManager) application.getSystemService(Context.LOCATION_SERVICE);
            try {
                if (locationManager == null) return;
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc == null) return;
                double lat = loc.getLatitude();
                double lng = loc.getLongitude();
                updateLocation(lat, lng);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    public LiveData<Geopoint> getLocation() {
        return location;
    }

    public void updateLocation(double lat, double lng) {
        api.geocode(lat + "," + lng).enqueue(this);
    }

    public void saveLocation() {
        Geopoint point = location.getValue();
        if (point == null) return;
        preferences.edit()
                .putString("city", point.getLongName())
                .putFloat("lat", (float) point.getLatitude())
                .putFloat("lng", (float) point.getLongitude())
                .apply();
    }

    @Override
    public void onResponse(@NonNull Call<Geopoint> call, @NonNull Response<Geopoint> response) {
        if (!response.isSuccessful()) return;
        Geopoint point = response.body();
        if (point == null) return;
        location.setValue(point);
    }

    @Override
    public void onFailure(@NonNull Call<Geopoint> call, @NonNull Throwable t) {
        t.printStackTrace();
    }
}
