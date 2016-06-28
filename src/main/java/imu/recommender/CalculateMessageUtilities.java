package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RoutingRequest;
import com.mongodb.*;
import com.mongodb.util.JSON;
//import com.sun.xml.internal.fastinfoset.util.StringArray;
import org.bitpipeline.lib.owm.OwmClient;
import org.bitpipeline.lib.owm.WeatherData;
import org.bitpipeline.lib.owm.WeatherStatusResponse;
import sun.font.TrueTypeFont;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class CalculateMessageUtilities {

    public static String calculate(RouteFormatRoot route, Route trip) throws Exception {
        //Get trip properties
        Integer route_distance=trip.getDistanceMeters();
        //String city = route.getAttribute("city");
        String city = route.getCoordinateReferenceSystem();
        //Object cord = route.getRequest
        BigDecimal lat = trip.getFrom().getCoordinate().geometry.coordinates.get(0);
        BigDecimal lon = trip.getFrom().getCoordinate().geometry.coordinates.get(1);
        Integer duration = trip.getDurationSeconds();

        //Connect to mongodb
        MongoClient mongo = new MongoClient("euprojects.net",3368);
        //Print all database names
        //System.out.println(mongo.getDatabaseNames());
        DB db = mongo.getDB("Optimum");
        DBCollection table = db.getCollection("OptimumMessages");
        //Select the messages where persuasive strategy is Reward
        BasicDBObject searchQuery = new BasicDBObject();

        //searchQuery.append("persuasive_strategy", "Reward");

        List<String> contextList = new ArrayList<String>();
        contextList.add("noContext");

        /*System.out.println(route.getLocationFrom());
        System.out.println(route.getLocationTo());
        System.out.println(route.getAttribute("city"));
        System.out.println(route.getAttributes());*/



        //Check if the distance of route is walking
        if(withinWalkingDistance(route_distance)){
            System.out.println("Walking Distance");
            contextList.add("WalkingDistance");
        }

        if(withinBikeDistance(route_distance)){
            System.out.println("Bike Distance");
            contextList.add("BikeDistance");

        }

        //Check if the weather is nice
        if(NiceWeather(city)){
            System.out.println("Nice Weather");
            contextList.add("Nice Weather");
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
        /*JsonTrip carTrip = CarTrip(route);
        if ( carTrip!= null && trip.getModality().equals("pt")) {
            Integer driving_distance = carTrip.getDistanceMeter();
            if (CostComparetoDriving("transport", "drive")) {
                contextList.add("Cost");
            }
            Integer driving_duration = carTrip.getDurationSeconds();
            if (DurationComparetoDriving(duration, driving_duration)) {
                contextList.add("Duration");
            }
        }*/

        /*
        if (EmissionComparetoOthers("user")){
            contextList.add("EmissionComparetoOthers");
        } */
        searchQuery.append("context",new BasicDBObject("$in", contextList));

        //Find all messages after filtering
        DBCursor cursor = table.find(searchQuery);

        List<Message>  messages= new ArrayList<Message>();
        Message message;
        Double max_message_utility = 0.0;
        String selected_message_text= "";

        while (cursor.hasNext()) {
            BasicDBObject obj = (BasicDBObject) cursor.next();
            //Calculate utility for each Message
            message = new Message();
            String messageId = obj.getString("id");
            message.setMessageId(messageId);
            String message_text= obj.getString("message_text");
            message.setMessageText(message_text);
            //Set random messageUtility
            Double messageUtility = Math.random();
            message.setUtility(messageUtility);
            if (messageUtility > max_message_utility) {
                max_message_utility = messageUtility;
                selected_message_text = message_text;
            }
            messages.add(message);
        }

        System.out.println(messages);
        System.out.println(selected_message_text);
        System.out.println(max_message_utility);

        /*Set<String> tables = db.getCollectionNames();

        for(String coll : tables){
            System.out.println(coll);
        } */
        return selected_message_text;

    }

    private static boolean withinWalkingDistance(int distance) {
        return (distance<1000);

    }
    private static boolean withinBikeDistance(int distance) {
        return(distance<3000);
    }
    private  static boolean NiceWeather(String city) throws Exception {

        OwmClient owm = new OwmClient ();

        //WeatherStatusResponse currentWeather = owm.currentWeatherAtCity(lat ,lon,1);
        WeatherStatusResponse currentWeather = owm.currentWeatherAtCity(city);

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

    /*private static JsonTrip CarTrip(JsonResponseRoute route){
        JsonTrip cartrip = null;
        for (int i = 0; i < route.getTrips().size(); i++) {
            if (route.getTrips().get(i).getModality().equals("car")) {
                cartrip = route.getTrips().get(i);
            }

        }
        return cartrip;
    }*/


}

