package shaiytan.ssapweather.view;

import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.*;
import android.view.View;
import android.widget.*;

import com.squareup.picasso.Picasso;

import java.util.*;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.*;
import shaiytan.ssapweather.service.WeatherService;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private ImageView icon;
    private TextView desc;
    private TextView temp;
    private TextView humid;
    private RecyclerView forecastView;
    private SwitchCompat forecastSwitch;
    private SwipeRefreshLayout swipeUpdater;
    private FloatingActionButton btnMap;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        Cursor forecast = dbHelper.readForecast(forecastSwitch.isChecked());
        if(forecast.getCount()>0) {
            WeatherItem currentWeather = dbHelper.readWeather();
            long lastUpdate = currentWeather.getDatetime()*1000;
            if(System.currentTimeMillis()-lastUpdate<30*60*1000) {
                setWeatherView(currentWeather);
                setForecastView(forecast);
            }
            else {
                Intent intent = new Intent(this, WeatherService.class);
                invokeService(intent);
            }
        }
        else {
            Intent intent = new Intent(this, WeatherService.class);
            invokeService(intent);
        }
    }
    private void init()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btnMap = (FloatingActionButton) findViewById(R.id.map_btn);
        icon = (ImageView) findViewById(R.id.ic_weather);
        desc = (TextView) findViewById(R.id.desc);
        temp = (TextView) findViewById(R.id.temp);
        humid = (TextView) findViewById(R.id.humid);
        forecastView = (RecyclerView) findViewById(R.id.rec_view);
        forecastView.setLayoutManager(
                new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        forecastView.setItemAnimator(new DefaultItemAnimator());
        forecastSwitch = (SwitchCompat) findViewById(R.id.switch1);
        forecastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Cursor forecast = dbHelper.readForecast(isChecked);
                setForecastView(forecast);
            }
        });
        swipeUpdater = (SwipeRefreshLayout) findViewById(R.id.swipe_updater);
        swipeUpdater.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Intent intent = new Intent(MainActivity.this, WeatherService.class);
                invokeService(intent);
                swipeUpdater.setRefreshing(true);
            }
        });
        dbHelper = new DBHelper(this);
    }
    private void invokeService(Intent intent)
    {
        PendingIntent result =
                createPendingResult(100, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("result",result);
        startService(intent);
    }
    private void setWeatherView(WeatherItem weather)
    {
        Picasso.with(this).load("http://openweathermap.org/img/w/" + weather.getImageID() +".png").into(icon);
        desc.setText(weather.getWeatherDescription());
        temp.setText(String.format("%+.1f C",weather.getTemperature()));
        humid.setText(String.format("Влажность: %.1f%%",weather.getHumidity()));
    }
    private void setForecastView(Cursor cursor)
    {
        ArrayList <WeatherItem> forecast=new ArrayList<>();
        while (cursor.moveToNext()){
            WeatherItem item = new WeatherItem(
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getString(cursor.getColumnIndex("icon")),
                    cursor.getDouble(cursor.getColumnIndex("temperature")),
                    cursor.getDouble(cursor.getColumnIndex("humidity")),
                    cursor.getLong(cursor.getColumnIndex("datetime")));
            forecast.add(item);
        }
        forecastView.setAdapter(new WeatherAdapter(this,forecast,forecastSwitch.isChecked()));
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(!data.getBooleanExtra("success",false)) {
            Toast.makeText(this, "Failed to download", Toast.LENGTH_SHORT).show();
        }
        WeatherItem weather = dbHelper.readWeather();
        setWeatherView(weather);
        Cursor forecast = dbHelper.readForecast(forecastSwitch.isChecked());
        setForecastView(forecast);
        swipeUpdater.setRefreshing(false);
    }

    public void onMapClick(View view) {
    }
}