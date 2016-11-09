package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RoutingRequest;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.helpers.WeatherInfo;
import imu.recommender.models.message.Message;
import imu.recommender.models.user.User;

import com.mongodb.*;
import com.mongodb.util.JSON;

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


public class CalculateMessageUtilities {

    public static String calculateForUser(RouteFormatRoot route, Route trip, User user) throws Exception {
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
        targetList.add("pt");
        //targetList.add(trip.getAdditionalInfo().get("mode").toString());

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
        /*if(withinWalkingDistance(route_distance) || withinBikeDistance(route_distance) ) {
            if (WeatherInfo.isWeatherNice(lat, lon, city)) {
                System.out.println("Nice Weather");
                contextList.add("NiceWeather");
            }
        }*/
        /*if (emissionsIncreasing("user")){
            searchQuery.append("context", "emissionsIncreasing");
        }*/
        if (user.tooManyPublicTransportRoutes()){
            contextList.add("TooManyTransportRoutes");
        }
        if (user.tooManyCarRoutes()){
            contextList.add("TooManyCarRoutes");
        }
        /*Route carTrip = CarTrip(route);
        if ( carTrip!= null && trip.getAdditionalInfo().get("mode").equals("pt")) {
            Integer driving_distance = carTrip.getDistanceMeters();
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

        //Find all messages after filtering

        //Message m = new Message();
        //mongoDatastore.save(m);
        contextList.add("NiceWeather");
        System.out.println(contextList);
        Query<Message> query = mongoDatastore.createQuery(Message.class);
        query.and(
                query.criteria("persuasive_strategy").equal("suggestion"),
                query.criteria("context").equal("NiceWeather"),
                query.criteria("className").equal("imu.recommender.models.message.Message")
                //query.criteria("")
                //query.criteria("context").equal(new BasicDBObject("$in", contextList))
                //query.criteria("target").equal(new BasicDBObject("$in", targetList))
        );

        List<Message> mes = query.asList();
        System.out.println(mes);
        Double max_message_utility = 0.0;
        String selected_message_text= "";
        //Calculate utility for each Message
        for (Message message : mes ) {
            System.out.println(message.getMessage_text());
            //Set random messageUtility
            Double messageUtility = Math.random();
            message.setUtility(messageUtility);
            if (messageUtility > max_message_utility) {
                max_message_utility = messageUtility;
                selected_message_text = message.getMessage_text();
            }
        }
        System.out.println(selected_message_text);
        return selected_message_text;

    }

    private static boolean withinWalkingDistance(int distance) {
        return (distance<1000);

    }
    private static boolean withinBikeDistance(int distance) {
        return(distance<3000);
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

