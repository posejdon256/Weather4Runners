package com.example.annabujak.weather4runners;

import android.Manifest;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.annabujak.weather4runners.CentralControl.CentralControl;
import com.example.annabujak.weather4runners.Dialogs.DialogFactory;
import com.example.annabujak.weather4runners.Dialogs.LocationTypeDialog;
import com.example.annabujak.weather4runners.Enum.WeatherConditionsNames;
import com.example.annabujak.weather4runners.Fragments.ChartFragment;
import com.example.annabujak.weather4runners.Fragments.CreditsFragment;
import com.example.annabujak.weather4runners.Fragments.ImportantConditionsFragment.ImportantConditionsFragment;
import com.example.annabujak.weather4runners.Listeners.AddChosenHourListener;
import com.example.annabujak.weather4runners.Listeners.ChosenPropositionsProvider;
import com.example.annabujak.weather4runners.Listeners.CityCountryLocationSetListener;
import com.example.annabujak.weather4runners.Listeners.CurrentLocationSetListener;
import com.example.annabujak.weather4runners.Listeners.DailyPropositionsChangedListener;
import com.example.annabujak.weather4runners.Listeners.ImportantConditionsChangedListener;
import com.example.annabujak.weather4runners.Listeners.PropositionClickedListener;
import com.example.annabujak.weather4runners.Listeners.UpdatingFinishedListener;
import com.example.annabujak.weather4runners.Listeners.WeatherConditionsImportanceOrderProvider;
import com.example.annabujak.weather4runners.Listeners.WeatherForecastUpdater;
import com.example.annabujak.weather4runners.Listeners.WeeklyPropositionsChangedListener;
import com.example.annabujak.weather4runners.Facebook.ILoginFacebook;
import com.example.annabujak.weather4runners.Fragments.LoginFragment;
import com.example.annabujak.weather4runners.Fragments.PagerFragment;
import com.example.annabujak.weather4runners.Fragments.WeatherPreferenceFragment.IWeatherPreferenceFragment;
import com.example.annabujak.weather4runners.Fragments.WeatherPreferenceFragment.WeatherPreferenceFragment;
import com.example.annabujak.weather4runners.Notifiers.DailyWeatherPropositionsNotifier;
import com.example.annabujak.weather4runners.Notifiers.WeeklyWeatherPropositionsNotifier;
import com.example.annabujak.weather4runners.Objects.ChosenProposition;
import com.example.annabujak.weather4runners.Objects.Preference;
import com.example.annabujak.weather4runners.Objects.PreferenceBalance;
import com.example.annabujak.weather4runners.Objects.PropositionsList;
import com.example.annabujak.weather4runners.Objects.User;
import com.example.annabujak.weather4runners.Tracker.LocationTracker;
import com.example.annabujak.weather4runners.Weather.JSONDownloaders.WeatherDownloadersManager;
import com.facebook.FacebookSdk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity
        implements DailyPropositionsChangedListener,
        WeeklyPropositionsChangedListener,
        AddChosenHourListener,
        DailyWeatherPropositionsNotifier,
        WeeklyWeatherPropositionsNotifier,
        UpdatingFinishedListener,
        ILoginFacebook,
        IWeatherPreferenceFragment,
        NavigationView.OnNavigationItemSelectedListener,
        ImportantConditionsChangedListener,
        WeatherForecastUpdater,
        PropositionClickedListener,
        WeatherConditionsImportanceOrderProvider,
        ChosenPropositionsProvider,
        CurrentLocationSetListener,
        CityCountryLocationSetListener {

    private static final int REQUEST_LOCATION_PERMISSIONS_CODE = 123;

    private static double DEFAULT_LONGITUDE = 21.0042;

    private static String LONGITUTE_SHARED_PREF = "longitude_shared_pref";

    private static double DEFAULT_LATITUDE = 52.1347;

    private static String LATITUDE_SHARED_PREF = "latitude_shared_pref";

    private static String CITY_NAME_SHARED_PREF = "city_name_shared_pref";

    private static String DEFAULT_CITY_NAME = "Warsaw";

    private static String COUNTRY_NAME_SHARED_PREF = "country_name_shared_pref";

    private static String DEFAULT_COUNTRY_NAME = "Poland";

    private static String DOWNLOADER_TYPE_SHARED_PREF = "location_type_shared_pref";

    private static String DEFAULT_DOWNLOADER_TYPE = WeatherDownloadersManager.BY_NAME_DOWNLOADER;

    private SharedPreferencesHelper sharedPreferencesHelper;

    private ProgressBar mLoadingIndicator;

    private NavigationView mNavigationView;

    private DrawerLayout mDrawerLayout;

    private CentralControl centralControl;

    private LinkedList<DailyPropositionsChangedListener> dailyPropositionsChangedListeners;

    private LinkedList<WeeklyPropositionsChangedListener> weeklyPropositionsChangedListeners;

    private PropositionsList dailyPropositions, weeklyPropositions;

    private LocationManager locationManager;

    private LocationTracker locationTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setViewReferences();
        restorePreviousStateIfAny(savedInstanceState);

        initListenersLists();
        initEmptyPropositionsList();

        createManagers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateLocation();
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.centralControl.updatePropositionsAsync();
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.sharedPreferencesHelper.saveSharedPref(
                LONGITUTE_SHARED_PREF,
                (float)this.locationTracker.getLongitude()
        );

        this.sharedPreferencesHelper.saveSharedPref(
                LATITUDE_SHARED_PREF,
                (float)this.locationTracker.getLatitude()
        );
    }

    @Override
    public void onBackPressed() {
        if (this.mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_set_location:
                LocationTypeDialog dialog = DialogFactory.getLocationTypeDialog(this, this);
                dialog.show(getFragmentManager(), "locationTypeDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        boolean returnResult = false;
        switch(item.getItemId()) {
            case R.id.nav_propositions:
                setFragment(new PagerFragment(), true);
                returnResult = true;
                break;
            case R.id.nav_favourite_values:
                setFragment(new WeatherPreferenceFragment(), true);
                returnResult = true;
                break;
            case R.id.id_nav_charts:
                setFragment(ChartFragment.create(getAllChosenPropositions()),
                        true);
                returnResult = true;
                break;
            case R.id.nav_order_of_imporance:
                ArrayList<WeatherConditionsNames> importantConditions = new ArrayList<>(
                        getWeatherConditionsImportanceOrder()
                );
                setFragment(ImportantConditionsFragment.Create(importantConditions), true);
                returnResult = true;
                break;
            case R.id.id_nav_info:
                setFragment(new CreditsFragment(), true);
                returnResult = true;
                break;
        }

        this.mDrawerLayout.closeDrawer(GravityCompat.START);

        return returnResult;
    }

    @Override
    public void UpdatePreference(Preference preference) {
        preference.setPreferenceBalance(centralControl.getPreferenceBalanceOrDefault());
        centralControl.updatePreference(preference);
        centralControl.updatePropositionsAsync();
    }

    @Override
    public void subscribeForDailyWeatherPropositionsChanged(DailyPropositionsChangedListener listener) {
        this.dailyPropositionsChangedListeners.add(listener);
        listener.onDailyPropositionsChanged(this.dailyPropositions);
    }

    @Override
    public void unsubscribeForDailyWeatherPropositionsChanged(DailyPropositionsChangedListener listener) {
        this.dailyPropositionsChangedListeners.remove(listener);
    }

    @Override
    public void subscribeForWeeklyWeatherPropositionsChanged(WeeklyPropositionsChangedListener listener) {
        this.weeklyPropositionsChangedListeners.add(listener);
        listener.onWeeklyPropositionsChanged(this.weeklyPropositions);
    }

    @Override
    public void unsubscribeForWeeklyWeatherPropositionsChanged(WeeklyPropositionsChangedListener listener) {
        this.weeklyPropositionsChangedListeners.remove(listener);
    }

    @Override
    public void onDailyPropositionsChanged(PropositionsList propositions) {
        this.dailyPropositions = propositions;
        for(DailyPropositionsChangedListener listener: this.dailyPropositionsChangedListeners) {
            listener.onDailyPropositionsChanged(propositions.getDeepCopy());
        }
    }

    @Override
    public void onWeeklyPropositionsChanged(PropositionsList propositions) {
        this.weeklyPropositions = propositions;
        for(WeeklyPropositionsChangedListener listener: this.weeklyPropositionsChangedListeners) {
            listener.onWeeklyPropositionsChanged(propositions.getDeepCopy());
        }
    }

    @Override
    public void onUpdatingFinished() {
        this.mLoadingIndicator.setVisibility(View.INVISIBLE);
    }

    @Override
    public void StartPagerFragment(){
        setFragment(new PagerFragment(), false);
    }

    @Override
    public void AddUser(String firstName, String secondName) {
        User user = new User(firstName,secondName);
        centralControl.updateUser(user);
    }

    @Override
    public void onImportantConditionsChangedListener(ArrayList<WeatherConditionsNames> orderedImportantConditions) {
        PreferenceBalance Balance = new PreferenceBalance(orderedImportantConditions);
        centralControl.updatePreferenceBalance(Balance);
        centralControl.updatePropositionsAsync();
    }

    @Override
    public void onWeatherForecastUpdate() {
        this.refreshAll();
    }

    @Override
    public void onPropositionClickedListener(ChosenProposition clickedProposition) {
        centralControl.addChosenHour(clickedProposition);
    }

    @Override
    public void onAddedChosenHour(ChosenProposition chosenProposition) {
        centralControl.addChosenHour(chosenProposition);
    }

    @Override
    public ArrayList<WeatherConditionsNames> getWeatherConditionsImportanceOrder() {
        return centralControl.getPreferenceBalanceOrDefault()
                .getWeatherConditionsOrder();
    }

    @Override
    public List<ChosenProposition> getAllChosenPropositions() {
        return this.centralControl.getAllChosenHours();
    }

    @Override
    public void setCityCountryLocation(String city, String country) {
        this.locationTracker.cancelUpdatingLocation(this.locationManager);

        this.centralControl.setLocation(city, country);
        this.centralControl.setByNameWeatherDownloader();

        Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.location_set_message) + String.format(": %s, %s", city, country),
                Toast.LENGTH_LONG)
                .show();

        this.sharedPreferencesHelper.saveSharedPref(
                CITY_NAME_SHARED_PREF,
                city);

        this.sharedPreferencesHelper.saveSharedPref(
                COUNTRY_NAME_SHARED_PREF,
                country
        );

        this.sharedPreferencesHelper.saveSharedPref(
                DOWNLOADER_TYPE_SHARED_PREF,
                WeatherDownloadersManager.BY_NAME_DOWNLOADER
        );
    }

    @Override
    public void setCurrentLocation() {
        Toast.makeText(getApplicationContext(),
                getResources().getString(R.string.start_setting_current_location_message),
                Toast.LENGTH_SHORT)
                .show();

        this.centralControl.setByCoordinatesWeatherDownloader();

        this.sharedPreferencesHelper.saveSharedPref(
                DOWNLOADER_TYPE_SHARED_PREF,
                WeatherDownloadersManager.BY_COORDINATES_DOWNLOADER
        );

        requestForLocationUpdate();
    }

    private void initListenersLists() {
        this.dailyPropositionsChangedListeners = new LinkedList<>();
        this.weeklyPropositionsChangedListeners = new LinkedList<>();
    }

    private void initEmptyPropositionsList() {
        this.dailyPropositions = new PropositionsList();
        this.weeklyPropositions = new PropositionsList();
    }

    private void createManagers() {
        this.sharedPreferencesHelper = SharedPreferencesHelper.create(this);
        this.centralControl = getCentralControl(this.sharedPreferencesHelper);
        this.locationManager = getLocationManager(getApplicationContext());
        this.locationTracker = new LocationTracker(this,
                this.sharedPreferencesHelper.getSharedPref(LONGITUTE_SHARED_PREF,
                        (float)DEFAULT_LONGITUDE),
                this.sharedPreferencesHelper.getSharedPref(LATITUDE_SHARED_PREF,
                        (float)DEFAULT_LATITUDE)
        );
    }

    private void restorePreviousStateIfAny(Bundle savedInstanceState) {
        if(savedInstanceState == null) {
            FacebookSdk.sdkInitialize(this);
            setFragment(new LoginFragment() ,false);
        }
    }

    private void refreshAll() {
        this.centralControl.setLocation(
                this.locationTracker.getLongitude(),
                this.locationTracker.getLatitude()
        );

        this.mLoadingIndicator.setVisibility(View.VISIBLE);
        this.centralControl.updateWeatherForecastAsync();
    }

    private void setViewReferences() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        this.mDrawerLayout = getDrawerLayout(toolbar);

        this.mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        this.mNavigationView.setNavigationItemSelectedListener(this);
        this.mNavigationView.setItemIconTintList(null);
    }

    private DrawerLayout getDrawerLayout(Toolbar toolbar) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.main_content);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        return drawer;
    }

    private CentralControl getCentralControl(SharedPreferencesHelper sharedPreferencesHelper) {
        CentralControl res = new CentralControl(getApplicationContext());
        res.setDailyPropositionsChangedListener(this);
        res.setWeeklyPropositionsChangedListener(this);
        res.setAddChosenHourListener(this);
        res.setUpdatingFinishedListener(this);

        res.setLocation(sharedPreferencesHelper.getSharedPref(LONGITUTE_SHARED_PREF, (float)DEFAULT_LONGITUDE),
                sharedPreferencesHelper.getSharedPref(LATITUDE_SHARED_PREF, (float)DEFAULT_LATITUDE));
        res.setLocation(sharedPreferencesHelper.getSharedPref(CITY_NAME_SHARED_PREF, DEFAULT_CITY_NAME),
                sharedPreferencesHelper.getSharedPref(COUNTRY_NAME_SHARED_PREF, DEFAULT_COUNTRY_NAME));

        switch(sharedPreferencesHelper.getSharedPref(DOWNLOADER_TYPE_SHARED_PREF, DEFAULT_DOWNLOADER_TYPE)) {
            case WeatherDownloadersManager.BY_COORDINATES_DOWNLOADER:
                res.setByCoordinatesWeatherDownloader();
                break;
            case WeatherDownloadersManager.BY_NAME_DOWNLOADER:
                res.setByNameWeatherDownloader();
                break;

        }

        return res;
    }

    private LocationManager getLocationManager(Context context) {
        return (LocationManager)context
                .getSystemService(LOCATION_SERVICE);
    }

    private void requestForLocationUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET}, REQUEST_LOCATION_PERMISSIONS_CODE);
        } else {
            updateLocation();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void updateLocation() {
        this.locationTracker.updateLocation(this.locationManager);
    }

    private void setFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.replace(R.id.main_fragment, fragment);
        fragmentTransaction.commit();
    }
    //Overload for Preference Fragment
    private void setFragment(android.preference.PreferenceFragment fragment, boolean addToBackStack) {
        android.app.FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        if (addToBackStack) {
            mFragmentTransaction.addToBackStack(null);
        }
        mFragmentTransaction.replace(android.R.id.content, fragment);
        mFragmentTransaction.commit();
    }
}