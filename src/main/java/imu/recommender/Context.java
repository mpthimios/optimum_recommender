package imu.recommender;

import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.WeatherInfo;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import org.mongodb.morphia.Datastore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evangelie on 22/12/2016.
 */
public class Context {
    public static List<String> getRelevantContextForUser(Recommender route, RouteModel trip, User user, Datastore mongoDatastore) throws Exception {
        //Get trip properties
        Integer route_distance = trip.getRoute().getDistanceMeters();
        Float lat = trip.getRoute().getFrom().getCoordinate().geometry.coordinates.get(0).floatValue();
        Float lon = trip.getRoute().getFrom().getCoordinate().geometry.coordinates.get(1).floatValue();
        String city = "Vienna";
        //String city = trip.getFrom().getAddress().get().getCity().get();
        Integer duration = trip.getRoute().getDurationSeconds();

        //Connect to mongodb
        //Datastore mongoDatastore = MongoConnectionHelper.getMongoDatastore();

        //Get personality of user
        String personality = user.getUserPersonalityType(user.getId(), mongoDatastore);
        //Get the most convincing persuasive strategy
        List<String> strategies = user.getBestPersuasiveStrategy(personality);
        String strategy = strategies.get(0);
        System.out.println(strategy);

        List<String> targetList = new ArrayList<String>();
        //Select the messages that the target of message is the same with the mode of route
        String target = trip.getRoute().getAdditionalInfo().get("mode").toString();
        //targetList.add("all");
        //targetList.add("pt");
        //targetList.add(trip.getRoute().getAdditionalInfo().get("mode").toString());

        List<String> contextList = new ArrayList<String>();

        //If too many pt and car routes contexts are false then add the context based on response of user
        if (!user.tooManyPublicTransportRoutes() && !user.tooManyCarRoutes() ) {
            //if user PreferredMode is car, add context
            if (user.getPreferredMode().equals("car")) {
                contextList.add("TooManyCarRoutes");
            }
            //if user PreferredMode is pt, add context
            if (user.getPreferredMode().equals("pt")) {
                contextList.add("TooManyTransportRoutes");
            }
        }

        //Check if the distance of route is walking
        if (withinWalkingDistance(route_distance)) {
            System.out.println("Walking Distance");
            contextList.add("WalkingDistance");
        }

        //Check if the distance of route is biking
        if (withinBikeDistance(route_distance)) {
            System.out.println("Bike Distance");
            contextList.add("BikeDistance");

        }
        if (WeatherInfo.isWeatherNice(lat, lon, city)) {
            System.out.println("Nice Weather");
            contextList.add("NiceWeather");
        }

        //Check the weather if withinBikeDistance or withinWalkingDistance is True
       /* if(withinWalkingDistance(route_distance) || withinBikeDistance(route_distance) ) {
            if (WeatherInfo.isWeatherNice(lat, lon, city)) {
                System.out.println("Nice Weather");
                contextList.add("NiceWeather");
            }
        }*/

        if (user.tooManyPublicTransportRoutes()) {
            contextList.add("TooManyTransportRoutes");
        }
        if (user.tooManyCarRoutes()) {
            contextList.add("TooManyCarRoutes");
        }
        RouteModel carTrip = CarTrip(route);
        if (carTrip != null && trip.getRoute().getAdditionalInfo().get("mode").equals("pt")) {
            Integer driving_distance = carTrip.getRoute().getDistanceMeters();
            if (CostComparetoDriving("transport", "drive")) {
                contextList.add("Cost");
            }
            Integer driving_duration = carTrip.getRoute().getDurationSeconds();
            if (DurationComparetoDriving(duration, driving_duration)) {
                contextList.add("Duration");
            }
        }

        if (EmissionComparetoOthers(user)) {
            contextList.add("emissionsIncreasing");
        }

        return contextList;
    }

    public static Boolean GetRelevantContext(String target, List<String> context){
        Boolean RelevantContext = Boolean.TRUE;
        if (target.equals("walk")){
            if ( !context.contains("WalkDistance")  && !context.contains("Duration") && !context.contains("TooManyCarRoutes") && !context.contains("emissionsIncreasing") && !context.contains("NiceWeather") && context.contains("TooManyTransportRoutes")){
                RelevantContext = Boolean.FALSE;
            }
        }
        else if(target.equals("bicycle") || target.equals("bikeSharing")){
            if ( !context.contains("BikeDistance") && !context.contains("Duration") && !context.contains("TooManyCarRoutes") && !context.contains("emissionsIncreasing") && !context.contains("NiceWeather") && context.contains("TooManyTransportRoutes")){
                RelevantContext = Boolean.FALSE;
            }
        }
        else if(target.equals("bike&ride")){
            if ( !context.contains("Duration") && !context.contains("TooManyCarRoutes") && !context.contains("emissionsIncreasing") && !context.contains("NiceWeather") && context.contains("TooManyTransportRoutes")){
                RelevantContext = Boolean.FALSE;
            }
        }
        else if(target.equals("pt") || target.equals("park&ride")){
            if ( !context.contains("Duration") && !context.contains("TooManyCarRoutes") && !context.contains("emissionsIncreasing") && !context.contains("NiceWeather")){
                RelevantContext = Boolean.FALSE;
            }
        }

        return RelevantContext;

    }

    public static boolean withinWalkingDistance(int distance) {
        return (distance< GetProperties.getMaxWalkingDistance());

    }

    public static boolean withinBikeDistance(int distance) {
        return(distance< GetProperties.getMaxBikeDistance());

    }

    public static boolean CostComparetoDriving(String transport_route, String driving_route) {
        //get distance from routes and calculate cost
        Double transport_cost = 1.4;
        Double driving_cost = 5.0;
        return driving_cost - transport_cost >= 2.0;

    }

    public static boolean DurationComparetoDriving(Integer transport_duration, Integer driving_duration) {

        return transport_duration - driving_duration <=GetProperties.getDuration();

    }

    public static boolean EmissionComparetoOthers(User user) {
        try{
            double user_emissions = user.getEmissionsLastWeek();
            double average_emissions_of_others = user.getAverageEmissions();
            System.out.println(user_emissions);
            if (user_emissions/average_emissions_of_others>1.0){
                return true;
            }
            else {
                return false;
            }

        }
        catch (Exception e){
            return false;
        }

    }

    public static RouteModel CarTrip(Recommender route){
        RouteModel cartrip = null;
        for (int i = 0; i < route.getRoutes().size(); i++) {
            if (route.getRoutes().get(i).getRoute().getAdditionalInfo().get("mode").equals("car")) {
                cartrip = route.getRoutes().get(i);
            }

        }
        return cartrip;
    }
}
