package shaiytan.ssapweather.view;

import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.*;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.*;
import android.view.*;
import android.widget.*;

import com.facebook.*;
import com.facebook.login.*;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.location.places.*;
import com.google.android.gms.location.places.ui.*;
import com.squareup.picasso.Picasso;

import java.util.*;

import shaiytan.ssapweather.R;
import shaiytan.ssapweather.content.*;
import shaiytan.ssapweather.geocoding.Geopoint;
import shaiytan.ssapweather.service.WeatherService;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    public static final int MAP_REQUEST = 1;
    public static final int WEATHER_REQUEST = 2;
    public static final int SIGN_IN_REQUEST = 3;

    private DBHelper dbHelper;
    private ImageView icon;
    private TextView desc;
    private TextView temp;
    private TextView humid;
    private RecyclerView forecastView;
    private SwitchCompat forecastSwitch;
    private SwipeRefreshLayout swipeUpdater;
    private String location="";
    private TextView loc;
    private GoogleApiClient googleApiClient;
    private CallbackManager fbManager;
    private AccessToken fbCurrentToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //инициалицация полей связанных с интерфейсом
        init();

        dbHelper = new DBHelper(this);
        location=dbHelper.getLocation();
        Cursor forecast = dbHelper.readForecast(forecastSwitch.isChecked());

        //при отсутствии актуальных данных о погоде, вызывается сервис
        if(forecast.getCount()>0) {
            WeatherItem currentWeather = dbHelper.readWeather();
            long lastUpdate = currentWeather.getDatetime()*1000;
            if(System.currentTimeMillis()-lastUpdate<30*60*1000) {
                setWeatherView(currentWeather);
                setForecastView(forecast);
            }
            else {
                Intent intent = new Intent(this, WeatherService.class);
                invokeService(intent);
            }
        }
        else {
            Intent intent = new Intent(this, WeatherService.class);
            invokeService(intent);
        }
    }
    private void init() {
        //строка живого поиска
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.mipmap.ic_launcher);
        toolbar.setTitle(R.string.app_name);
        initLiveSearch();

        //кнопки авторизации фейсбука и гугла
        initGoogleSignIn();
        initFacebookSignIn();

        //Карточка текущей погоды
        icon = (ImageView) findViewById(R.id.ic_weather);
        desc = (TextView) findViewById(R.id.desc);
        temp = (TextView) findViewById(R.id.temp);
        humid = (TextView) findViewById(R.id.humid);
        loc = (TextView) findViewById(R.id.loc);

        //Список карточек с прогнозом
        forecastView = (RecyclerView) findViewById(R.id.rec_view);
        forecastView.setLayoutManager(
                new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        forecastView.setItemAnimator(new DefaultItemAnimator());
        forecastSwitch = (SwitchCompat) findViewById(R.id.switch1);
        forecastSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Cursor forecast = dbHelper.readForecast(isChecked);
                setForecastView(forecast);
            }
        });

        //pull to update
        swipeUpdater = (SwipeRefreshLayout) findViewById(R.id.swipe_updater);
        swipeUpdater.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Intent intent = new Intent(MainActivity.this, WeatherService.class);
                invokeService(intent);
                swipeUpdater.setRefreshing(true);
            }
        });
    }
    private void initLiveSearch() {
        PlaceAutocompleteFragment placePicker = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_pick);
        placePicker.setFilter(new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .build());
        placePicker.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                location=place.getName().toString();
                Intent intent = new Intent(MainActivity.this, WeatherService.class);
                intent.addCategory("with_location")
                        .putExtra("lat",place.getLatLng().latitude)
                        .putExtra("lon",place.getLatLng().longitude);
                invokeService(intent);
            }
            @Override
            public void onError(Status status) {
                Toast.makeText(MainActivity.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void initFacebookSignIn() {
        fbManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(fbManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        fbCurrentToken = loginResult.getAccessToken();
                        Toast.makeText(MainActivity.this, loginResult.getAccessToken().getToken(), Toast.LENGTH_SHORT).show();
                    }

                    //заглушки на ошибку
                    @Override
                    public void onCancel() {}
                    @Override
                    public void onError(FacebookException error) {}
                });
    }
    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.google_auth_key))
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Toast.makeText(MainActivity.this, "connectionFailed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_google);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                startActivityForResult(signInIntent, SIGN_IN_REQUEST);
            }
        });
    }

    //вызов сервиса с заданными параметрами
    private void invokeService(Intent intent) {
        PendingIntent result =
                createPendingResult(WEATHER_REQUEST, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        intent.putExtra("location",location);
        intent.putExtra("result",result);
        startService(intent);
    }

    //вывод текущей погоды на экран
    private void setWeatherView(WeatherItem weather) {
        Picasso.with(this).load("http://openweathermap.org/img/w/" + weather.getImageID() +".png").into(icon);
        desc.setText(weather.getWeatherDescription());
        temp.setText(String.format("%+.1f C",weather.getTemperature()));
        humid.setText(String.format("Влажность: %.1f%%",weather.getHumidity()));
        loc.setText(location);
    }

    //вывод прогноза погоды
    private void setForecastView(Cursor cursor) {
        ArrayList <WeatherItem> forecast=new ArrayList<>();
        while (cursor.moveToNext()){
            WeatherItem item = new WeatherItem(
                    cursor.getString(cursor.getColumnIndex("description")),
                    cursor.getString(cursor.getColumnIndex("icon")),
                    cursor.getDouble(cursor.getColumnIndex("temperature")),
                    cursor.getDouble(cursor.getColumnIndex("humidity")),
                    cursor.getLong(cursor.getColumnIndex("datetime")));
            forecast.add(item);
        }
        forecastView.setAdapter(new WeatherAdapter(this,forecast,forecastSwitch.isChecked()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //получение результатов с карты, от сервиса и данные аутентификации от гугла и фейсбука соответственно
        switch (requestCode){
            case MAP_REQUEST:
                if(resultCode==RESULT_OK){
                    Geopoint point = (Geopoint) data.getSerializableExtra("point");
                    location=point.getLongName();
                    Intent intent = new Intent(MainActivity.this, WeatherService.class);
                    intent.addCategory("with_location")
                            .putExtra("lat",point.getLatitude())
                            .putExtra("lon",point.getLongitude());
                    invokeService(intent);

                }
                break;
            case WEATHER_REQUEST:
                if(!data.getBooleanExtra("success",false)) {
                    Toast.makeText(this, "Failed to download", Toast.LENGTH_SHORT).show();
                }
                location=dbHelper.getLocation();
                WeatherItem weather = dbHelper.readWeather();
                setWeatherView(weather);
                Cursor forecast = dbHelper.readForecast(forecastSwitch.isChecked());
                setForecastView(forecast);
                swipeUpdater.setRefreshing(false);
                break;

            //заглушка, та как хз что сделать с полученным токеном
            case SIGN_IN_REQUEST:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {
                    GoogleSignInAccount acct = result.getSignInAccount();
                    Toast.makeText(this, "Token:"+acct.getIdToken(), Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                fbManager.onActivityResult(requestCode,resultCode,data);
                Toast.makeText(this, "Token:"+fbCurrentToken.getToken(), Toast.LENGTH_SHORT).show();
                break;
        }

    }

    //вызов карты
    public void onMapClick(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivityForResult(intent, MAP_REQUEST);
    }
}