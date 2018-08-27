package shaiytan.ssapweather.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by Shaiytan on 19.06.2017.
 * Модель прогноза погоды
 */
@Entity(tableName = "weather")
public class WeatherItem {
    @ColumnInfo(name = "description")
    private String weatherDescription;

    @ColumnInfo(name = "icon")
    private String imageID;

    private double temperature;
    private double humidity;
    private long datetime;
    private String location;

    @PrimaryKey(autoGenerate = true)
    private Long id;

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

    public long getDatetime() {
        return datetime;
    }

    @Ignore
    public WeatherItem(
            String weatherDescription,
            String imageID,
            double temperature,
            double humidity,
            long datetime) {
        this(weatherDescription, imageID, temperature, humidity, datetime, null, null);
    }

    public WeatherItem(
            String weatherDescription,
            String imageID,
            double temperature,
            double humidity,
            long datetime,
            String location,
            Long id) {
        this.weatherDescription = weatherDescription;
        this.imageID = imageID;
        this.temperature = temperature;
        this.humidity = humidity;
        this.datetime = datetime;
        this.location = location;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public static class WeatherDeserializer implements JsonDeserializer<WeatherItem> {
        @Override
        public WeatherItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonObject weather = jsonObject.get("weather").getAsJsonArray().get(0).getAsJsonObject();
            String description = weather.get("description").getAsString();
            String icon = weather.get("icon").getAsString();
            JsonObject main = jsonObject.get("main").getAsJsonObject();
            double temp = main.get("temp").getAsDouble();
            double humidity = main.get("humidity").getAsDouble();
            long datetime = jsonObject.get("dt").getAsLong();
            return new WeatherItem(description, icon, temp, humidity, datetime * 1000);
        }
    }

    public static class ForecastDeserializer implements JsonDeserializer<WeatherItem[]> {
        @Override
        public WeatherItem[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(WeatherItem.class, new WeatherItem.WeatherDeserializer())
                    .create();
            return gson.fromJson(json.getAsJsonObject().get("list"), WeatherItem[].class);
        }
    }
}
