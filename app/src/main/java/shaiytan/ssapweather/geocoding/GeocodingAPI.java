package shaiytan.ssapweather.geocoding;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Shaiytan on 27.06.2017.
 * Используется для нахождения названия города по заданным координатам
 */

public interface GeocodingAPI {
    String DEFAULT_PARAMS =
            "?language=ru&result_type=locality|administrative_area_level_1&" +
                    "key=AIzaSyDyqI-qCuVFLicdMj48DpNlxVTnOgAFg4w";
    @GET("geocode/json"+DEFAULT_PARAMS)
    Call<Geopoint> geocode(@Query("latlng")String latlng);
}
