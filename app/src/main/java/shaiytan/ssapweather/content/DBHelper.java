package shaiytan.ssapweather.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;


/**
 * Created by Shaiytan on 23.06.2017.
 */

public class DBHelper extends SQLiteOpenHelper
{
    //Информация о БД
    private static final String DBNAME="forecast.db";
    private static final int DBVERSION=1;

    //SQL Запросы
    private static final String CREATE_TABLE_SCRIPT = "CREATE TABLE weather(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "location TEXT NOT NULL," +
            "datetime INTEGER NOT NULL," +
            "description TEXT NOT NULL," +
            "temperature REAL NOT NULL," +
            "humidity REAL NOT NULL," +
            "icon TEXT NOT NULL);";
    private static final String DROP_TABLE_SCRIPT = "DROP TABLE IF EXISTS weather;";
    //В записи _id=0 хранится текущая погода, в остальных - прогноз на ближайшие 5 дней
    private static final String SELECT_CURRENT_WEATHER = "SELECT * FROM weather WHERE _id=0;";
    private static final String SELECT_FORECAST = "SELECT * FROM weather WHERE _id!=0;";
    public static final String SELECT_LOCATION = "SELECT DISTINCT location FROM weather;";

    public DBHelper(Context context) {
            super(context, DBNAME, null, DBVERSION);
        }
    @Override
    public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_SCRIPT);
        }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_SCRIPT);
        onCreate(db);
    }

    public WeatherItem readWeather(){
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_CURRENT_WEATHER, null);
        cursor.moveToNext();
        WeatherItem item = new WeatherItem(
                cursor.getString(cursor.getColumnIndex("description")),
                cursor.getString(cursor.getColumnIndex("icon")),
                cursor.getDouble(cursor.getColumnIndex("temperature")),
                cursor.getDouble(cursor.getColumnIndex("humidity")),
                cursor.getLong(cursor.getColumnIndex("datetime")));
        cursor.close();
        return item;
    }

    public Cursor readForecast(boolean extended) {
        SQLiteDatabase db = getReadableDatabase();
        if(!extended) //прогноз фильтруется - берем по одной записи в день, с максимальным значением температуры
            return db.query("weather",
                    new String[]{"description","icon","temperature","humidity","datetime","max(temperature)"},
                    "_id!=0",null,"strftime('%d',datetime,'unixepoch','localtime')",null,"datetime");
        else return db.rawQuery(SELECT_FORECAST,null); //или же вывод погоды по часам
    }

    public void writeCurrentWeather(WeatherItem weather,String location) {
        ContentValues cv = new ContentValues();
        cv.put("datetime",weather.getDatetime());
        cv.put("description",weather.getWeatherDescription());
        cv.put("temperature",weather.getTemperature());
        cv.put("humidity",weather.getHumidity());
        cv.put("icon",weather.getImageID());
        cv.put("location",location);
        SQLiteDatabase db = getWritableDatabase();

        if(db.update("weather",cv,"_id=0",null)==0) {
            cv.put("_id",0);
            db.insert("weather",null,cv);
        }
    }

    public void writeForecast(WeatherItem[] forecast,String location) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("weather","_id!=0",null);//Стереть устаревшие данные
        for (WeatherItem weather : forecast) {
            ContentValues cv = new ContentValues();
            cv.put("datetime",weather.getDatetime());
            cv.put("description",weather.getWeatherDescription());
            cv.put("temperature",weather.getTemperature());
            cv.put("humidity",weather.getHumidity());
            cv.put("icon",weather.getImageID());
            cv.put("location",location);
            db.insert("weather",null,cv);
        }
    }

    public String getLocation() {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_LOCATION, null);
        String location;
        if (cursor.moveToNext()) location = cursor.getString(0);
        else location = "";
        cursor.close();
        return location;
    }
}
