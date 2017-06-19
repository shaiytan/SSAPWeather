package shaiytan.ssapweather.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import shaiytan.ssapweather.content.WeatherItem;

/**
 * Created by Shaiytan on 19.06.2017.
 */

public interface WeatherAPI {
    @GET("weather?units=metric&lang=ru&APPID=9e56cdd894013de6a160a5bc63d9ae8b")
    Call<WeatherItem> getCurrentWeather(@Query("lat")double lat,@Query("lon")double lon);
}