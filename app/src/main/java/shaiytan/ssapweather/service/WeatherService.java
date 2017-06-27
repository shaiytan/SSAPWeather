package shaiytan.ssapweather.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.DBHelper;
import shaiytan.ssapweather.content.WeatherItem;
import shaiytan.ssapweather.geocoding.GeocodingAPI;
import shaiytan.ssapweather.geocoding.Geopoint;

public class WeatherService extends Service {
    public static final int UPDATE_PERIOD = 60 * 1000; //30min to update
    private static final double UNSET = Double.MAX_VALUE;
    private WeatherAPI weatherAPI;
    private DBHelper dbHelper;
    private NotificationManager nm;
    private double latitude;
    private double longitude;
    private String location;
    public static final String RESULT_ACTION = "shaiytan.ssapweather.service";
    private PendingIntent result;

    public WeatherService() {
    }

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
        dbHelper = new DBHelper(this);
        latitude=UNSET;
        longitude=UNSET;
        location="";
        updateTimer = new Timer();
        updateTimer.schedule(updateTask,UPDATE_PERIOD, UPDATE_PERIOD);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        result = intent.getParcelableExtra("result");
        if(intent.hasCategory("with_location")) {
            latitude = intent.getDoubleExtra("lat", UNSET);
            longitude = intent.getDoubleExtra("lon", UNSET);
            location = intent.getStringExtra("location");
        }
        else if(location.isEmpty()){
            LocationManager loc = (LocationManager) getSystemService(LOCATION_SERVICE);
            try {
                loc.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                        PendingIntent.getService(this,100,new Intent(),PendingIntent.FLAG_UPDATE_CURRENT));
                Location location = loc.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                latitude=location.getLatitude();
                longitude=location.getLongitude();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            location = intent.getStringExtra("location");
        }
        new Thread(loadTask).start();
        return START_STICKY;
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
    private final TimerTask updateTask = new TimerTask() {
        @Override
        public void run() {
            boolean success = loadWeather() && loadForecast();
            if (success)
            {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("select * from weather where _id=0", null);
                cursor.moveToFirst();
                WeatherItem res = new WeatherItem(
                        cursor.getString(cursor.getColumnIndex("description")),
                        cursor.getString(cursor.getColumnIndex("icon")),
                        cursor.getDouble(cursor.getColumnIndex("temperature")),
                        cursor.getDouble(cursor.getColumnIndex("humidity")),
                        cursor.getLong(cursor.getColumnIndex("datetime"))
                );
                cursor.close();
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle("Current Weather")
                        .setContentText(res.toString())
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .build();
                nm.notify(1,notification);
            }
        }
    };
    private Timer updateTimer;
    private final Runnable loadTask = new Runnable() {
        @Override
        public void run() {
            Intent intent=new Intent(RESULT_ACTION);
            if(location.isEmpty())
            {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Geopoint.class, new Geopoint.GeopointDeserializer())
                        .create();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("https://maps.googleapis.com/maps/api/")
                        .addConverterFactory(GsonConverterFactory.create(gson))
                        .build();
                GeocodingAPI geocodingAPI = retrofit.create(GeocodingAPI.class);
                try {
                    Response<Geopoint> response = geocodingAPI.geocode(latitude + "," + longitude).execute();
                    if(response.isSuccessful()) location=response.body().getLongName();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
}
