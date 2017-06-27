package shaiytan.ssapweather.view;

import android.content.Intent;
import android.location.*;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.gson.*;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.R;
import shaiytan.ssapweather.geocoding.*;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        Callback<Geopoint> {

    private GoogleMap mMap;
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
        toolbar = (Toolbar) findViewById(R.id.tb);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        LocationManager loc= (LocationManager) getSystemService(LOCATION_SERVICE);
        try
        {
            loc.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,createPendingResult(1,new Intent(),0));
            Location l=loc.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            LatLng here = new LatLng(l.getLatitude(),l.getLongitude());
            marker = mMap.addMarker(new MarkerOptions().position(here).title("You Are Here"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here,6));
        }
        catch (SecurityException ignored){}
    }

    @Override
    public void onMapClick(LatLng latLng) {
        marker.setPosition(latLng);
        geocodingAPI.geocode(latLng.latitude+","+latLng.longitude).enqueue(this);
    }

    @Override
    public void onResponse(Call<Geopoint> call, Response<Geopoint> response) {
        point = response.body();
        String name = point.getLongName();
        marker.setTitle(name);
        toolbar.setTitle(name);
    }

    @Override
    public void onFailure(Call<Geopoint> call, Throwable t) {
    }

    public void onSubmit(View view) {
        Intent intent = new Intent();
        intent.putExtra("point",point);
        setResult(RESULT_OK,intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
