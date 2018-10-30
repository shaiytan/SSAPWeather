package shaiytan.ssapweather.view;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Locale;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.model.Geopoint;
import shaiytan.ssapweather.model.WeatherItem;
import shaiytan.ssapweather.notification.NotificationAlarmReceiver;
import shaiytan.ssapweather.viewmodel.LocationViewModel;
import shaiytan.ssapweather.viewmodel.WeatherViewModel;

public class MainActivity extends AppCompatActivity {
    public static final int MAP_REQUEST = 1;

    private ImageView icon;
    private TextView desc;
    private TextView temp;
    private TextView humid;
    private RecyclerView forecastView;
    private SwipeRefreshLayout swipeUpdater;
    private TextView loc;
    private WeatherViewModel weatherModel;
    private LocationViewModel locationModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherModel = ViewModelProviders.of(this).get(WeatherViewModel.class);
        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        //инициалицация полей связанных с интерфейсом
        init();

        locationModel.getLocation().observe(this, point -> {
            if (point == null) return;
            locationModel.saveLocation();
            weatherModel.loadForecast(point, false);
        });
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        weatherModel.getForecastData().observe(this, forecast -> {
            if (forecast == null) return;
            setForecastView(forecast);
            for (int i = 0; i < forecast.size(); i++) {
                WeatherItem item = forecast.get(i);
                PendingIntent pendingIntent =
                        NotificationAlarmReceiver.createPendingIntent(this, i, item);
                am.set(AlarmManager.RTC, item.getDatetime(), pendingIntent);
            }
        });
        weatherModel.getCurrentWeatherData().observe(this, currentWeather -> {
            if (currentWeather == null) return;
            setWeatherView(currentWeather);
        });
        weatherModel.getLoadingStatus().observe(this, refreshing -> {
            if (refreshing == null) {
                Toast.makeText(this, "Loading Error", Toast.LENGTH_SHORT).show();
                swipeUpdater.setRefreshing(false);
            } else swipeUpdater.setRefreshing(refreshing);
        });
    }

    private void init() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setTitle(R.string.app_name);
        initLiveSearch();

        icon = findViewById(R.id.ic_weather);
        desc = findViewById(R.id.desc);
        temp = findViewById(R.id.temp);
        humid = findViewById(R.id.humid);
        loc = findViewById(R.id.loc);

        forecastView = findViewById(R.id.rec_view);
        forecastView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        forecastView.setItemAnimator(new DefaultItemAnimator());

        swipeUpdater = findViewById(R.id.swipe_updater);
        swipeUpdater.setOnRefreshListener(() -> {
            Geopoint point = locationModel.getLocation().getValue();
            if (point == null) return;
            weatherModel.loadForecast(point, true);
        });
    }

    private void initLiveSearch() {
        PlaceAutocompleteFragment placePicker = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_pick);
        placePicker.setFilter(new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .build());
        placePicker.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                locationModel.updateLocation(place.getLatLng().latitude, place.getLatLng().longitude);
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(MainActivity.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setWeatherView(WeatherItem weather) {
        Picasso.with(this)
                .load("http://openweathermap.org/img/w/" + weather.getImageID() + ".png")
                .into(icon);
        desc.setText(weather.getWeatherDescription());
        Locale locale = Locale.getDefault();
        temp.setText(String.format(locale, "%+.1f C", weather.getTemperature()));
        humid.setText(String.format(locale, "Влажность: %.1f%%", weather.getHumidity()));
        loc.setText(weather.getLocation());
    }

    private void setForecastView(List<WeatherItem> forecast) {
        forecastView.setAdapter(new WeatherAdapter(this, forecast));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MAP_REQUEST && resultCode == RESULT_OK) {
            Geopoint point = (Geopoint) data.getSerializableExtra("point");
            locationModel.updateLocation(point.getLatitude(), point.getLongitude());
        }
    }

    public void onMapClick(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, MAP_REQUEST);
    }
}