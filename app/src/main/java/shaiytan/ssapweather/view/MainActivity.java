package shaiytan.ssapweather.view;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
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

import java.util.ArrayList;
import java.util.Locale;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.DBHelper;
import shaiytan.ssapweather.content.WeatherItem;
import shaiytan.ssapweather.geocoding.Geopoint;
import shaiytan.ssapweather.service.WeatherService;

public class MainActivity extends AppCompatActivity {
    public static final int MAP_REQUEST = 1;
    public static final int WEATHER_REQUEST = 2;

    private DBHelper dbHelper;
    private ImageView icon;
    private TextView desc;
    private TextView temp;
    private TextView humid;
    private RecyclerView forecastView;
    private SwitchCompat forecastSwitch;
    private SwipeRefreshLayout swipeUpdater;
    private String location = "";
    private TextView loc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //инициалицация полей связанных с интерфейсом
        init();

        dbHelper = new DBHelper(this);
        location = dbHelper.getLocation();
        Cursor forecast = dbHelper.readForecast(forecastSwitch.isChecked());

        //при отсутствии актуальных данных о погоде, вызывается сервис
        if (forecast.getCount() > 0) {
            WeatherItem currentWeather = dbHelper.readWeather();
            long lastUpdate = currentWeather.getDatetime() * 1000;
            if (System.currentTimeMillis() - lastUpdate < 30 * 60 * 1000) {
                setWeatherView(currentWeather);
                setForecastView(forecast);
            } else {
                Intent intent = new Intent(this, WeatherService.class);
                invokeService(intent);
            }
        } else {
            Intent intent = new Intent(this, WeatherService.class);
            invokeService(intent);
        }
    }

    private void init() {
        //строка живого поиска
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setTitle(R.string.app_name);
        initLiveSearch();

        //Карточка текущей погоды
        icon = findViewById(R.id.ic_weather);
        desc = findViewById(R.id.desc);
        temp = findViewById(R.id.temp);
        humid = findViewById(R.id.humid);
        loc = findViewById(R.id.loc);

        //Список карточек с прогнозом
        forecastView = findViewById(R.id.rec_view);
        forecastView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        forecastView.setItemAnimator(new DefaultItemAnimator());
        forecastSwitch = findViewById(R.id.switch1);
        forecastSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Cursor forecast = dbHelper.readForecast(isChecked);
            setForecastView(forecast);
        });

        //pull to update
        swipeUpdater = findViewById(R.id.swipe_updater);
        swipeUpdater.setOnRefreshListener(() -> {
            Intent intent = new Intent(MainActivity.this, WeatherService.class);
            invokeService(intent);
            swipeUpdater.setRefreshing(true);
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
                location = place.getName().toString();
                Intent intent = new Intent(MainActivity.this, WeatherService.class);
                intent.addCategory("with_location")
                        .putExtra("lat", place.getLatLng().latitude)
                        .putExtra("lon", place.getLatLng().longitude);
                invokeService(intent);
            }

            @Override
            public void onError(Status status) {
                Toast.makeText(MainActivity.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //вызов сервиса с заданными параметрами
    private void invokeService(Intent intent) {
        PendingIntent result =
                createPendingResult(WEATHER_REQUEST, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("location", location);
        intent.putExtra("result", result);
        startService(intent);
    }

    //вывод текущей погоды на экран
    private void setWeatherView(WeatherItem weather) {
        Picasso.with(this).load("http://openweathermap.org/img/w/" + weather.getImageID() + ".png").into(icon);
        desc.setText(weather.getWeatherDescription());
        Locale locale = Locale.getDefault();
        temp.setText(String.format(locale, "%+.1f C", weather.getTemperature()));
        humid.setText(String.format(locale, "Влажность: %.1f%%", weather.getHumidity()));
        loc.setText(location);
    }

    //вывод прогноза погоды
    private void setForecastView(Cursor cursor) {
        ArrayList<WeatherItem> forecast = new ArrayList<>();
        while (cursor.moveToNext()) {
            WeatherItem item = new WeatherItem(
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getString(cursor.getColumnIndex("icon")),
                    cursor.getDouble(cursor.getColumnIndex("temperature")),
                    cursor.getDouble(cursor.getColumnIndex("humidity")),
                    cursor.getLong(cursor.getColumnIndex("datetime")));
            forecast.add(item);
        }
        forecastView.setAdapter(new WeatherAdapter(this, forecast, forecastSwitch.isChecked()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //получение результатов с карты, от сервиса и данные аутентификации от гугла и фейсбука соответственно
        switch (requestCode) {
            case MAP_REQUEST:
                if (resultCode == RESULT_OK) {
                    Geopoint point = (Geopoint) data.getSerializableExtra("point");
                    location = point.getLongName();
                    Intent intent = new Intent(MainActivity.this, WeatherService.class);
                    intent.addCategory("with_location")
                            .putExtra("lat", point.getLatitude())
                            .putExtra("lon", point.getLongitude());
                    invokeService(intent);

                }
                break;
            case WEATHER_REQUEST:
                if (!data.getBooleanExtra("success", false)) {
                    Toast.makeText(this, "Failed to download", Toast.LENGTH_SHORT).show();
                }
                location = dbHelper.getLocation();
                WeatherItem weather = dbHelper.readWeather();
                setWeatherView(weather);
                Cursor forecast = dbHelper.readForecast(forecastSwitch.isChecked());
                setForecastView(forecast);
                swipeUpdater.setRefreshing(false);
                break;
        }

    }

    //вызов карты
    public void onMapClick(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, MAP_REQUEST);
    }
}