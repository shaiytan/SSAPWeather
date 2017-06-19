package shaiytan.ssapweather;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.content.WeatherItem;
import shaiytan.ssapweather.service.WeatherAPI;

public class MainActivity extends Activity implements Callback<WeatherItem> {

    private Retrofit retrofit;
    private WeatherAPI weatherAPI;
    private TextView desc;
    private TextView icon;
    private TextView temp;
    private TextView humidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        retrofit = new Retrofit.Builder()
                .baseUrl("http://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory
                        .create(new GsonBuilder()
                                .registerTypeAdapter(WeatherItem.class,new WeatherItem.WeatherDeserializer())
                                .create()))
                .build();
        weatherAPI = retrofit.create(WeatherAPI.class);
        desc = (TextView) findViewById(R.id.desc);
        icon = (TextView) findViewById(R.id.ic);
        temp = (TextView) findViewById(R.id.temp);
        humidity = (TextView) findViewById(R.id.humid);
    }

    public void onLoadClick(View view) {
        weatherAPI.getCurrentWeather(47.825,35.187).enqueue(this);
    }

    @Override
    public void onResponse(Call<WeatherItem> call, Response<WeatherItem> response) {
        if(response.isSuccessful()) {
            WeatherItem weather = response.body();
            desc.setText(weather.getWeatherDescription());
            icon.setText(weather.getImageID());
            temp.setText(weather.getTemperature() + " C");
            humidity.setText(weather.getHumidity() + "%");
        }
        else {
            Toast.makeText(this, "ShitHappens", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFailure(Call<WeatherItem> call, Throwable t) {
        Toast.makeText(this, "ShitHappens", Toast.LENGTH_SHORT).show();
    }
}
