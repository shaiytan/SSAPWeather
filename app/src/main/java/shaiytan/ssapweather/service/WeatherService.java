package shaiytan.ssapweather.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.DBHelper;
import shaiytan.ssapweather.content.WeatherItem;

public class WeatherService extends Service {
    public static final int UPDATE_PERIOD = 60 * 1000; //30min to update
    private WeatherAPI weatherAPI;
    private DBHelper dbHelper;
    private NotificationManager nm;
    private double latitude;
    private double longitude;
    public static final String RESULT_ACTION = "shaiytan.ssapweather.service";
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

        updateTimer = new Timer();
        updateTimer.schedule(updateTask,UPDATE_PERIOD, UPDATE_PERIOD);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        latitude = intent.getDoubleExtra("lat",0);
        longitude = intent.getDoubleExtra("lon",0);
        new Thread(loadTask,"serviceloader").start();
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
            ContentValues cv = new ContentValues();
            cv.put("datetime",weather.getDatetime().getTime());
            cv.put("description",weather.getWeatherDescription());
            cv.put("temperature",weather.getTemperature());
            cv.put("humidity",weather.getHumidity());
            cv.put("icon",weather.getImageID());

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if(db.update("weather",cv,"_id=0",null)==0)
            {
                cv.put("_id",0);
                db.insert("weather",null,cv);
            }
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
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("weather","_id!=0",null);
            for (WeatherItem weather : forecast) {
                ContentValues cv = new ContentValues();
                cv.put("datetime",weather.getDatetime().getTime());
                cv.put("description",weather.getWeatherDescription());
                cv.put("temperature",weather.getTemperature());
                cv.put("humidity",weather.getHumidity());
                cv.put("icon",weather.getImageID());
                db.insert("weather",null,cv);
            }
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
                Cursor cursor = db.query("weather", new String[]{"*"}, "_id=0", null, null, null, null);
                cursor.moveToFirst();
                long lastUpdateTime = cursor.getLong(cursor.getColumnIndex("datetime"));
                WeatherItem res = new WeatherItem(
                        cursor.getString(cursor.getColumnIndex("description")),
                        cursor.getString(cursor.getColumnIndex("icon")),
                        cursor.getDouble(cursor.getColumnIndex("temperature")),
                        cursor.getDouble(cursor.getColumnIndex("humidity")),
                        new Date(lastUpdateTime)
                );
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
            boolean success = loadWeather() && loadForecast();
            intent.putExtra("success",success);
            sendBroadcast(intent);
        }
    };
}
