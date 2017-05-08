package imu.recommender;

import com.mongodb.BasicDBObject;
import imu.recommender.helpers.GetProperties;
import imu.recommender.models.message.Message;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CalculateMessageUtilities {

	private static Logger logger = Logger.getLogger(CalculateMessageUtilities.class);

    public static String calculateForUser(List<String> contextList, User user, String target, Datastore mongoDatastore) throws Exception {

        //Get personality of user
        String personality = user.getUserPersonalityType(user.getId(), mongoDatastore);
        //Get the most convincing persuasive strategy
        List<String> strategies = user.getBestPersuasiveStrategy(personality);
        String strategy = strategies.get(0);
        //Get user language
        String lang = user.getLanguage();


        String selected_message_text= "";
        String selected_message_params= "";

        //Get the user percentages that are true
        List<String> PercentageList = new ArrayList<>();
        double emissions = user.getEmissionsLastWeek();
        if(emissions>0.1){
            PercentageList.add("CO2Em");
        }
        double PCar = user.getCarUsageComparedToOthers();
        double PWalkGW = user.getWalkUsageComparedToOthersGW();
        double PBikeGW = user.getBikeUsageComparedToOthersGW();
        double PPtGW = user.getPtUsageComparedToOthers();
        double MinWalked = user.getMinWalked();
        double MinBiked = user.getMinBiked();
        double MinPT = user.getMinPT();
        double PReduceDriving = user.getPercentageReduceDriving();
        double PWalkSD = user.getWalkUsageComparedToOthers();
        double PBikeSD = user.getBikeUsageComparedToOthers();

        //Initialize all percentage values in order to solve the cold start  problem

        if (PCar > GetProperties.getPCar()){
            PercentageList.add("PCar");
        }
        if (PWalkGW > GetProperties.getPWalkGW()){
            PercentageList.add("PWalkGW");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PWalkGW = 25.0 + (int)Math.round(result * 100)/(double)100;;
            PercentageList.add("PWalkGW");
        }
        if (PBikeGW > GetProperties.getPBikeGW()){
            PercentageList.add("PBikeGW");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PBikeGW = 25.0 + (int)Math.round(result * 100)/(double)100;;
            PercentageList.add("PBikeGW");
        }
        if (PPtGW > GetProperties.getPPtGW()){
            PercentageList.add("PPtGW");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PPtGW = 25.0 + (int)Math.round(result * 100)/(double)100;;
            PercentageList.add("PPtGW");
        }
        if (MinWalked > GetProperties.getMinWalked()){
            PercentageList.add("MinWalked");
        }
        if (MinBiked > GetProperties.getMinBiked()){
            PercentageList.add("MinBiked");
        }
        if (MinPT > GetProperties.getMinPT()){
            PercentageList.add("MinPT");
        }
        /*if (MinBikeSharing > GetProperties.getMinBikeSharing()){
            PercentageList.add("MinBikeSharing");
        }
        if (MinBikeRide > GetProperties.getMinBikeRide()){
            PercentageList.add("MinBikeRide");
        }
        if (MinParkRide > GetProperties.getMinParkRide()){
            PercentageList.add("MinParkRide");
        }*/
        if (PReduceDriving > 5.0){
            PercentageList.add("PReduceDriving");
        }
        if (PWalkSD > GetProperties.getPWalkGW()){
            PercentageList.add("PWalkSD");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PWalkSD = 50.0 + (int)Math.round(result * 100)/(double)100;
            PercentageList.add("PWalkSD");
        }
        if (PBikeSD > GetProperties.getPBikeGW()){
            PercentageList.add("PBikeSD");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PBikeSD = 50.0 + (int)Math.round(result * 100)/(double)100;;
            PercentageList.add("PBikeSD");
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
        logger.debug("found " + mes.size() + " messages that match the search criteria");
        //If no message exists change strategy(select the next more persuasive strategy)
        int i=0;
        while (mes.isEmpty() && i<strategies.size() ){
            strategy = strategies.get(i);
            logger.debug("message list is empty, trying next persuasive strategy: -" + strategy + "-");
            query = mongoDatastore.createQuery(Message.class);
            query.and(
                    query.criteria("persuasive_strategy").equal(strategy),
                    query.criteria("context").equal(new BasicDBObject("$in", contextList)),
                    query.criteria("parameters").equal(new BasicDBObject("$in", PercentageList)),
                    query.criteria("target").equal(target)
            );

            mes = query.asList();
            i++;
        }

        Double max_message_utility = 0.0;
        //Select a list of messages with the maximum utility based on context
        List<Message> messages = new ArrayList<>();
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
        Double max_message_utility2 = 0.0;
        for (Message m: messages ) {
            //Set random messageUtility
            Double messageUtility = Math.random();
            m.setUtility(messageUtility);
            if (messageUtility > max_message_utility2) {
                max_message_utility2 = messageUtility;
                if ("de".equals(lang)) {
                    selected_message_text = m.getMessage_text_german();
                }
                else if("sl".equals(lang)){
                    selected_message_text = m.getMessage_text_slo();
                }
                else {
                    selected_message_text = m.getMessage_text();
                }
                selected_message_params = m.getParameters();
            }
        }

        //Calculate the number of days since user login
        Timestamp endDate = new Timestamp(System.currentTimeMillis());
        Timestamp startDate;
        try {
            startDate = new Timestamp(user.getFistLogin().getTime());
        }
        catch (Exception e){
            startDate = Timestamp.valueOf("2017-02-21 10:51:55.415");
            logger.debug(e);
        }
        Integer days = Math.round(Math.abs( (endDate.getTime()-startDate.getTime())/86400000));
        String lastWeek = "last week";
        if (selected_message_text.contains(lastWeek)){
            if(days.equals(1) || days.equals(0)){
                selected_message_text = selected_message_text.replace(lastWeek, "last day");
            }
            if(days>=2 && days<=6){
                selected_message_text = selected_message_text.replace(lastWeek, "last "+days+"days");
            }
        }
        String LastWeek = "Last week";
        if (selected_message_text.contains(LastWeek)){
            if(days.equals(1) || days.equals(0)){
                selected_message_text = selected_message_text.replace(LastWeek, "Last day");
            }
            if(days>=2 && days<=6){
                selected_message_text = selected_message_text.replace(LastWeek, "Last "+days+"days");
            }
        }



        if ( !"no".equals(selected_message_params) ){
            if ("CO2Em".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getEmissionsLastWeek())));
            }
            if ("PCar".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getCarUsageComparedToOthers())));
            }
            if ("PWalkGW".equals(selected_message_params)){
                if(user.getWalkUsageComparedToOthersGW() > GetProperties.getPWalkGW()){
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getWalkUsageComparedToOthersGW())));
                }
                else {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(PWalkGW)));
                }
            }
            if ("PBikeGW".equals(selected_message_params)){
                if(user.getBikeUsageComparedToOthersGW() > GetProperties.getPBikeGW()) {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getBikeUsageComparedToOthersGW())));
                }
                else {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(PBikeGW)));
                }
            }
            if ("PPtGW".equals(selected_message_params)){
                if(user.getPtUsageComparedToOthers() > GetProperties.getPPtGW()) {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getPtUsageComparedToOthers())));
                }
                else {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(PPtGW)));
                }
            }
            if ("MinWalked".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getMinWalked())));
            }
            if ("MinBiked".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getMinBiked())));
            }
            if ("MinPT".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getMinPT())));
            }
            if ("PReduceDriving".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getPercentageReduceDriving())));
            }
            if ("PWalkSD".equals(selected_message_params)){
                if(user.getWalkUsageComparedToOthers() > GetProperties.getPWalkGW()) {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getWalkUsageComparedToOthers())));
                }
                else {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(PWalkSD)));
                }
            }
            if ("PBikeSD".equals(selected_message_params)){
                if(user.getBikeUsageComparedToOthers() > GetProperties.getPBikeGW()) {
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getBikeUsageComparedToOthers())));
                }
                else{
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(PBikeSD)));
                }
            }
        }

        logger.debug(selected_message_text);
        logger.debug("-"+strategy+"-");
        selected_message_text = selected_message_text + "_" +strategy;
        return selected_message_text;

    }

    private static double calculateUtility( String context, String mode, User user){
        double utility=0.0;
        String WalkDistance = "WalkDistance";
        String BikeDistance = "BikeDistance";
        String Duration = "Duration";
        String TooManyCarRoutes = "TooManyCarRoutes";
        String EmissionComparetoOthers = "EmissionComparetoOthers";
        String NiceWeather = "NiceWeather";
        String TooManyTransportRoutes = "TooManyTransportRoutes";
        String emissionsIncreasing = "emissionsIncreasing";


        if ("walk".equals(mode)) {
            if (user.tooManyCarRoutes()) {
                if (WalkDistance.equals(context)) {
                    utility = 0.4218;
                }
                if (Duration.equals(context)) {
                    utility = 0.3228;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.0456;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0777;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1321;
                }
            }
            if (user.tooManyPublicTransportRoutes()) {
                if (WalkDistance.equals(context)) {
                    utility = 0.4074;
                }
                if (Duration.equals(context)) {
                    utility = 0.3157;
                }
                if (TooManyTransportRoutes.equals(context)) {
                    utility = 0.0353;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0776;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.164;
                }
            } else {
                if (WalkDistance.equals(context)) {
                    utility = 0.4;
                }
                if (Duration.equals(context)) {
                    utility = 0.3;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.1;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1;
                }

            }
        }
        else if ("bicycle".equals(mode) || "bikeSharing".equals(mode)) {
            if (user.tooManyCarRoutes()) {
                if (BikeDistance.equals(context)) {
                    utility = 0.422;
                }
                if (Duration.equals(context)) {
                    utility = 0.3228;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.0456;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0777;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1321;
                }
            }
            if (user.tooManyPublicTransportRoutes()) {
                if (BikeDistance.equals(context)) {
                    utility = 0.4074;
                }
                if (Duration.equals(context)) {
                    utility = 0.3157;
                }
                if (TooManyTransportRoutes.equals(context)) {
                    utility = 0.0353;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0776;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.164;
                }
            } else {
                if (BikeDistance.equals(context)) {
                    utility = 0.4;
                }
                if (Duration.equals(context)) {
                    utility = 0.3;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.1;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.2;
                }

            }
        }
        else if("bike&ride".equals(mode)) {
            if (user.tooManyCarRoutes()) {
                if (Duration.equals(context)) {
                    utility = 0.5152;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.0901;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.179;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.2157;
                }
            }
            if (user.tooManyPublicTransportRoutes()) {
                if (Duration.equals(context)) {
                    utility = 0.5193;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.049;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.1958;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.2359;
                }
            } else {
                if (BikeDistance.equals(context)) {
                    utility = 0.422;
                }
                if (Duration.equals(context)) {
                    utility = 0.3228;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0777;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1321;
                }
            }
        }
        else if ("pt".equals(mode)) {
            if (Duration.equals(context)) {
                utility = 0.5125;
            }
            if (emissionsIncreasing.equals(context)) {
                utility = 0.315;
            }
            if (NiceWeather.equals(context)) {
                utility = 0.0775;
            }
            if (TooManyCarRoutes.equals(context)) {
                utility = 0.0949;
            }

        }
        else if ("park&ride".equals(mode)){
            if (Duration.equals(context)) {
                utility = 0.5152;
            }
            if (EmissionComparetoOthers.equals(context)) {
                utility = 0.179;
            }
            if (NiceWeather.equals(context)) {
                utility = 0.2157;
            }
            if (TooManyCarRoutes.equals(context)) {
                utility = 0.0901;
            }
        }

        return utility;
    }


}

