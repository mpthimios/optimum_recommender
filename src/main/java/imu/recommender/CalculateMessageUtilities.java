package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RoutingRequest;
import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.helpers.WeatherInfo;
import imu.recommender.models.message.Message;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.strategy.Strategy;
import imu.recommender.models.user.User;

import com.mongodb.*;
import com.mongodb.util.JSON;

import org.bitpipeline.lib.owm.OwmClient;
import org.bitpipeline.lib.owm.WeatherData;
import org.bitpipeline.lib.owm.WeatherForecastResponse;
import org.bitpipeline.lib.owm.WeatherStatusResponse;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.omg.CORBA.ContextList;
import sun.font.TrueTypeFont;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class CalculateMessageUtilities {

    public static String calculateForUser(Recommender route, RouteModel trip, User user) throws Exception {
        //Get trip properties
        Integer route_distance=trip.getRoute().getDistanceMeters();
        Float lat = trip.getRoute().getFrom().getCoordinate().geometry.coordinates.get(0).floatValue();
        Float lon = trip.getRoute().getFrom().getCoordinate().geometry.coordinates.get(1).floatValue();
        String city = "Vienna";
        //String city = trip.getFrom().getAddress().get().getCity().get();
        Integer duration = trip.getRoute().getDurationSeconds();

        //Get personality of user
        String personality = user.getUserPersonalityType(user.getId());
        //Get the most convincing persuasive strategy
        String strategy = user.getBestPersuasiveStrategy(personality);

        //Connect to mongodb
        Datastore mongoDatastore = MongoConnectionHelper.getMongoDatastore();

        List<String> targetList = new ArrayList<String>();
        //Select the messages that the target of message is the same with the mode of route
        String target = trip.getRoute().getAdditionalInfo().get("mode").toString();
        //targetList.add("all");
        //targetList.add("pt");
        //targetList.add(trip.getRoute().getAdditionalInfo().get("mode").toString());

        List<String> contextList = new ArrayList<String>();

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
        /*if (emissionsIncreasing("user")){
            searchQuery.append("context", "emissionsIncreasing");
        }*/
        if (user.tooManyPublicTransportRoutes()){
            contextList.add("TooManyTransportRoutes");
        }
        if (user.tooManyCarRoutes()){
            contextList.add("TooManyCarRoutes");
        }
        RouteModel carTrip = CarTrip(route);
        if ( carTrip!= null && trip.getRoute().getAdditionalInfo().get("mode").equals("pt")) {
            Integer driving_distance = carTrip.getRoute().getDistanceMeters();
            if (CostComparetoDriving("transport", "drive")) {
                contextList.add("Cost");
            }
            Integer driving_duration = carTrip.getRoute().getDurationSeconds();
            if (DurationComparetoDriving(duration, driving_duration)) {
                contextList.add("Duration");
            }
        }

        if (EmissionComparetoOthers(user)){
            contextList.add("EmissionComparetoOthers");
        }

        //Find all messages after filtering

        //Message m = new Message();
        //mongoDatastore.save(m);
        //contextList.add("NiceWeather");
        System.out.println(contextList);
        System.out.println(strategy);
        String selected_message_text= "";
        String selected_message_params= "";

        //Get the user percentages that are true
        List<String> PercentageList = new ArrayList<String>();
        double emissions = user.getEmissionsLastWeek();
        if(emissions>200){
            PercentageList.add("CO2Em");
        }
        double PCar = user.getCarUsageComparedToOthers();
        double PWalkGW = user.getWalkUsageComparedToOthers();
        double PBikeGW = user.getBikeUsageComparedToOthers();
        double PPtGW = user.getPtUsageComparedToOthers();
        if (PCar > GetProperties.getPCar()){
            PercentageList.add("PCar");
        }
        if (PWalkGW > GetProperties.getPWalkGW()){
            PercentageList.add("PWalkGW");
        }
        if (PBikeGW > GetProperties.getPBikeGW()){
            PercentageList.add("PBikeGW");
        }
        if (PPtGW > GetProperties.getPPtGW()){
            PercentageList.add("PPtGW");
        }
        PercentageList.add("no");
        Query<Message> query = mongoDatastore.createQuery(Message.class);
        query.and(
                query.criteria("persuasive_strategy").equal(strategy),
                query.criteria("context").equal(new BasicDBObject("$in", contextList)),
                query.criteria("parameters").equal(new BasicDBObject("$in", PercentageList)),
                query.criteria("target").equal(target)
        );

        List<Message> mes = query.asList();
        Double max_message_utility = 0.0;
        //Select a list of messages with the maximum utility based on context
        List<Message> messages = new ArrayList<Message>();
        for (Message message : mes ) {
            //Calculate message utility based on context.
            Double messageUtility = calculateUtility(message.getContext(), target, user);
            message.setUtility(messageUtility);

            if (messageUtility > max_message_utility) {
                max_message_utility = messageUtility;
                messages.clear();
                messages.add(message);
            }
            else if (messageUtility.equals(max_message_utility)){
                messages.add(message);
            }
        }
        //Select the message that will be displayed on the user
        for (Message m: messages ) {
            //Set random messageUtility
            Double messageUtility = Math.random();
            m.setUtility(messageUtility);
            if (messageUtility > max_message_utility) {
                max_message_utility = messageUtility;
                selected_message_text = m.getMessage_text();
                selected_message_params = m.getParameters();
            }
        }


        if ( !selected_message_params.equals("no") ){
            if (selected_message_params.equals("CO2Em")){
                selected_message_text = selected_message_text.replace("X", Double.toString(user.getEmissionsLastWeek()));
            }
            if (selected_message_params.equals("PCar")){
                selected_message_text = selected_message_text.replace("X", Double.toString(user.getCarUsageComparedToOthers()));
            }
            if (selected_message_params.equals("PWalkGW")){
                selected_message_text = selected_message_text.replace("X", Double.toString(user.getWalkUsageComparedToOthers()));
            }
            if (selected_message_params.equals("PBikeGW")){
                selected_message_text = selected_message_text.replace("X", Double.toString(user.getBikeUsageComparedToOthers()));
            }
            if (selected_message_params.equals("PPtGW")){
                selected_message_text = selected_message_text.replace("X", Double.toString(user.getPtUsageComparedToOthers()));
            }
        }


        System.out.println(selected_message_text);

        //increase the number_of_times_sent of the selected strategy
        Query<Strategy> strategyQuery = mongoDatastore.createQuery(Strategy.class).field("persuasive_strategy").equal((String) strategy);
        Integer number_of_times_sent = strategyQuery.get().getNumber_of_times_sent();
        number_of_times_sent ++;
        mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("number_of_times_sent", number_of_times_sent));
        //Increase the number of attemps for the current user
        Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(user.getId());
        Integer user_attempts;
        if (strategy.equals("suggestion ") ) {
            try {
                user_attempts = userQuery.get().getSugAttempts();
            }
            catch (Exception e){
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugAttempts", 0));
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugSuccess", 0));
                user_attempts = userQuery.get().getSugAttempts();
            }
            user_attempts = user_attempts + 1;
            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugAttempts", user_attempts));
        }
        else if (strategy.equals("comparison") ) {
            try {
                user_attempts = userQuery.get().getCompAttempts();
            }
            catch (Exception e){
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compAttempts", 0));
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", 0));
                user_attempts = userQuery.get().getCompAttempts();
            }
            user_attempts = user_attempts + 1;
            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compAttempts", user_attempts));
        }
        if (strategy.equals("self-monitoring") ) {
            try {
                user_attempts = userQuery.get().getSelfAttempts();
            }
            catch (Exception e){
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfAttempts", 0));
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfSuccess", 0));
                user_attempts = userQuery.get().getSelfAttempts();
            }
            user_attempts = user_attempts + 1;
            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfAttempts", user_attempts));
        }
        selected_message_text = selected_message_text + "_" +strategy;
        return selected_message_text;

    }

    public static boolean withinWalkingDistance(int distance) {
        return (distance<GetProperties.getMaxWalkingDistance());

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

    private static double calculateUtility( String context, String mode, User user){
        double utility=0.0;
        switch (mode) {
            case "walk":
                if (user.tooManyCarRoutes()) {
                    if (context.equals("WalkDistance")) {
                        utility = 0.4218;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3228;
                    }
                    if (context.equals("TooManyCarRoutes")) {
                        utility = 0.0456;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.0777;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.1321;
                    }
                }
                if (user.tooManyPublicTransportRoutes()) {
                    if (context.equals("WalkDistance")) {
                        utility = 0.4074;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3157;
                    }
                    if (context.equals("TooManyTransportRoutes")) {
                        utility = 0.0353;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.0776;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.164;
                    }
                } else {
                    if (context.equals("WalkDistance")) {
                        utility = 0.4;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.1;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.1;
                    }

                }
                break;
            case "bicycle":
            case "bikeSharing":
                if (user.tooManyCarRoutes()) {
                    if (context.equals("BikeDistance")) {
                        utility = 0.422;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3228;
                    }
                    if (context.equals("TooManyCarRoutes")) {
                        utility = 0.0456;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.0777;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.1321;
                    }
                }
                if (user.tooManyPublicTransportRoutes()) {
                    if (context.equals("BikeDistance")) {
                        utility = 0.4074;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3157;
                    }
                    if (context.equals("TooManyTransportRoutes")) {
                        utility = 0.0353;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.0776;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.164;
                    }
                } else {
                    if (context.equals("BikeDistance")) {
                        utility = 0.4;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.1;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.2;
                    }

                }
                break;
            case "bike&ride":
                if (user.tooManyCarRoutes()) {
                    if (context.equals("Duration")) {
                        utility = 0.5152;
                    }
                    if (context.equals("TooManyCarRoutes")) {
                        utility = 0.0901;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.179;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.2157;
                    }
                }
                if (user.tooManyPublicTransportRoutes()) {
                    if (context.equals("Duration")) {
                        utility = 0.5193;
                    }
                    if (context.equals("TooManyCarRoutes")) {
                        utility = 0.049;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.1958;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.2359;
                    }
                } else {
                    if (context.equals("BikeDistance")) {
                        utility = 0.422;
                    }
                    if (context.equals("Duration")) {
                        utility = 0.3228;
                    }
                    if (context.equals("EmissionComparetoOthers")) {
                        utility = 0.0777;
                    }
                    if (context.equals("NiceWeather")) {
                        utility = 0.1321;
                    }
                }
                break;
            case "pt":
                if (context.equals("Duration")) {
                    utility = 0.5125;
                }
                if (context.equals("EmissionComparetoOthers")) {
                    utility = 0.315;
                }
                if (context.equals("NiceWeather")) {
                    utility = 0.0775;
                }
                if (context.equals("TooManyCarRoutes")) {
                    utility = 0.0949;
                }

                break;
            case "park&ride":
                if (context.equals("Duration")) {
                    utility = 0.5152;
                }
                if (context.equals("EmissionComparetoOthers")) {
                    utility = 0.179;
                }
                if (context.equals("NiceWeather")) {
                    utility = 0.2157;
                }
                if (context.equals("TooManyCarRoutes")) {
                    utility = 0.0901;
                }
                break;
        }

        return utility;
    }


}

