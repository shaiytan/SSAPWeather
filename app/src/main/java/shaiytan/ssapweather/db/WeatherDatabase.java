package shaiytan.ssapweather.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import shaiytan.ssapweather.model.WeatherItem;

@Database(entities = {WeatherItem.class}, version = 2)
public abstract class WeatherDatabase extends RoomDatabase {
    private static final String DB_NAME = "forecast.db";
    private static WeatherDatabase instanse = null;

    private static WeatherDatabase create(Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                WeatherDatabase.class,
                DB_NAME)
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    public static WeatherDatabase getInstanse(Context context) {
        if (instanse == null)
            instanse = create(context);
        return instanse;
    }

    public abstract WeatherDAO getWeatherDAO();
}
