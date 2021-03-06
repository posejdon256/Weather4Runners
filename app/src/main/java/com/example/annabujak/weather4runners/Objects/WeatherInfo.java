package com.example.annabujak.weather4runners.Objects;

import com.example.annabujak.weather4runners.Enum.Cloudiness;

import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Created by slowik on 24.04.2017.
 */

public class WeatherInfo {

    private long longDate; // UTC, unix epoch
    private long Id;
    private int Temperature;
    private int Humidity;
    private Cloudiness Cloudiness;
    private double Precipitation;
    private boolean isChecked;
    private double WindSpeed;
    private String IconName;
    private String Description ="";

    public WeatherInfo(int _Temperature, int _Humidity, Cloudiness _Cloudiness, double _Precipitation, long _Date, double _WindSpeed,
                       String _IconName, String _Description) {
        Temperature = _Temperature;
        Humidity = _Humidity;
        Cloudiness = _Cloudiness;
        Precipitation = _Precipitation;
        longDate = _Date;
        isChecked = false;
        WindSpeed = _WindSpeed;
        IconName = _IconName;
        Description = _Description;
    }

    public WeatherInfo getDeepCopy() {
        WeatherInfo res = new WeatherInfo(this.getTemperature(),
                this.getHumidity(),
                this.getCloudiness(),
                this.getPrecipitation(),
                this.getDate(),
                this.getWindSpeed(),
                this.getIconName(),
                this.getDescription());

        res.setIsChecked(this.getIsChecked());

        return res;
    }

    public int getTemperature() {
        return Temperature;
    }

    public int getHumidity() {
        return Humidity;
    }

    public Cloudiness getCloudiness() {return Cloudiness;}

    public double getPrecipitation() {
        return Precipitation;
    }

    public long getDate(){ return this.longDate;}

    public String getFormattedDate(SimpleDateFormat dateFormat) {
        return dateFormat.format(this.getDate());
    }

    public void setId(long id){Id = id;}
    public long getId(){return Id;}

    public void setIsChecked(boolean _isChecked){
        isChecked = _isChecked;
    }
    public boolean getIsChecked(){return isChecked;}

    public double getWindSpeed(){return WindSpeed;}

    public String getIconName(){return IconName;}
    public void setIconName(String _IconName){IconName = _IconName;}

    public String getDescription(){return Description;}
    public void setDescription(String _Description){Description = _Description;}

    public String getConditionsSummary() {
        return (Integer.valueOf(this.getTemperature())).toString() + "°C, "
                + Long.valueOf(Math.round(this.getWindSpeed())).toString() + " km/h, "
                + String.format("%.2f", this.getPrecipitation()) + " mm, "
                + Integer.valueOf(this.getHumidity()).toString() + " %";

    }


}
