package shaiytan.ssapweather.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import shaiytan.ssapweather.model.Geopoint;

/**
 * Created by Shaiytan on 27.06.2017.
 * Используется для нахождения названия города по заданным координатам
 */

public interface GeocodingAPI {
    String DEFAULT_PARAMS =
            "?language=ru&result_type=locality|administrative_area_level_1&" +
                    "key=AIzaSyDyqI-qCuVFLicdMj48DpNlxVTnOgAFg4w";

    static GeocodingAPI create() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Geopoint.class, new Geopoint.GeopointDeserializer())
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/maps/api/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit.create(GeocodingAPI.class);
    }

    @GET("geocode/json" + DEFAULT_PARAMS)
    Call<Geopoint> geocode(@Query("latlng") String latlng);
}
