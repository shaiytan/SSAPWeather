package shaiytan.ssapweather.content;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Shaiytan on 19.06.2017.
 */
// TODO: 20.06.2017 add deserializer
public class WeatherItem {
    private String weatherDescription;
    private String imageID;
    private double temperature;
    private double humidity;

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public String getImageID() {
        return imageID;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public WeatherItem(String weatherDescription, String imageID, double temperature, double humidity) {
        this.weatherDescription = weatherDescription;
        this.imageID = imageID;
        this.temperature = temperature;
        this.humidity = humidity;
    }
    public static class WeatherDeserializer implements JsonDeserializer<WeatherItem>{

        @Override
        public WeatherItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject weather = json.getAsJsonObject().get("weather").getAsJsonArray().get(0).getAsJsonObject();
            String description = weather.get("description").getAsString();
            String icon = weather.get("icon").getAsString();
            JsonObject main = json.getAsJsonObject().get("main").getAsJsonObject();
            double temp = main.get("temp").getAsDouble();
            double humidity = main.get("humidity").getAsDouble();
            return new WeatherItem(description,icon,temp,humidity);
        }
    }
}
