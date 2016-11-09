package imu.recommender.helpers;

import org.bitpipeline.lib.owm.OwmClient;
import org.bitpipeline.lib.owm.WeatherData;
import org.bitpipeline.lib.owm.WeatherStatusResponse;


public class WeatherInfo {
	
	public static boolean isWeatherNice(Float lat, Float lon, String city) throws Exception {

        OwmClient owm = new OwmClient ();
        //WeatherStatusResponse currentWeather = owm.currentWeatherAtCity(lat ,lon,1);
        WeatherStatusResponse currentWeather = owm.currentWeatherAtCity("Wien");

        if (currentWeather.hasWeatherStatus ()) {

            WeatherData weather = currentWeather.getWeatherStatus ().get (0);
            if (weather.getPrecipitation () == Integer.MIN_VALUE) {
                WeatherData.WeatherCondition weatherCondition = weather.getWeatherConditions ().get (0);
                String description = weatherCondition.getDescription ();
                if (description.contains ("rain") || description.contains ("shower"))
                    System.out.println ("No rain measures in "+city+" but reports of " + description);
                else
                    System.out.println ("No rain measures in "+city+ ": " + description);
                return true;
            } else
                System.out.println ("It's raining in "+city+": " + weather.getPrecipitation () + " mm/h");
            return false;

        }
        else
            System.out.println("No info about weather.");
        return false;
    }
	
}
