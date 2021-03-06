package shaiytan.ssapweather.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.model.WeatherItem;

/**
 * Created by Shaiytan on 26.06.2017.
 * адаптер для красивого отображения погоды в списке
 */
class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    private List<WeatherItem> forecast;
    private Context context;

    WeatherAdapter(Context context, List<WeatherItem> forecast) {
        this.forecast = forecast;
        this.context = context;
    }

    //создание карточки с погодой, и последующее её переиспользование
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.weather_item, parent, false);
        return new ViewHolder(v);
    }

    //Размещение данных на карточке
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherItem item = forecast.get(position);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(item.getDatetime());
        Locale locale = Locale.getDefault();
        holder.datetime.setText(String.format(locale, "%1$td.%1$tm\n%1$tH:%1$tM", calendar));
        Picasso.with(context)
                .load("http://openweathermap.org/img/w/" + item.getImageID() + ".png")
                .into(holder.icon);
        holder.maxTemp.setText(String.format(locale, "%+.1f C", item.getTemperature()));
        holder.humidity.setText(String.format(locale, "%.1f%%", item.getHumidity()));
    }

    @Override
    public int getItemCount() {
        return forecast.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView icon;
        private TextView maxTemp;
        private TextView humidity;
        private TextView datetime;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ic_forecast);
            maxTemp = itemView.findViewById(R.id.temp_max);
            humidity = itemView.findViewById(R.id.humidity);
            datetime = itemView.findViewById(R.id.datetime);
        }
    }

}
