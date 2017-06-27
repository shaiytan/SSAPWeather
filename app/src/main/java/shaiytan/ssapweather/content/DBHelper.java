package shaiytan.ssapweather.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.*;

import java.util.Date;


/**
 * Created by Shaiytan on 23.06.2017.
 */

public class DBHelper extends SQLiteOpenHelper
{
    private static final String CREATE_TABLE_SCRIPT = "CREATE TABLE weather(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "datetime INTEGER NOT NULL," +
            "description TEXT NOT NULL," +
            "temperature REAL NOT NULL," +
            "humidity REAL NOT NULL," +
            "icon TEXT NOT NULL);";
    private static final String DBNAME="forecast.db";
    private static final int DBVERSION=1;
    private static final String DROP_TABLE_SCRIPT = "DROP TABLE IF EXISTS weather;";
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
    public WeatherItem readWeather()
    {
        Cursor cursor = getReadableDatabase().rawQuery("select * from weather where _id!=0", null);
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
    public Cursor readForecast(boolean extended)
    {
        SQLiteDatabase db = getReadableDatabase();
        if(!extended)
            return db.query("weather",
                    new String[]{"description","icon","temperature","humidity","datetime","max(temperature)"},
                    "_id!=0",null,"strftime('%d',datetime,'unixepoch','localtime')",null,"datetime");
        else return db.rawQuery("select * from weather where _id!=0",null);
    }
    public void writeCurrentWeather(WeatherItem weather)
    {
        ContentValues cv = new ContentValues();
        cv.put("datetime",weather.getDatetime());
        cv.put("description",weather.getWeatherDescription());
        cv.put("temperature",weather.getTemperature());
        cv.put("humidity",weather.getHumidity());
        cv.put("icon",weather.getImageID());

        SQLiteDatabase db = getWritableDatabase();
        if(db.update("weather",cv,"_id=0",null)==0)
        {
            cv.put("_id",0);
            db.insert("weather",null,cv);
        }
    }
    public void writeForecast(WeatherItem[] forecast)
    {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("weather","_id!=0",null);
        for (WeatherItem weather : forecast) {
            ContentValues cv = new ContentValues();
            cv.put("datetime",weather.getDatetime());
            cv.put("description",weather.getWeatherDescription());
            cv.put("temperature",weather.getTemperature());
            cv.put("humidity",weather.getHumidity());
            cv.put("icon",weather.getImageID());
            db.insert("weather",null,cv);
        }
    }
}
