package com.example.annabujak.weather4runners.CentralControl;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.widget.Toast;

import com.example.annabujak.weather4runners.Database.DBManager;
import com.example.annabujak.weather4runners.Listeners.AddChosenHourListener;
import com.example.annabujak.weather4runners.Listeners.DailyPropositionsChangedListener;
import com.example.annabujak.weather4runners.Listeners.UpdatingFinishedListener;
import com.example.annabujak.weather4runners.Listeners.WeeklyPropositionsChangedListener;
import com.example.annabujak.weather4runners.Objects.ChosenProposition;
import com.example.annabujak.weather4runners.Objects.Preference;
import com.example.annabujak.weather4runners.Objects.PreferenceBalance;
import com.example.annabujak.weather4runners.Objects.PropositionsList;
import com.example.annabujak.weather4runners.Objects.User;
import com.example.annabujak.weather4runners.Objects.WeatherInfo;
import com.example.annabujak.weather4runners.R;
import com.example.annabujak.weather4runners.Weather.Approximators.WeatherInfosLinearApproximatorFactory;
import com.example.annabujak.weather4runners.Weather.Filter.WeatherFilter;
import com.example.annabujak.weather4runners.Weather.JSONDownloaders.JSONWeatherByCoordinatesDownloader;
import com.example.annabujak.weather4runners.Weather.JSONDownloaders.JSONWeatherByNameDownloader;
import com.example.annabujak.weather4runners.Weather.JSONParsers.Extractors.JSONOpenWeatherMapValuesExtractorFactory;
import com.example.annabujak.weather4runners.Weather.JSONTransformator;
import com.example.annabujak.weather4runners.Weather.JSONTransformatorBuilder;
import com.example.annabujak.weather4runners.Weather.JSONDownloaders.JSONWeatherDownloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by slowik on 07.05.2017.
 */

public class CentralControl {

    private static final int HOURS_PER_FORECAST = 3;

    private static final int BEST_WEATHER_PROPOSITIONS = Integer.MAX_VALUE;

    private Context context;

    private DBManager databaseManager;

    private WeatherForecastManager weatherForecastManager;

    private WeatherFilter weatherFilter;

    private DailyPropositionsChangedListener dailyPropositionsChangedListener;

    private WeeklyPropositionsChangedListener weeklyPropositionsChangedListener;

    private UpdatingFinishedListener updatingFinishedListener;

    private AddChosenHourListener addChosenHourListener;

    public CentralControl(Context context) {
        this.context = context;

        this.databaseManager = new DBManager(context);
        this.weatherForecastManager = new WeatherForecastManager(
                getDefaultJSONTransformator());

        this.weatherFilter = new WeatherFilter(BEST_WEATHER_PROPOSITIONS,
                getPreferenceBalanceOrDefault());
    }

    public void setLocation(String city, String country) {
        this.weatherForecastManager.setLocation(city, country);
    }

    public void setLocation(double longitude, double latitude) {
        this.weatherForecastManager.setLocation(longitude, latitude);
    }

    public void setByCoordinatesWeatherDownloader() {
        this.weatherForecastManager.setByCoordinatesWeatherDownloader();
    }

    public void setByNameWeatherDownloader() {
        this.weatherForecastManager.setByNameWeatherDownloader();
    }

    public void setDailyPropositionsChangedListener(DailyPropositionsChangedListener listener) {
        this.dailyPropositionsChangedListener = listener;
    }
    public void setAddChosenHourListener(AddChosenHourListener listener){
        this.addChosenHourListener = listener;
    }

    public void setWeeklyPropositionsChangedListener(WeeklyPropositionsChangedListener listener) {
        this.weeklyPropositionsChangedListener = listener;
    }

    public void setUpdatingFinishedListener(UpdatingFinishedListener listener) {
        this.updatingFinishedListener = listener;
    }

