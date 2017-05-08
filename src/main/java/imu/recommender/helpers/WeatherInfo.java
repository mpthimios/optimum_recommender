package imu.recommender.helpers;

import imu.recommender.models.weather.Weather;
import org.apache.log4j.Logger;
import org.bitpipeline.lib.owm.WeatherStatusResponse;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class WeatherInfo implements ServletContextListener {

    private static String weatherId;

    private static Logger logger = Logger.getLogger(WeatherInfo.class);

    public static boolean isWeatherNice(String city, Datastore mongoDatastore) throws Exception {

        //Find the last inserted weather Data

        Query<Weather> query = mongoDatastore.createQuery(Weather.class);
        query.criteria("country").equal(city);
        List<Weather> weathers = query.asList();
        return weathers.get(weathers.size()-1).getGoodWeather();
    }

    public static boolean isHistoricalWeatherNice(Float lat, Float lon, Integer start, Integer end) throws Exception {

        //owm.setAPPID("e0f5c1a1d86a8bd69e497197804d411c");
        //WeatherStatusResponse currentWeather = owm.currentWeatherAtCity("Tokyo", "JP");
        WeatherStatusResponse currentWeather;

        String url = "http://history.openweathermap.org/data/2.5/history/city?lat="+lat+"&lon="+lon+"&type=hour&start="+start+"&end="+end+"&appid="+GetProperties.getweatherId();

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        //con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        System.out.println(response.toString());
        JSONObject js = new JSONObject(response.toString());

        try {
            if(js.get("weather").toString().contains("Rain") ) {
                return false;
            }
            else{
                return true;
            }

        }catch (Exception e){
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
            return true;
        }

    }
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        //test
        try{
            ServletContext sc = servletContextEvent.getServletContext();
            String weatherId = sc.getInitParameter("weatherId");
            this.weatherId = weatherId;

        }
        catch (Exception e){
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
        }
    }
	
}
