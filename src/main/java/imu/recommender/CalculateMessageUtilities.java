package imu.recommender;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import imu.recommender.helpers.GetProperties;
import imu.recommender.models.message.Message;
import imu.recommender.models.message.MessageThrottling;
import imu.recommender.models.strategy.Strategy;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.abs;

public class CalculateMessageUtilities {

	private static Logger logger = Logger.getLogger(CalculateMessageUtilities.class);

    public static String calculateForUser(List<String> contextList, User user, String target, Datastore mongoDatastore, double RewardPoints) throws Exception {

        //Get personality of user
        String personality = user.getUserPersonalityType(user.getId(), mongoDatastore);
        //Get the most convincing persuasive strategy
        List<String> strategies = user.getBestPersuasiveStrategy(personality);
        String strategy = strategies.get(0);
        //Get user language
        String lang = user.getLanguage();
        String pilot = user.getPilot();


        //Get the id of the displayed messages during last X hours
        List<String> DisplayedMessages = get_presented_messages(user.getId(),strategy, mongoDatastore);

        String selected_message_text= "";
        String selected_message_params= "";
        Object selectedMessageId ="";

        //Get the user percentages that are true
        List<String> PercentageList = new ArrayList<>();
        double emissions = user.getEmissionsLastWeek();
        if(emissions>0.1){
            PercentageList.add("CO2Em");
        }
        double PCar = user.getCarUsageComparedToOthers();
        double PWalkGW = user.getWalkUsageComparedToOthersGW();
        double PBikeGW = user.getBikeUsageComparedToOthersGW();
        double PPtGW = user.getPtUsageComparedToOthersGW();
        double MinWalked = user.getMinWalked();
        double MinBiked = user.getMinBiked();
        double MinPT = user.getMinPT();
        double PReduceDriving = user.getPercentageReduceDriving();
        double PWalkSD = user.getWalkUsageComparedToOthers();
        double PBikeSD = user.getBikeUsageComparedToOthers();

        //Initialize all percentage values in order to solve the cold start  problem

        //Get user reward points
        Double points = user.getPoints();
        if (RewardPoints>0.0){
            PercentageList.add("points");
        }

        if (contextList.contains("ReachingTarget")){
            PercentageList.add("prize");
        }

        if (PCar > GetProperties.getPCar()){
            PercentageList.add("PCar");
        }
        if (PWalkGW > GetProperties.getPWalkGW()){
            PercentageList.add("PWalkGW");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PWalkGW = 50.0 + (int)Math.round(result * 100)/(double)100;;
            PercentageList.add("PWalkGW");
        }
        if (PBikeGW > GetProperties.getPBikeGW()){
            PercentageList.add("PBikeGW");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PBikeGW = 50.0 + (int)Math.round(result * 100)/(double)100;;
            PercentageList.add("PBikeGW");
        }
        if (PPtGW > GetProperties.getPPtGW()){
            PercentageList.add("PPtGW");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            PPtGW = 50.0 + (int)Math.round(result * 100)/(double)100;
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
            logger.debug(PWalkSD);
            PercentageList.add("PWalkSD");
        }
        if (PBikeSD > GetProperties.getPBikeGW()){
            PercentageList.add("PBikeSD");
        }
        else{
            double random = new Random().nextDouble();
            double result = -0.5 + (random * (0.5 + 0.5));
            logger.debug(result);
            PBikeSD = 50.0 + (int) Math.round(result * 100)/(double)100;
            logger.debug(PBikeSD);
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
        //prepei na allaksei kai na epistrefei mia sortarismenh lista me ta messages apo ola ta context
        List<Message> messages = new ArrayList<>();
        LinkedHashMap<Message, Double> rankedMessagesMap = new LinkedHashMap<>();
        for (Message message : mes ) {
            //Calculate message utility based on context.
            Double messageUtility = calculateUtility(message.getContext(), target, user);
            message.setUtility(messageUtility);
            rankedMessagesMap.put(message, messageUtility);
            //Rank messages based on context
            rankedMessagesMap = entriesSortedByValues(rankedMessagesMap);

            /*if (messageUtility > max_message_utility) {
                max_message_utility = messageUtility;
                messages.clear();
                messages.add(message);
            }
            else if (messageUtility.equals(max_message_utility)){
                messages.add(message);
            }*/
        }
        for (Map.Entry<Message, Double> entry : rankedMessagesMap.entrySet()){
            messages.add(entry.getKey());
        }

        //Select the message that will be displayed on the user
        Double max_message_utility2 = 0.0;
        for (Message m: messages) {
            //If message didn't display during last X hours
            if ( (!DisplayedMessages.contains(m.getId()) ) && selected_message_text.isEmpty()){
                if (check_message(user,mongoDatastore, 3, m)){
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
                    selectedMessageId = m.getId();
                }
                //Set random messageUtility
                /*Double messageUtility = Math.random();
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
                    selectedMessageId = m.getId();
                }*/
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
                    logger.debug(user.getWalkUsageComparedToOthers());
                    selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(user.getWalkUsageComparedToOthers())));
                }
                else {
                    logger.debug("----else"+PWalkSD);
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
            if ("points".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(RewardPoints)));
            }
            if ("prize".equals(selected_message_params)){
                selected_message_text = selected_message_text.replace("X", Double.toString(Math.round(RewardPoints)));
                Integer prize = getPrize(user,RewardPoints);
                logger.debug(prize);
                if ("de".equals(lang)) {
                    if(pilot.equals("BRI")) {
                        selected_message_text = selected_message_text.replace("Y", "zu verdienen und damit Wertkarten für Einkäufe im Wert von "+prize+"£ zu erhalten");
                    }
                    else if(pilot.equals("VIE")) {
                        selected_message_text = selected_message_text.replace("Y", "im Wert von "+prize+"€ zu verdienen");
                    }
                    else if(pilot.equals("LJU")) {
                        if (prize.equals(1)){
                            selected_message_text = selected_message_text.replace("Y", "für eine Monatskarte zu verdienen");
                        }
                        else if (prize>1){
                            selected_message_text = selected_message_text.replace("Y", "für eine "+prize+" Monatskarten zu verdienen");
                        }

                    }
                }
                else if("sl".equals(lang)){
                    if(pilot.equals("BRI")) {
                        selected_message_text = selected_message_text.replace("Y", "ki ti prinesejo kupone v vrednosti "+prize+"£ ali Swift kartico");
                    }
                    else if(pilot.equals("VIE")) {
                        selected_message_text = selected_message_text.replace("Y", "ki ti prinesejo "+prize+"€");
                    }
                    else if(pilot.equals("LJU")) {
                        if (prize.equals(1)) {
                            selected_message_text = selected_message_text.replace("Y", "za mesečno vozovnico");
                        }
                        else if (prize.equals(2)){
                            selected_message_text = selected_message_text.replace("Y", "mesečni vozovnici");
                        }
                        else if (prize.equals(3) || prize.equals(4)){
                            selected_message_text = selected_message_text.replace("Y", "mesečne vozovnice");
                        }
                        else if (prize>4){
                            selected_message_text = selected_message_text.replace("Y", "mesečnih vozovnic");
                        }
                    }
                }
                else {
                    if(pilot.equals("BRI")) {
                        selected_message_text = selected_message_text.replace("Y", "getting "+prize+"£ worth of shopping vouchers/switch cards");
                    }
                    else if(pilot.equals("VIE")) {
                        selected_message_text = selected_message_text.replace("Y", "getting "+prize+"€");
                    }
                    else if(pilot.equals("LJU")) {
                        if (prize.equals(1)){
                            selected_message_text = selected_message_text.replace("Y", "getting a monthly pass");
                        }
                        else if (prize>1){
                            selected_message_text = selected_message_text.replace("Y", "getting "+prize+" monthly passes");
                        }
                    }
                }
            }
        }

        logger.debug(selected_message_text);
        logger.debug("-"+strategy+"-");
        logger.debug("-"+selectedMessageId+"-");

        try {
            //increase the number_of_times_sent of the selected message
            Query<Message> query2 = mongoDatastore.createQuery(Message.class);
            query2.criteria("id").equal(selectedMessageId);
            Message messageObj = query2.get();
            Integer numberOfTimesMessageSent = messageObj.getNumber_of_times_sent();
            numberOfTimesMessageSent++;
            mongoDatastore.update(query2, mongoDatastore.createUpdateOperations(Message.class).set("number_of_times_sent",numberOfTimesMessageSent ));
            //increase the number_of_times_sent of the selected strategy
            Query<Strategy> strategyQuery = mongoDatastore.createQuery(Strategy.class).field("persuasive_strategy").equal(strategy);
            Strategy dbStrategy = strategyQuery.get();
            logger.debug("number of times sent: " + strategyQuery.get().getNumber_of_times_sent());
            Integer number_of_times_sent = strategyQuery.get().getNumber_of_times_sent();
            number_of_times_sent ++;
            mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("number_of_times_sent", number_of_times_sent));
            //Increase the number of attemps for the current user
            Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(user.getId());
            Integer user_attempts;
            DBCollection trips = mongoDatastore.getDB().getCollection("UserTrip");
            if (strategy.equals("suggestion") ) {
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
            if (strategy.equals("reward") ) {
                try {
                    user_attempts = userQuery.get().getRewAttempts();
                }
                catch (Exception e){
                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("rewAttempts", 0));
                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("rewSuccess", 0));
                    user_attempts = userQuery.get().getSelfAttempts();
                }
                user_attempts = user_attempts + 1;
                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("rewAttempts", user_attempts));
            }
            selected_message_text = selected_message_text + "_" +strategy+ "_" +selectedMessageId;
            return selected_message_text;

        }
        catch(Exception e){
            Strategy newStrategy = new Strategy();
            mongoDatastore.save(newStrategy);
            logger.debug("Exception: "+e.getMessage(),e);
            selected_message_text = selected_message_text + "_" +strategy+ "_" +selectedMessageId;
            return selected_message_text;
        }

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
        String ReachingPrizeTarget = "ReachingTarget";


        if ("walk".equals(mode)) {
            if (user.tooManyCarRoutes()) {
                if (WalkDistance.equals(context)) {
                    utility = 0.3515;
                }
                if (Duration.equals(context)) {
                    utility = 0.2382;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.0389;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0625;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1011;
                }
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.2081;
                }
            }
            if (user.tooManyPublicTransportRoutes()) {
                if (WalkDistance.equals(context)) {
                    utility = 0.3324;
                }
                if (Duration.equals(context)) {
                    utility = 0.2343;
                }
                if (TooManyTransportRoutes.equals(context)) {
                    utility = 0.0344;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0621;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1123;
                }
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.2245;
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
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.1;
                }

            }
        }
        else if ("bicycle".equals(mode) || "bikeSharing".equals(mode)) {
            if (user.tooManyCarRoutes()) {
                if (BikeDistance.equals(context)) {
                    utility = 0.3512;
                }
                if (Duration.equals(context)) {
                    utility = 0.2382;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.0389;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.0625;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1011;
                }
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.2081;
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
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.2245;
                }
            } else {
                if (BikeDistance.equals(context)) {
                    utility = 0.4;
                }
                if (Duration.equals(context)) {
                    utility = 0.1;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.1;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.1;
                }
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.3;
                }

            }
        }
        else if("bike&ride".equals(mode)) {
            if (user.tooManyCarRoutes()) {
                if (Duration.equals(context)) {
                    utility = 0.4376;
                }
                if (TooManyCarRoutes.equals(context)) {
                    utility = 0.0557;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.1282;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.2043;
                }
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.1742;
                }
            }
            if (user.tooManyPublicTransportRoutes()) {
                if (Duration.equals(context)) {
                    utility = 0.4385;
                }
                if (TooManyTransportRoutes.equals(context)) {
                    utility = 0.0296;
                }
                if (EmissionComparetoOthers.equals(context)) {
                    utility = 0.1334;
                }
                if (NiceWeather.equals(context)) {
                    utility = 0.2243;
                }
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.1742;
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
                if (ReachingPrizeTarget.equals(context)) {
                    utility = 0.1;
                }
            }
        }
        else if ("pt".equals(mode)) {
            if (Duration.equals(context)) {
                utility = 0.4323;
            }
            if (emissionsIncreasing.equals(context)) {
                utility = 0.1935;
            }
            if (NiceWeather.equals(context)) {
                utility = 0.0361;
            }
            if (TooManyCarRoutes.equals(context)) {
                utility = 0.0845;
            }
            if (ReachingPrizeTarget.equals(context)) {
                utility = 0.2536;
            }

        }
        else if ("park&ride".equals(mode)){
            if (Duration.equals(context)) {
                utility = 0.4376;
            }
            if (EmissionComparetoOthers.equals(context)) {
                utility = 0.1282;
            }
            if (NiceWeather.equals(context)) {
                utility = 0.2043;
            }
            if (TooManyCarRoutes.equals(context)) {
                utility = 0.0557;
            }
            if (ReachingPrizeTarget.equals(context)) {
                utility = 0.1742;
            }
        }

        return utility;
    }

    private static List<String> get_presented_messages(String userId, String strategy, Datastore mongoDatastore) {

        //check if the graph displayed last X hours.
        Integer X=GetProperties.getHours();
        //get the current timestamp and the timestamp of the latestUpdate
        Timestamp now = new Timestamp(System.currentTimeMillis());
        //Compare if the timestamp is less than X hours.
        DBCollection routes = mongoDatastore.getDB().getCollection("UserTrip");
        BasicDBObject TripQuery = new BasicDBObject();
        TripQuery.put("userId", userId);
        TripQuery.put("body.additionalInfo.additionalProperties.strategy", strategy);
        TripQuery.put("body.additionalInfo.additionalProperties.messageId",new BasicDBObject("$exists", true));
        BasicDBObject fields = new BasicDBObject();
        fields.put("createdat", 1);
        fields.put("body.additionalInfo.additionalProperties.messageId",1);
        List<DBObject> request = routes.find(TripQuery, fields).sort(new BasicDBObject("$natural", -1)).limit(10).toArray();
        List<String> DisplayedMessages = new ArrayList<String>();

        if (request.isEmpty()){
            return DisplayedMessages;
        }
        else {
            for (int i =0; i<request.size();i++) {

                String dateString = request.get(i).get("createdat").toString();
                logger.debug(dateString);
                DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.ENGLISH);
                try {
                    Date date = format.parse(dateString);
                    long milliseconds = Math.abs(date.getTime() - now.getTime());
                    int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);
                    if (hours > X) {
                        String messageId = ((DBObject)((DBObject)((DBObject)request.get(i).get("body")).get("additionalInfo")).get("additionalProperties")).get("messageId").toString();
                        logger.debug(messageId);
                        DisplayedMessages.add(messageId);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return DisplayedMessages;
        }
    }

    private static Boolean check_message(User user,Datastore mongoDatastore, Integer N, Message message) {

        String messageId = message.getId();

        DBCollection routes = mongoDatastore.getDB().getCollection("UserTrip");
        BasicDBObject TripQuery = new BasicDBObject();
        TripQuery.put("userId", user.getId());
        TripQuery.put("body.additionalInfo.additionalProperties.messageId",messageId);

        BasicDBObject fields = new BasicDBObject();
        fields.put("requestId", 1);

        List<String> RequestIds = new ArrayList <String>();

        DBCursor cursor =routes.find(TripQuery,fields);
        while (cursor.hasNext()) {
            String requestId = cursor.next().get("requestId").toString();
            logger.debug(requestId);
            RequestIds.add(requestId);
        }
        logger.debug(RequestIds);

        DBCollection feedback = mongoDatastore.getDB().getCollection("UserRoute");

        BasicDBObject FeedbackQuery = new BasicDBObject();
        FeedbackQuery.put("_id",new BasicDBObject("$in", RequestIds));
        FeedbackQuery.put("userId", user.getId());
        FeedbackQuery.put("route_feedback.helpful", Boolean.TRUE);

        Integer positiveFeedback = feedback.find(FeedbackQuery).size();

        BasicDBObject FeedbackQuery2 = new BasicDBObject();
        FeedbackQuery2.put("_id",new BasicDBObject("$in", RequestIds));
        FeedbackQuery2.put("userId", user.getId());
        FeedbackQuery2.put("route_feedback.helpful", Boolean.FALSE);
        Integer negativeFeedback = feedback.find(FeedbackQuery2).size();
        logger.debug(negativeFeedback);
        logger.debug(positiveFeedback);
        Integer no_answer= abs(negativeFeedback - positiveFeedback);
        logger.debug(no_answer);
        DBCollection messageThrottlingDb = mongoDatastore.getDB().getCollection("MessageThrottling");
        if (mongoDatastore.createQuery(MessageThrottling.class).field("userId").equal(user.getId()).field("messageId").equal(messageId).asList().isEmpty()) {
            MessageThrottling messageThrottling = new MessageThrottling();
            messageThrottling.setCount(1);
            messageThrottling.setMessageId(messageId);
            messageThrottling.setUserId(user.getId());
            mongoDatastore.save(messageThrottling);
        }
        //messageThrottlingDb.find();

        /*Query<MessageThrottling> query = mongoDatastore.createQuery(MessageThrottling.class);
        query.and(
                query.criteria("userId").equal(user.getId()),
                query.criteria("messageId").equal(messageId));*/
        BasicDBObject query = new BasicDBObject();
        query.put("userId", user.getId());
        query.put("messageId", messageId);
        BasicDBObject fields1 = new BasicDBObject();
        fields.put("count", 1);
        DBObject messageThr = messageThrottlingDb.findOne(query,fields1);
        logger.debug(messageThr.toString());
        //Integer count = query.get().getCount();
        Integer count = Integer.parseInt(messageThr.get("count").toString());
        MessageThrottling messageThrottling = mongoDatastore.createQuery(MessageThrottling.class).field("userId").equal(user.getId()).field("messageId").equal(messageId).get();
        //Integer count = messageThrottling.getCount();
        logger.debug(no_answer);
        if (no_answer>0){
            logger.debug("count"+count);
            Integer message_display= (count%no_answer);
            logger.debug("messageDisplay"+message_display);
            if (message_display.equals(0)){
                count=1;
                logger.debug(count);
                messageThrottlingDb.update(new BasicDBObject("_id", messageThrottling.get_id()),new BasicDBObject("$set", new BasicDBObject("count", count)));
                return Boolean.TRUE;
            }
            else {
                count++;
                logger.debug(count);
                BasicDBObject mes = new BasicDBObject("userId",user.getId()).append("messageId",messageId);
                messageThrottlingDb.update(mes,new BasicDBObject("$set", new BasicDBObject("count", count)));
                logger.debug(count);
                return Boolean.FALSE;
            }
        }
        else {
            return Boolean.TRUE;
        }

    }

    private static Integer getPrize(User user, Double rewardPoints) {
        Integer prize = 0;

        try {
            String city = user.getPilot();
            //Get user points
            Double points = user.getPoints();
            points=points+rewardPoints;
            logger.debug("points"+points);
            logger.debug("city"+city);
            if (city.equals("BRI")) {
                prize = (points.intValue()/1140)*5;
            }
            else if (city.equals("LJU")) {
                prize = points.intValue()/7400;
            }
            else if (city.equals("VIE")) {
                prize = (points.intValue() / 1000) * 5;
            }
        } catch (Exception e) {
            return 0;
        }
        return prize;
    }

    public Double GetRemainingPoints(User user) {
        try {
            String city = user.getPilot();
            Double new_points=0.0;
            //Get user points
            Double points = user.getPoints();
            if (city.equals("BRI")) {
                if (points < 1140) {
                    new_points = 1140.0- points;
                    }

                } else if (points > 1140 && points < 2280) {
                    new_points=2280.0-points;
                } else if (points >= 2280) {
                new_points = points - 2280.0;
                while (new_points > 2280) {
                    new_points = new_points - 2280;
                }
                if (new_points < 1140) {
                    new_points = 1140.0-new_points;

                } else if (new_points > 1140 && new_points < 2280) {
                    new_points = 2280.0-new_points;
                }
            }
            else if (city.equals("LJU")) {
                if (points<7400) {
                    new_points = 7400.0-points;
                }else{
                    new_points = points - 7400.0;
                    while (new_points > 7400) {
                        new_points=new_points-7400;
                    }
                    new_points = 7400-new_points;
                }
            }
            else if (city.equals("VIE")) {
                if (points<1000) {
                    new_points = 1000.0 - points;
                    }
                }else{
                    new_points = points - 1000.0;
                    while (new_points > 1000) {
                        new_points=new_points-1000;
                    }
                    new_points = 1000 - new_points;

                }
            return new_points;
        } catch (Exception e) {
            return 0.0;
        }

    }

    static <K,V extends Comparable<? super V>>
    LinkedHashMap<K, V> entriesSortedByValues(LinkedHashMap<K,V> map) {

        List<Map.Entry<K, V>> entries =
                new ArrayList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b){
                return a.getValue().compareTo(b.getValue());
            }
        });
        LinkedHashMap<K, V> sortedEntries = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : entries) {
            sortedEntries.put(entry.getKey(), entry.getValue());
        }
        return sortedEntries;
    }



}

