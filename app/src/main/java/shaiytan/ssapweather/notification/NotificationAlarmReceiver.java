package shaiytan.ssapweather.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.model.WeatherItem;

public class NotificationAlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_DESCRIPTION = "desc";
    public static final String EXTRA_IMAGE_ID = "imgid";
    public static final String EXTRA_TEMPERATURE = "temp";
    public static final String EXTRA_HUMIDITY = "humid";
    public static final String EXTRA_DATETIME = "dt";
    public static final String EXTRA_LOCATION = "loc";
    public static final String EXTRA_ID = "id";

    public static PendingIntent createPendingIntent(Context context, int requestCode, WeatherItem data) {
        Intent intent = new Intent(context, NotificationAlarmReceiver.class);
        intent.setAction("shaiytan.nar.notification");
        intent
                .putExtra(EXTRA_DESCRIPTION, data.getWeatherDescription())
                .putExtra(EXTRA_IMAGE_ID, data.getImageID())
                .putExtra(EXTRA_TEMPERATURE, data.getTemperature())
                .putExtra(EXTRA_HUMIDITY, data.getHumidity())
                .putExtra(EXTRA_DATETIME, data.getDatetime())
                .putExtra(EXTRA_LOCATION, data.getLocation())
                .putExtra(EXTRA_ID, data.getId());
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        WeatherItem res = getWeatherItem(intent);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(res.getLocation() + ": " + res.getTemperature() + " C, " + res.getWeatherDescription())
                .setContentText("Влажность: " + res.getHumidity())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
        nm.notify(1, notification);
    }

    private WeatherItem getWeatherItem(Intent intent) {
        return new WeatherItem(
                intent.getStringExtra(EXTRA_DESCRIPTION),
                intent.getStringExtra(EXTRA_IMAGE_ID),
                intent.getDoubleExtra(EXTRA_TEMPERATURE, 0.0),
                intent.getDoubleExtra(EXTRA_HUMIDITY, 0.0),
                intent.getLongExtra(EXTRA_DATETIME, 0L),
                intent.getStringExtra(EXTRA_LOCATION),
                intent.getLongExtra(EXTRA_ID, 0L)
        );
    }
}
