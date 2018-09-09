package shaiytan.ssapweather.api;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import shaiytan.ssapweather.model.WeatherItem;

/**
 * Created by Shaiytan on 19.06.2017.
 * Делаются запросы к сайту с погодой
 */

public interface WeatherAPI {
    String DEFAULT_PARAMS = "?units=metric&lang=ru&APPID=9e56cdd894013de6a160a5bc63d9ae8b";

    @GET("weather" + DEFAULT_PARAMS)
    Call<WeatherItem> getCurrentWeather(@Query("lat") double lat, @Query("lon") double lon);

    static WeatherAPI create() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(WeatherItem.class, new WeatherItem.WeatherDeserializer())
                .registerTypeAdapter(WeatherItem[].class, new WeatherItem.ForecastDeserializer())
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit.create(WeatherAPI.class);
    }

    @GET("forecast" + DEFAULT_PARAMS)
    Call<WeatherItem[]> getForecast(@Query("lat") double lat, @Query("lon") double lon);
}
