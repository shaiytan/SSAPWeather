package shaiytan.ssapweather.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.R;
import shaiytan.ssapweather.db.WeatherDAO;
import shaiytan.ssapweather.db.WeatherDatabase;
import shaiytan.ssapweather.geocoding.GeocodingAPI;
import shaiytan.ssapweather.geocoding.Geopoint;
import shaiytan.ssapweather.model.WeatherItem;

//Сервис для получения прогноза погоды
public class WeatherService extends Service {
    private static final int UPDATE_PERIOD = 30 * 60 * 1000; //уведомления приходят каждые 30 минут
    private static final double UNSET = Double.MAX_VALUE;

    private WeatherAPI weatherAPI;
    private GeocodingAPI geocodingAPI;
    private WeatherDAO dao;
    private double latitude;
    private double longitude;
    private NotificationManager nm;
    private String location;
    private PendingIntent result;

    //отправка уведомлений по таймеру
    // TODO: Разобраться с AlarmManager и переписать этот сервис как IntentService
    private Timer updateTimer;
    private final TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            boolean success = updateForecast();
            if (success) {
                WeatherItem res = dao.getCurrentWeather();
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle(location + ": " + res.getTemperature() + " C, " + res.getWeatherDescription())
                        .setContentText("Влажность: " + res.getHumidity())
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .build();
                nm.notify(1, notification);
            }
        }
    };
    private final Runnable loadTask = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent();
            try {
                Response<Geopoint> response = geocodingAPI.geocode(latitude + "," + longitude).execute();
                if (response.isSuccessful()) location = response.body().getLongName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            boolean success = updateForecast();
            intent.putExtra("success", success);
            try {
                result.send(WeatherService.this, 100, intent);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(WeatherItem.class, new WeatherItem.WeatherDeserializer())
                .registerTypeAdapter(WeatherItem[].class, new WeatherItem.ForecastDeserializer())
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        weatherAPI = retrofit.create(WeatherAPI.class);
        gson = new GsonBuilder()
                .registerTypeAdapter(Geopoint.class, new Geopoint.GeopointDeserializer())
                .create();
        retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        geocodingAPI = retrofit.create(GeocodingAPI.class);
        dao = WeatherDatabase.getInstanse(this).getWeatherDAO();
        latitude = UNSET;
        longitude = UNSET;
        location = "";

        //запуск таймера для отсылки уведомлений
        updateTimer = new Timer();
        updateTimer.schedule(updateTask, UPDATE_PERIOD, UPDATE_PERIOD);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //PendingIntent вызывающей активити, через который отправляется результат
        result = intent.getParcelableExtra("result");

        //В параметрах передаются координаты города, для которого нужно загрузить погоду
        //Если параметры отсутствуют, обновляем погоду для последнего известного местоположения
        if (intent.hasCategory("with_location")) {
            latitude = intent.getDoubleExtra("lat", UNSET);
            longitude = intent.getDoubleExtra("lon", UNSET);
            location = intent.getStringExtra("location");
        }
        //для первого запуска сервиса определяется текущее местоположение
        else if (longitude == UNSET || latitude == UNSET) {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                        PendingIntent.getService(this, 100, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT));
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            location = intent.getStringExtra("location");
        }
        //загрузка данных в отдельном потоке
        new Thread(loadTask).start();

        return START_STICKY; //тут сервис должен бы стать неубиваемым, но почему-то это не так((
    }

    synchronized private boolean updateForecast() {
        try {
            Response<WeatherItem> weatherResponse = weatherAPI.getCurrentWeather(latitude, longitude).execute();
            WeatherItem weather = weatherResponse.body();
            if (weather == null) return false;
            weather.setLocation(location);
            weather.setId(0L);
            Response<WeatherItem[]> response = weatherAPI.getForecast(latitude, longitude).execute();
            WeatherItem[] forecast = response.body();
            if (forecast == null) return false;
            for (WeatherItem item : forecast) {
                item.setLocation(location);
            }
            dao.updateForecast(weather, Arrays.asList(forecast));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updateTimer.cancel();
    }

}
