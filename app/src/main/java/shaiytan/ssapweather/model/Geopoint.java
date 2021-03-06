package shaiytan.ssapweather.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * Created by Shaiytan on 27.06.2017.
 */

public class Geopoint implements Serializable {
    private String longName;
    private double latitude;
    private double longitude;

    public Geopoint(String longName, double latitude, double longitude) {
        this.longName = longName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLongName() {
        return longName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public static class GeopointDeserializer implements JsonDeserializer<Geopoint> {

        @Override
        public Geopoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject()
                    .get("results").getAsJsonArray().get(0).getAsJsonObject();
            String long_name = jsonObject.get("address_components")
                    .getAsJsonArray().get(0)
                    .getAsJsonObject().get("long_name").getAsString();
            JsonObject location = jsonObject.get("geometry").getAsJsonObject()
                    .get("location").getAsJsonObject();
            double lat = location.get("lat").getAsDouble();
            double lng = location.get("lng").getAsDouble();
            return new Geopoint(long_name, lat, lng);
        }
    }
}
