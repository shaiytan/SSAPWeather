package shaiytan.ssapweather.view;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.WeatherItem;

/**
 * Created by Shaiytan on 26.06.2017.
 */
public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    private List<WeatherItem> forecast;
    private Context context;
    public WeatherAdapter(Context context,List<WeatherItem> forecast) {
        this.forecast = forecast;
        this.context=context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.weather_item,parent,false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        WeatherItem item = forecast.get(position);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getDefault());
        calendar.setTimeInMillis(item.getDatetime()*1000);
        holder.datetime.setText(String.format("%1$td.%1$tm\n%1$tH:%1$tM", calendar));
        Picasso.with(context)
                .load("http://openweathermap.org/img/w/" + item.getImageID() +".png")
                .into(holder.icon);
        holder.maxTemp.setText(String.format("%+.1f C",item.getTemperature()));
        holder.humidity.setText(String.format("%.1f%%",item.getHumidity()));
    }
    @Override
    public int getItemCount() {
        return forecast.size();
    }
    class ViewHolder extends RecyclerView.ViewHolder{
        private ImageView icon;
        private TextView maxTemp;
        private TextView humidity;
        private TextView datetime;
        ViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.ic_forecast);
            maxTemp = (TextView) itemView.findViewById(R.id.temp_max);
            humidity = (TextView) itemView.findViewById(R.id.humidity);
            datetime= (TextView) itemView.findViewById(R.id.datetime);
        }
    }

}
