package imu.recommender.models.weather;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;

/**
 * Created by evangelia on 3/29/2017.
 */

@Entity("WeatherInfo")

public class Weather {
    @Id
    private ObjectId id;
    private String country;
    private DBObject weatherInfo;
    private Boolean GoodWeather;
    private Date createdDate;

    public Weather(){

        //initialize variables
        this.country = "";
        this.GoodWeather = Boolean.FALSE;
        this.createdDate = new Date();

    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public DBObject getWeatherInfo() {
        return weatherInfo;
    }

    public void setWeatherInfo(DBObject weatherInfo) {
        this.weatherInfo = weatherInfo;
    }

    public Boolean getGoodWeather() {
        return GoodWeather;
    }

    public void setGoodWeather(Boolean goodWeather) {
        GoodWeather = goodWeather;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
