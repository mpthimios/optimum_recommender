package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RoutingRequest;
import com.mongodb.*;
import com.mongodb.util.JSON;
//import com.sun.xml.internal.fastinfoset.util.StringArray;
import imu.recommender.models.User;
import org.bitpipeline.lib.owm.OwmClient;
import org.bitpipeline.lib.owm.WeatherData;
import org.bitpipeline.lib.owm.WeatherStatusResponse;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import sun.font.TrueTypeFont;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import imu.recommender.models.Message;


public class CalculateMessageUtilities {

    public static String calculate(RouteFormatRoot route, Route trip) throws Exception {
        //Get trip properties
        Integer route_distance=trip.getDistanceMeters();
        Float lat = trip.getFrom().getCoordinate().geometry.coordinates.get(0).floatValue();
        Float lon = trip.getFrom().getCoordinate().geometry.coordinates.get(1).floatValue();
        String city = "Vienna";
        //String city = trip.getFrom().getAddress().get().getCity().get();
        Integer duration = trip.getDurationSeconds();

        //Connect to mongodb
        Datastore mongoDatastore = MongoConnectionHelper.getMongoDatastore();

        List<String> targetList = new ArrayList<String>();
        //Select the messages that the target of message is the same with the mode of route
        targetList.add("all");
        targetList.add(trip.getAdditionalInfo().get("mode").toString());

        List<String> contextList = new ArrayList<String>();
        contextList.add("noContext");

        //Check if the distance of route is walking
        if(withinWalkingDistance(route_distance)){
            System.out.println("Walking Distance");
            contextList.add("WalkingDistance");
        }

        //Check if the distance of route is biking
        if(withinBikeDistance(route_distance)){
            System.out.println("Bike Distance");
            contextList.add("BikeDistance");

        }

        //Check the weather if withinBikeDistance or withinWalkingDistance is True
        if(withinWalkingDistance(route_distance) || withinBikeDistance(route_distance) ) {
            if (NiceWeather(lat, lon, city)) {
                System.out.println("Nice Weather");
                contextList.add("NiceWeather");
            }
        }
        /*if (emissionsIncreasing("user")){
            searchQuery.append("context", "emissionsIncreasing");
        }*/
        if (tooManyPublicTransportRoutes("user")){
            contextList.add("TooManyTransportRoutes");
        }
        if (tooManyCarRoutes("user")){
            contextList.add("TooManyCarRoutes");
        }
        Route carTrip = CarTrip(route);
        if ( carTrip!= null && trip.getAdditionalInfo().get("mode").equals("pt")) {
            Integer driving_distance = carTrip.getDistanceMeters();
            if (CostComparetoDriving("transport", "drive")) {
                contextList.add("Cost");
            }
            Integer driving_duration = carTrip.getDurationSeconds();
            if (DurationComparetoDriving(duration, driving_duration)) {
                contextList.add("Duration");
            }
        }

        /*
        if (EmissionComparetoOthers("user")){
            contextList.add("EmissionComparetoOthers");
        } */

        //Find all messages after filtering

        //Message m = new Message();
        //mongoDatastore.save(m);
        contextList.add("NiceWeather");
        System.out.println(contextList);
        Query<Message> query = mongoDatastore.createQuery(Message.class);
        query.and(
                query.criteria("persuasive_strategy").equal("suggestion"),
                query.criteria("context").equal(new BasicDBObject("$in", contextList))
                //query.criteria("target").equal(new BasicDBObject("$in", targetList))
        );

        List<Message> mes = query.asList();
        System.out.println(mes);
        Double max_message_utility = 0.0;
        String selected_message_text= "";
        //Calculate utility for each Message
        for (Message message : mes) {
            System.out.println(message.getMessage_text());
            //Set random messageUtility
            Double messageUtility = Math.random();
            message.setUtility(messageUtility);
            if (messageUtility > max_message_utility) {
                max_message_utility = messageUtility;
                selected_message_text = message.getMessage_text();
            }
        }

        return selected_message_text;

    }

    private static boolean withinWalkingDistance(int distance) {
        return (distance<1000);

    }
    private static boolean withinBikeDistance(int distance) {
        return(distance<3000);
    }
    private  static boolean NiceWeather(Float lat, Float lon, String city) throws Exception {

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
    private static boolean emissionsIncreasing(String user) {
        //
        return true;

    }
    private static boolean tooManyPublicTransportRoutes(String user) {
        //Get percentage_of_public_transport_use_last_period from mongodb
        Double percentage_of_public_transport_use_last_period = 0.9;
        return percentage_of_public_transport_use_last_period>0.6;

    }
    private static boolean tooManyCarRoutes(String user) {
        //Get percentage_of_car_use_last_period from mongodb
        Double percentage_of_car_use_last_period = 0.4;
        return percentage_of_car_use_last_period>0.6;
    }

    private static boolean CostComparetoDriving(String transport_route, String driving_route) {
        //get distance from routes and calculate cost
        Double transport_cost = 1.4;
        Double driving_cost = 5.0;
        return driving_cost - transport_cost >= 2.0;

    }

    private static boolean DurationComparetoDriving(Integer transport_duration, Integer driving_duration) {

        return transport_duration - driving_duration <=5;

    }

    private static boolean EmissionComparetoOthers(String user) {

        return true;

    }

    private static Route CarTrip(RouteFormatRoot route){
        Route cartrip = null;
        for (int i = 0; i < route.getRoutes().size(); i++) {
            if (route.getRoutes().get(i).getAdditionalInfo().get("mode").equals("car")) {
                cartrip = route.getRoutes().get(i);
            }

        }
        return cartrip;
    }


}