    public void updateWeatherForecastAsync() {
        (new WeatherForecastUpdaterTask()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void updatePropositionsAsync() {
        recomputePropositionsAsync(this.databaseManager.GetWeatherData());
    }

    public void updateUser(User user){
        databaseManager.UpdateUserDatas(user);
    }
    public void addChosenHour(ChosenProposition hour){
        databaseManager.AddChosenHourAndUpdateIsChecked(hour);
        updatePropositionsAsync();
    }
    public List<ChosenProposition> getAllChosenHours(){
        return databaseManager.GetChosenHours();
    }
    public void updatePreference(Preference preference){databaseManager.UpdatePreferences(preference);}
    public void updatePreferenceBalance(PreferenceBalance balance){
        databaseManager.UpdatePreferenceBalance(balance);
        this.weatherFilter = new WeatherFilter(BEST_WEATHER_PROPOSITIONS, balance);
    }

    private void recomputePropositionsAsync(ArrayList<WeatherInfo> weatherForecast) {
        (new PropositionsComputer()).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                weatherForecast);
    }

    private JSONTransformator getDefaultJSONTransformator() {
        return (new JSONTransformatorBuilder())
                .setApproximatorFactory(new WeatherInfosLinearApproximatorFactory())
                .setValuesExtractorFactory(new JSONOpenWeatherMapValuesExtractorFactory())
                .setHoursPerForecast(HOURS_PER_FORECAST)
                .build();
    }

    private JSONWeatherDownloader getDefaultJSONDownloader(String cityName,
                                                           String coutryName,
                                                           String language) {
        return new JSONWeatherByNameDownloader(cityName, coutryName, language);
    }

    public PreferenceBalance getPreferenceBalanceOrDefault() {
        PreferenceBalance res;
        try {
            res = databaseManager.GetPreference().getPreferenceBalance();
        } catch(Exception e) {
            res = new PreferenceBalance();
        }

        return res;
    }

    private Preference getUserWeatherPreferenceOrDefault() {
        try {
            return databaseManager.GetPreference();
        } catch(Exception e) {
            return new Preference();
        }
    }

    class WeatherForecastUpdaterTask extends AsyncTask<Void, Void, ArrayList<WeatherInfo>> {

        @Override
        protected ArrayList<WeatherInfo> doInBackground(Void... params) {
            try {
                ArrayList<WeatherInfo> weatherForecast = weatherForecastManager
                        .getNewestWeatherForecast();
                databaseManager.UpdateWeatherData(weatherForecast);
                return weatherForecast;
            } catch(IOException e) {
                this.cancel(true);
                return new ArrayList<>();
            }
        }

        @Override
        protected void onCancelled(ArrayList<WeatherInfo> weatherInfos) {
            super.onCancelled(weatherInfos);

            Toast.makeText(context,
                    context.getResources().getString(R.string.error_with_internet_con_message),
                    Toast.LENGTH_LONG).show();
            updatingFinishedListener.onUpdatingFinished();
        }

        @Override
        protected void onPostExecute(ArrayList<WeatherInfo> weatherInfos) {
            super.onPostExecute(weatherInfos);

            recomputePropositionsAsync(weatherInfos);
        }
    }

    class PropositionsComputer
            extends AsyncTask<ArrayList<WeatherInfo>, Void, Pair<ArrayList<WeatherInfo>, ArrayList<WeatherInfo>>> {

        @Override
        protected Pair<ArrayList<WeatherInfo>, ArrayList<WeatherInfo>> doInBackground(ArrayList<WeatherInfo>... params) {
            ArrayList<WeatherInfo> weatherForecast = params[0];

            Preference preference = getUserWeatherPreferenceOrDefault();

            ArrayList<WeatherInfo> dailyPropositions = weatherFilter
                    .GetDailyWeather(weatherForecast, preference);

            ArrayList<WeatherInfo> weeklyPropositions = weatherFilter
                    .GetWeeklyWeather(weatherForecast, preference);

            return new Pair<>(dailyPropositions, weeklyPropositions);
        }

        @Override
        protected void onPostExecute(Pair<ArrayList<WeatherInfo>, ArrayList<WeatherInfo>> propositionsPair) {
            super.onPostExecute(propositionsPair);

            dailyPropositionsChangedListener.onDailyPropositionsChanged(
                    new PropositionsList(propositionsPair.first));
            weeklyPropositionsChangedListener.onWeeklyPropositionsChanged(
                    new PropositionsList(propositionsPair.second));

            updatingFinishedListener.onUpdatingFinished();
        }
    }
}
