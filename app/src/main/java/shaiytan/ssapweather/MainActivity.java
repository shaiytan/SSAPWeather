package shaiytan.ssapweather;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.Date;
import java.util.LinkedList;

import shaiytan.ssapweather.content.DBHelper;
import shaiytan.ssapweather.content.WeatherItem;
import shaiytan.ssapweather.service.WeatherService;


public class MainActivity extends Activity {
    private ListView flist;
    private BroadcastReceiver br;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flist = (ListView) findViewById(R.id.forecast);
        br = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                DBHelper dbHelper = new DBHelper(MainActivity.this);
                Cursor cursor = dbHelper.getReadableDatabase().rawQuery("select * from weather", null);
                Toast.makeText(MainActivity.this, "success "+intent.getBooleanExtra("success",false), Toast.LENGTH_SHORT).show();
                LinkedList<WeatherItem> items = new LinkedList<>();
                while (cursor.moveToNext()){
                    items.add( new WeatherItem(
                            cursor.getString(cursor.getColumnIndex("description")),
                            cursor.getString(cursor.getColumnIndex("icon")),
                            cursor.getDouble(cursor.getColumnIndex("temperature")),
                            cursor.getDouble(cursor.getColumnIndex("humidity")),
                            new Date(cursor.getLong(cursor.getColumnIndex("datetime"))))
                    );
                }
                cursor.close();
                flist.setAdapter(new ArrayAdapter<>(
                        MainActivity.this, android.R.layout.simple_list_item_1, items));
            }
        };
        IntentFilter filter = new IntentFilter(WeatherService.RESULT_ACTION);
        registerReceiver(br,filter);
    }

    public void onLoadClick(View view) {
        Intent intent = new Intent(this, WeatherService.class)
                .putExtra("lat",47.825)
                .putExtra("lon",35.187);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(br);
    }
}
