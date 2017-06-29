package shaiytan.ssapweather.service;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.IBinder;

import com.google.gson.*;

import java.io.IOException;
import java.util.*;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.*;
import shaiytan.ssapweather.geocoding.*;

//Сервис для получения прогноза погоды
public class WeatherService extends Service {
    private static final int UPDATE_PERIOD = 30 * 60 * 1000; //уведомления приходят каждые 30 минут
    private static final double UNSET = Double.MAX_VALUE;

    private WeatherAPI weatherAPI;
    private GeocodingAPI geocodingAPI;
    private DBHelper dbHelper;
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
            boolean success = loadWeather() && loadForecast();
            if (success)
            {
                WeatherItem res = dbHelper.readWeather();
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle(location+": "+res.getTemperature()+" C, "+res.getWeatherDescription())
                        .setContentText("Влажность: "+res.getHumidity())
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .build();
                nm.notify(1,notification);
            }
        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(WeatherItem.class,new WeatherItem.WeatherDeserializer())
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
        dbHelper = new DBHelper(this);
        latitude=UNSET;
        longitude=UNSET;
        location="";

        //запуск таймера для отсылки уведомлений
        updateTimer = new Timer();
        updateTimer.schedule(updateTask,UPDATE_PERIOD, UPDATE_PERIOD);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //PendingIntent вызывающей активити, через который отправляется результат
        result = intent.getParcelableExtra("result");

        //В параметрах передаются координаты города, для которого нужно загрузить погоду
        //Если параметры отсутствуют, обновляем погоду для последнего известного местоположения
        if(intent.hasCategory("with_location")) {
            latitude = intent.getDoubleExtra("lat", UNSET);
            longitude = intent.getDoubleExtra("lon", UNSET);
            location = intent.getStringExtra("location");
        }
        //для первого запуска сервиса определяется текущее местоположение
        else if(longitude==UNSET||latitude==UNSET){
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                        PendingIntent.getService(this,100,new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                latitude=loc.getLatitude();
                longitude=loc.getLongitude();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            location = intent.getStringExtra("location");
        }
        //загрузка данных в отдельном потоке
        new Thread(loadTask).start();

        return START_STICKY; //тут сервис должен бы стать неубиваемым, но почему-то это не так((
    }
    private final Runnable loadTask = new Runnable() {
        @Override
        public void run() {
            Intent intent=new Intent();
            Response<Geopoint> response;
            try {
                response = geocodingAPI.geocode(latitude + "," + longitude).execute();
                if(response.isSuccessful()) location=response.body().getLongName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            boolean success = loadWeather() && loadForecast();
            intent.putExtra("success",success);
            try {
                result.send(WeatherService.this,100,intent);
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    };
    synchronized private boolean loadWeather() {
        try {
            Response<WeatherItem> response = weatherAPI.getCurrentWeather(latitude, longitude).execute();
            WeatherItem weather = response.body();
            dbHelper.writeCurrentWeather(weather,location);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    synchronized private boolean loadForecast(){
        try {
            Response<WeatherItem[]> response = weatherAPI.getForecast(latitude, longitude).execute();
            WeatherItem[] forecast = response.body();
            dbHelper.writeForecast(forecast,location);
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
