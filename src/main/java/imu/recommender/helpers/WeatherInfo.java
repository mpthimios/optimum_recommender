package imu.recommender.helpers;

import org.bitpipeline.lib.owm.OwmClient;
import org.bitpipeline.lib.owm.WeatherStatusResponse;
import org.json.JSONObject;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherInfo implements ServletContextListener {

    private static String weatherId;

    public static boolean isWeatherNice(Float lat, Float lon, String city) throws Exception {

        OwmClient owm = new OwmClient();

        //owm.setAPPID("e0f5c1a1d86a8bd69e497197804d411c");
        //WeatherStatusResponse currentWeather = owm.currentWeatherAtCity("Tokyo", "JP");
        WeatherStatusResponse currentWeather;

        String url = "http://api.openweathermap.org/data/2.5/weather?lat="+lat+"&lon="+lon+"&appid="+GetProperties.getweatherId();
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
        System.out.println(js.get("weather").toString().contains("Rain") || js.get("weather").toString().contains("Rain"));
        //System.out.println(js.getJSONArray("weather").get(1));

        try {
            if(js.get("weather").toString().contains("Rain") || js.get("weather").toString().contains("Rain") ) {
                return false;
            }
            else{
                return true;
            }

        }catch (Exception e){
            return true;
        }

    }

    public static boolean isHistoricalWeatherNice(Float lat, Float lon, Integer start, Integer end) throws Exception {

        OwmClient owm = new OwmClient();

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
        System.out.println(js.get("weather").toString().contains("Rain") || js.get("weather").toString().contains("Rain"));
        //System.out.println(js.getJSONArray("weather").get(1));

        try {
            if(js.get("weather").toString().contains("Rain") || js.get("weather").toString().contains("Rain") ) {
                return false;
            }
            else{
                return true;
            }

        }catch (Exception e){
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
            e.printStackTrace();
        }
    }
	
}
