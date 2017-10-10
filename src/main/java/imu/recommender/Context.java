package imu.recommender;

import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.WeatherInfo;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import uk.recurse.geocoding.reverse.Country;
import uk.recurse.geocoding.reverse.ReverseGeocoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by evangelie on 22/12/2016.
 */
public class Context {

    public static final Logger logger = Logger.getLogger(Context.class);

    public static List<String> getRelevantContextForUser(Recommender route, RouteModel trip, User user, Datastore mongoDatastore) throws Exception {

    	String Duration = "Duration";
        String TooManyCarRoutes = "TooManyCarRoutes";       
        String NiceWeather = "NiceWeather";
        String TooManyTransportRoutes = "TooManyTransportRoutes";
        String emissionsIncreasing = "emissionsIncreasing";

        //Get trip properties
        Integer routeDistance = trip.getRoute().getDistanceMeters();

        //Get coordinates
        String[] coordinates = {
                trip.getRoute().getFrom().getCoordinate().getGeometry().getCoordinates().get().asNewList().get(0).toString(),
                trip.getRoute().getFrom().getCoordinate().getGeometry().getCoordinates().get().asNewList().get(1).toString()
        };

        String city = "Vienna";
        ReverseGeocoder geocoder = new ReverseGeocoder();
        Optional country = geocoder.getCountry(Double.parseDouble(coordinates[1]), Double.parseDouble(coordinates[0]));
        if (country.isPresent()) {
            String location = ((Country) country.get()).iso();
            logger.debug("Request from: " + location);
            if (location.matches("GB")) {
                city = "Birmingham";
            } else if (location.matches("AT")) {
                city = "Vienna";
            } else if (location.matches("SI")) {
                city = "Ljubljana";
            }
        }

        Integer duration = trip.getRoute().getDurationSeconds();

        //Get personality of user
        String personality = user.getUserPersonalityType(user.getId(), mongoDatastore);
        //Get the most convincing persuasive strategy
        List<String> strategies = user.getBestPersuasiveStrategy(personality);
        String strategy = strategies.get(0);
        logger.debug(strategy);

        List<String> contextList = new ArrayList<>();

        //If too many pt and car routes contexts are false then add the context based on response of user
        if (!user.tooManyPublicTransportRoutes() && !user.tooManyCarRoutes() ) {
            //if user PreferredMode is car, add context
            if ("car".equals(user.getPersonality().convertPreferredMode())) {
                contextList.add(TooManyCarRoutes);
            }
            //if user PreferredMode is pt, add context
            if ("pt".equals(user.getPersonality().convertPreferredMode())) {
                contextList.add(TooManyTransportRoutes);
            }
        }

        //Check if the distance of route is walking
        if (withinWalkingDistance(routeDistance)) {
            contextList.add("WalkingDistance");
        }

        //Check if the distance of route is biking
        if (withinBikeDistance(routeDistance)) {
            contextList.add("BikeDistance");

        }
        if (WeatherInfo.isWeatherNice(city, mongoDatastore)) {
            contextList.add(NiceWeather);
        }

        //Check the weather if withinBikeDistance or withinWalkingDistance is True
       /* if(withinWalkingDistance(routeDistance) || withinBikeDistance(routeDistance) ) {
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
        if (carTrip != null && "pt".equals(trip.getRoute().getAdditionalInfo().get("mode"))) {
            if (CostComparetoDriving("transport", "drive")) {
                contextList.add("Cost");
            }
            Integer drivingDuration = carTrip.getRoute().getDurationSeconds();
            if (DurationComparetoDriving(duration, drivingDuration)) {
                contextList.add(Duration);
            }
        }

        if (EmissionComparetoOthers(user)) {
            contextList.add(emissionsIncreasing);
        }

        return contextList;
    }

    public static Boolean GetRelevantContext(String target, List<String> context){
        Boolean relevantContext = Boolean.TRUE;
        String WalkDistance = "WalkingDistance";
        String BikeDistance = "BikeDistance";
        String Duration = "Duration";
        String TooManyCarRoutes = "TooManyCarRoutes";
        String NiceWeather = "NiceWeather";
        String TooManyTransportRoutes = "TooManyTransportRoutes";
        String emissionsIncreasing = "emissionsIncreasing";
        if ("walk".equals(target)){
            if ( !context.contains(WalkDistance)  && !context.contains(Duration) && !context.contains(TooManyCarRoutes) && !context.contains(emissionsIncreasing) && !context.contains(NiceWeather) && context.contains(TooManyTransportRoutes)){
                relevantContext = Boolean.FALSE;
            }
        }
        else if("bicycle".equals(target) || "bikeSharing".equals(target)){
            if ( !context.contains(BikeDistance) && !context.contains(Duration) && !context.contains(TooManyCarRoutes) && !context.contains(emissionsIncreasing) && !context.contains(NiceWeather) && context.contains(TooManyTransportRoutes)){
                relevantContext = Boolean.FALSE;
            }
        }
        else if("bike&ride".equals(target)){
            if ( !context.contains(Duration) && !context.contains(TooManyCarRoutes) && !context.contains(emissionsIncreasing) && !context.contains(NiceWeather) && context.contains(TooManyTransportRoutes)){
                relevantContext = Boolean.FALSE;
            }
        }
        else if("pt".equals(target) || "park&ride".equals(target)){
            if ( !context.contains(Duration) && !context.contains(TooManyCarRoutes) && !context.contains(emissionsIncreasing) && !context.contains(NiceWeather)){
                relevantContext = Boolean.FALSE;
            }
        }

        return relevantContext;

    }

    public static boolean withinWalkingDistance(int distance) {
        return distance< GetProperties.getMaxWalkingDistance();

    }

    public static boolean withinBikeDistance(int distance) {
        return distance< GetProperties.getMaxBikeDistance();

    }

    public static boolean CostComparetoDriving(String transport_route, String driving_route) {
        //get distance from routes and calculate cost
        Double transportCost = 1.4;
        Double drivingCost = 5.0;
        return drivingCost - transportCost >= 2.0;
    }

    public static boolean DurationComparetoDriving(Integer transportDuration, Integer drivingDuration) {

        return transportDuration - drivingDuration <=GetProperties.getDuration();

    }

    public static boolean EmissionComparetoOthers(User user) {
        try{
            double userEmissions = user.getEmissionsLastWeek();
            double averageEmissionsOfOthers = user.getAverageEmissions();
            logger.debug(userEmissions);
            return userEmissions/averageEmissionsOfOthers>1.0;

        }
        catch (Exception e){
            logger.debug(e);
            return false;
        }

    }

    public static RouteModel CarTrip(Recommender route){
        RouteModel cartrip = null;
        for (int i = 0; i < route.getRoutes().size(); i++) {
            if ("car".equals(route.getRoutes().get(i).getRoute().getAdditionalInfo().get("mode"))) {
                cartrip = route.getRoutes().get(i);
            }

        }
        return cartrip;
    }
}
