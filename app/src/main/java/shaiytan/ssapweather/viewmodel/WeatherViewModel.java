package shaiytan.ssapweather.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import shaiytan.ssapweather.api.WeatherAPI;
import shaiytan.ssapweather.db.WeatherDAO;
import shaiytan.ssapweather.db.WeatherDatabase;
import shaiytan.ssapweather.model.Geopoint;
import shaiytan.ssapweather.model.WeatherItem;

public class WeatherViewModel extends AndroidViewModel {
    private WeatherDAO dao;
    private WeatherAPI api;
    private LiveData<WeatherItem> currentWeatherData;
    private LiveData<List<WeatherItem>> forecastData;
    private MutableLiveData<Boolean> loadingStatus;

    public WeatherViewModel(@NonNull Application application) {
        super(application);
        dao = WeatherDatabase.getInstanse(application).getWeatherDAO();
        api = WeatherAPI.create();
        currentWeatherData = dao.getCurrentWeather();
        forecastData = dao.getForecast();
        loadingStatus = new MutableLiveData<>();
        loadingStatus.setValue(false);
    }

    public LiveData<WeatherItem> getCurrentWeatherData() {
        return currentWeatherData;
    }

    public LiveData<List<WeatherItem>> getForecastData() {
        return forecastData;
    }

    public LiveData<Boolean> getLoadingStatus() {
        return loadingStatus;
    }

    public void loadForecast(Geopoint point, boolean forced) {
        if (!forced && checkForecast(point)) return;
        loadingStatus.setValue(true);
        api.getCurrentWeather(point.getLatitude(), point.getLongitude()).enqueue(new Callback<WeatherItem>() {
            @Override
            public void onResponse(@NonNull Call<WeatherItem> call, @NonNull Response<WeatherItem> response) {
                if (!response.isSuccessful()) {
                    loadingStatus.setValue(null);
                    return;
                }
                WeatherItem weather = response.body();
                if (weather == null) {
                    loadingStatus.setValue(null);
                    return;
                }
                weather.setId(0L);
                weather.setLocation(point.getLongName());
                dao.updateCurrentWeather(weather);
                loadingStatus.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<WeatherItem> call, @NonNull Throwable t) {
                loadingStatus.setValue(null);
            }
        });
        api.getForecast(point.getLatitude(), point.getLongitude()).enqueue(new Callback<WeatherItem[]>() {
            @Override
            public void onResponse(@NonNull Call<WeatherItem[]> call, @NonNull Response<WeatherItem[]> response) {
                if (!response.isSuccessful()) {
                    loadingStatus.setValue(null);
                    return;
                }
                WeatherItem[] forecast = response.body();
                if (forecast == null || forecast.length == 0) {
                    loadingStatus.setValue(null);
                    return;
                }
                for (WeatherItem item : forecast) item.setLocation(point.getLongName());
                dao.updateForecast(Arrays.asList(forecast));
                loadingStatus.setValue(false);
            }

            @Override
            public void onFailure(@NonNull Call<WeatherItem[]> call, @NonNull Throwable t) {
                loadingStatus.setValue(null);
            }
        });
    }

    private boolean checkForecast(Geopoint point) {
        WeatherItem currentWeather = currentWeatherData.getValue();
        List<WeatherItem> forecast = forecastData.getValue();
        return currentWeather != null
                && forecast != null
                && !forecast.isEmpty()
                && currentWeather.getLocation().equals(point.getLongName())
                && forecast.get(0).getDatetime() > System.currentTimeMillis();
    }
}
