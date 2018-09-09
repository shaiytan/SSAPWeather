package shaiytan.ssapweather.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import shaiytan.ssapweather.model.WeatherItem;

@Dao
public abstract class WeatherDAO {
    @Query("SELECT DISTINCT * FROM weather WHERE id=0")
    public abstract LiveData<WeatherItem> getCurrentWeather();

    @Query("SELECT * FROM weather WHERE id!=0")
    public abstract LiveData<List<WeatherItem>> getForecast();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void updateCurrentWeather(WeatherItem item);

    @Insert
    protected abstract void writeForecast(List<WeatherItem> forecast);

    @Query("DELETE FROM weather WHERE id!=0")
    protected abstract void clearForecast();

    @Transaction
    public void updateForecast(List<WeatherItem> forecast) {
        clearForecast();
        writeForecast(forecast);
    }
}
