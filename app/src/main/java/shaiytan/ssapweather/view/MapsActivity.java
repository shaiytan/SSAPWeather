package shaiytan.ssapweather.view;

import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.R;
import shaiytan.ssapweather.geocoding.GeocodingAPI;
import shaiytan.ssapweather.geocoding.Geopoint;

//Карта для выбора города или области для прогноза погоды
public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        Callback<Geopoint> {

    private Marker marker;
    private GeocodingAPI geocodingAPI;
    private Toolbar toolbar;
    private Geopoint point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Geopoint.class, new Geopoint.GeopointDeserializer())
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        geocodingAPI = retrofit.create(GeocodingAPI.class);
        toolbar = findViewById(R.id.tb);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setOnMapClickListener(this);
        LocationManager loc = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            loc.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, createPendingResult(1, new Intent(), 0));
            Location l = loc.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            LatLng here = new LatLng(l.getLatitude(), l.getLongitude());
            marker = googleMap.addMarker(new MarkerOptions().position(here).title("Выбери город"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 6));
            geocodingAPI.geocode(here.latitude + "," + here.longitude).enqueue(this);
        } catch (SecurityException ignored) {
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        marker.setPosition(latLng);
        geocodingAPI.geocode(latLng.latitude + "," + latLng.longitude).enqueue(this);
    }

    @Override
    public void onResponse(@NonNull Call<Geopoint> call, @NonNull Response<Geopoint> response) {
        point = response.body();
        String name = point.getLongName();
        marker.setTitle(name);
        toolbar.setTitle(name);
    }

    @Override
    public void onFailure(@NonNull Call<Geopoint> call, @NonNull Throwable t) {
    }

    public void onSubmit(View view) {
        Intent intent = new Intent();
        intent.putExtra("point", point);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
