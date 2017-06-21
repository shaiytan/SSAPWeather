package shaiytan.ssapweather;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.google.gson.GsonBuilder;

import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;
import shaiytan.ssapweather.content.WeatherItem;
import shaiytan.ssapweather.service.WeatherAPI;

public class MainActivity extends Activity implements Callback<WeatherItem[]> {
    private WeatherAPI weatherAPI;
    private ListView flist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory
                        .create(new GsonBuilder()
                                .registerTypeAdapter(WeatherItem[].class, new WeatherItem.ForecastDeserializer())
                                .create()))
                .build();
        weatherAPI = retrofit.create(WeatherAPI.class);
        flist = (ListView) findViewById(R.id.forecast);
    }

    public void onLoadClick(View view) {
        weatherAPI.getForecast(47.825,35.187).enqueue(this);
    }

    @Override
    public void onResponse(Call<WeatherItem[]> call, Response<WeatherItem[]> response) {
        WeatherItem weather[] = response.body();
        flist.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, weather));
    }

    @Override
    public void onFailure(Call<WeatherItem[]> call, Throwable t) {
        Toast.makeText(this, "ShitHappens", Toast.LENGTH_SHORT).show();
    }
}
