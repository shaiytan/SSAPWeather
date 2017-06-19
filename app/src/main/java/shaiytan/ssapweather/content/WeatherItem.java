package shaiytan.ssapweather.content;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Shaiytan on 19.06.2017.
 */

public class WeatherItem {
    @SerializedName("weather")
    private List<WeatherCommon> wc;
    @SerializedName("main")
    private WeatherValues wv;

    public String getWeatherDescription() {
        return wc.get(0).weatherDescription;
    }

    public String getImageID() {
        return wc.get(0).imageID;
    }

    public double getTemperature() {
        return wv.temperature;
    }

    public double getHumidity() {
        return wv.humidity;
    }

    public WeatherItem(List<WeatherCommon> wc, WeatherValues wv) {
        this.wc = wc;
        this.wv = wv;
    }
    class WeatherCommon{
        @SerializedName("description")
        String weatherDescription;
        @SerializedName("icon")
        String imageID;
    }
    class WeatherValues{
        @SerializedName("temp")
        private double temperature;
        @SerializedName("humidity")
        private double humidity;
    }
}
