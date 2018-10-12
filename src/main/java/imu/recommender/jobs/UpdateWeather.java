package imu.recommender.jobs;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.weather.Weather;
import org.apache.log4j.Logger;
import org.bitpipeline.lib.owm.WeatherStatusResponse;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by evangelia on 3/29/2017.
 */
public class UpdateWeather implements Job{
    private Logger logger = Logger.getLogger(UpdateWeather.class);

    @Override
    public void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        HttpURLConnection con;
        Datastore mongoDatastore;

        try {
            mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
            return;
        }

        //owm.setAPPID("e0f5c1a1d86a8bd69e497197804d411c");
        //WeatherStatusResponse currentWeather = owm.currentWeatherAtCity("Tokyo", "JP");
        WeatherStatusResponse currentWeather;
        ArrayList<String> Countries = new ArrayList<>();
        Countries.add("Vienna");
        Countries.add("Birmingham,GB");
        Countries.add("Ljubljana");

        try {

            for (int i=0;i<Countries.size();i++){
                //String url = "http://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&appid="+ GetProperties.getweatherId();
                String url = "http://api.openweathermap.org/data/2.5/weather?q="+Countries.get(i)+"&appid="+ GetProperties.getweatherId();

                URL obj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

                // optional default is GET
                connection.setRequestMethod("GET");

                //add request header
                //con.setRequestProperty("User-Agent", USER_AGENT);

                int responseCode = connection.getResponseCode();
                System.out.println("\nSending 'GET' request to URL : " + url);
                System.out.println("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                System.out.println(response.toString());
                JSONObject js = new JSONObject(response.toString());
                //System.out.println(js.getJSONArray("weather").get(1));
                Boolean GoodWeather = Boolean.FALSE;
                try {
                    if(js.get("weather").toString().contains("Rain") || js.get("weather").toString().contains("rain")) {
                        GoodWeather =  Boolean.FALSE;
                    }
                    else{
                        GoodWeather = Boolean.TRUE;
                    }

                }catch (Exception e){
                    GoodWeather = Boolean.TRUE;
                    logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
                }

                Weather weatherInfo = new Weather();
                weatherInfo.setWeatherInfo((DBObject) JSON.parse(response.toString()));
                if (Countries.get(i).equals("Birmingham,GB")){
                    String country="Birmingham";
                    weatherInfo.setCountry(country);
                }
                else {
                    weatherInfo.setCountry(Countries.get(i));
                }
                weatherInfo.setGoodWeather(GoodWeather);
                weatherInfo.setCreatedDate(new Date());
                mongoDatastore.save(weatherInfo);

            }

        }
        catch (Exception e){
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
        }

    }
}
