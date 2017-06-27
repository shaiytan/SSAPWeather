package shaiytan.ssapweather.content;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by Shaiytan on 19.06.2017.
 */
public class WeatherItem {
    private String weatherDescription;
    private String imageID;
    private double temperature;
    private double humidity;
    private long datetime;
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

    public WeatherItem(String weatherDescription, String imageID, double temperature, double humidity, long datetime) {
        this.weatherDescription = weatherDescription;
        this.imageID = imageID;
        this.temperature = temperature;
        this.humidity = humidity;
        this.datetime = datetime;
    }

    @Override
    public String toString() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(datetime*1000);
        return String.format("Weather: %s" + "\nTemperature: %.1f C" +
                        "\nHumidity: %.1f %%\nDate: %02d.%02d %02d:%02d",
                weatherDescription,temperature,humidity,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH)+1,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
    }

    public static class WeatherDeserializer implements JsonDeserializer<WeatherItem>{

        @Override
        public WeatherItem deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            JsonObject weather = jsonObject.get("weather").getAsJsonArray().get(0).getAsJsonObject();
            String description = weather.get("description").getAsString();
            String icon = weather.get("icon").getAsString();
            JsonObject main = jsonObject.get("main").getAsJsonObject();
            double temp = main.get("temp").getAsDouble();
            double humidity = main.get("humidity").getAsDouble();
            long datetime = jsonObject.get("dt").getAsLong();
            return new WeatherItem(description,icon,temp,humidity,datetime);
        }
    }
    public static class ForecastDeserializer implements JsonDeserializer<WeatherItem[]>{

        @Override
        public WeatherItem[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Gson gson=new GsonBuilder().registerTypeAdapter(WeatherItem.class, new WeatherItem.WeatherDeserializer()).create();
            return gson.fromJson(json.getAsJsonObject().get("list"),WeatherItem[].class);
        }
    }
}
