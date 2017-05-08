package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.strategy.Strategy;
import imu.recommender.models.user.Personality;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by evangelie on 12/12/2016.
 */
public class UpdateStrategiesProbabilities  implements Job{
    private Logger logger = Logger.getLogger(UpdateStrategiesProbabilities.class);
    @Override
    public void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        Datastore mongoDatastore;

        try {
            mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
            return;
        }

        DBCollection m = mongoDatastore.getCollection( Strategy.class );
        DBCollection trips = mongoDatastore.getDB().getCollection("UserTrip");
        DBCollection routes = mongoDatastore.getDB().getCollection("UserRoute");
        //Update the probabilities of each strategy based on all users.
        List strategies = m.distinct( "persuasive_strategy", new BasicDBObject());

        //Get total popupdisplays and total helpful
        BasicDBObject RouteQuery = new BasicDBObject();
        RouteQuery.put("route_feedback.helpful", true);

        BasicDBObject fields = new BasicDBObject();
        fields.put("_id", 1);
        DBCursor requestIdsSuccess = routes.find(RouteQuery, fields );
        List<String> requestId = new ArrayList<String>();
        while (requestIdsSuccess.hasNext() ) {
            requestId.add(requestIdsSuccess.next().get("_id").toString());
        }

        BasicDBObject RouteQuery1 = new BasicDBObject();
        RouteQuery1.put("route_feedback.helpful", false);
        DBCursor RequsetIdsFailed = routes.find(RouteQuery1);
        List<String> requestIdsFailed = new ArrayList<String>();
        while (RequsetIdsFailed.hasNext() ) {
            requestIdsFailed.add(RequsetIdsFailed.next().get("_id").toString());
        }

        for (Object id : strategies ) {
            try {
                //Get total attemps
                Query<Strategy> strategyQuery = mongoDatastore.createQuery(Strategy.class).field("persuasive_strategy").equal(id.toString());
                //Update the number of success
                //Find all saved trips with persusasive message (suggestion)
                BasicDBObject TripQuery = new BasicDBObject();
                TripQuery.put("viewed", true);
                TripQuery.put("body.additionalInfo.additionalProperties.strategy", strategyQuery.get().getPersuasive_strategy());
                List tripsIds = trips.distinct("requestId",TripQuery);
                Integer success = tripsIds.size();
                mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("number_of_successes", success));

                //Get total strategy success
                BasicDBObject TripQuery2 = new BasicDBObject();
                TripQuery2.put("requestId", new BasicDBObject("$in", requestId));
                TripQuery2.put("body.additionalInfo.additionalProperties.strategy", strategyQuery.get().getPersuasive_strategy());
                List StrategySucess = trips.distinct("requestId",TripQuery2);
                Integer strategysucess = StrategySucess.size();

                //Get total strategy fail
                BasicDBObject TripQuery3 = new BasicDBObject();
                TripQuery3.put("requestId", new BasicDBObject("$in", requestIdsFailed));
                TripQuery3.put("body.additionalInfo.additionalProperties.strategy", strategyQuery.get().getPersuasive_strategy());
                List StrategyFail = trips.distinct("requestId",TripQuery3);
                Integer strategyfail = StrategyFail.size();
                Integer total_strategy_feedback = strategyfail+strategysucess;

                mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("total_sucess_feedback", strategysucess));
                mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("total_feedback", total_strategy_feedback));

                try {
                    //Get total attemps and successes
                    Integer number_of_times_sent = strategyQuery.get().getNumber_of_times_sent();
                    Integer number_of_success = strategyQuery.get().getNumber_of_successes();

                    //If total_feedback > 0  Calculate probability based on feedback else based on view.
                    Double probability;
                    if(total_strategy_feedback > 0){
                        probability = (double) strategysucess / (double) total_strategy_feedback;
                        }
                    else {
                        //Calculate probability
                        if (number_of_success.equals(0) | number_of_times_sent.equals(0)) {
                            probability = 0.0;
                        } else {
                            probability = (double) number_of_success / (double) number_of_times_sent;
                        }
                    }
                    //Update probability on mongodb
                    mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("probability", probability));

                    //--------------------------------------------------------------------------------
                    //Calculate probability of this strategy for each user.
                    DBCollection users = mongoDatastore.getCollection(User.class);

                    List userIds = users.distinct("id", new BasicDBObject());

                    //Update the probabilities of each strategy based on all users.

                    for (Object userid : userIds) {

                        Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(userid);

                        //Get success and feedback attempts
                        BasicDBObject TripQuery4 = new BasicDBObject();
                        TripQuery4.put("requestId", new BasicDBObject("$in", requestId));
                        TripQuery4.put("userId",userid);
                        TripQuery4.put("body.additionalInfo.additionalProperties.strategy", strategyQuery.get().getPersuasive_strategy());
                        List UserFeedbackSucess = trips.distinct("requestId",TripQuery4);
                        Integer userFeedbackSucess = UserFeedbackSucess.size();

                        //Get total strategy fail
                        BasicDBObject TripQuery5 = new BasicDBObject();
                        TripQuery5.put("requestId", new BasicDBObject("$in", requestIdsFailed));
                        TripQuery5.put("userId", userid);
                        TripQuery5.put("body.additionalInfo.additionalProperties.strategy", strategyQuery.get().getPersuasive_strategy());
                        List UserFeedbackFail = trips.distinct("requestId", TripQuery5);
                        Integer userFeedbackFail = UserFeedbackFail.size();
                        Integer total_user_feedback = userFeedbackFail +userFeedbackSucess;

                        if ("suggestion".equals(id.toString())) {
                            Integer user_attempts;
                            Integer user_success;
                            if (total_user_feedback > 0){
                                user_success = userFeedbackSucess;
                                user_attempts = total_user_feedback;
                                number_of_times_sent = total_strategy_feedback;
                                number_of_success = strategysucess;
                            }
                            else{
                                //Get total attemps and total successes
                                //Find all saved trips of current user with persusasive message (suggestion)
                                BasicDBObject searchTripQuery = new BasicDBObject();
                                searchTripQuery.put("viewed", true);
                                searchTripQuery.put("userId", userid);
                                searchTripQuery.put("body.additionalInfo.additionalProperties.strategy", "suggestion");
                                List tripIds = trips.distinct("requestId",searchTripQuery);
                                Integer StrategySuccess = tripIds.size();
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugSuccess", StrategySuccess));
                                try {
                                    //Get total attemps and successes
                                    user_attempts = userQuery.get().getSugAttempts();
                                    user_success = userQuery.get().getSugSuccess();

                                } catch (Exception e) {
                                    //Create the fields
                                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugAttempts", 0));
                                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugSuccess", 0));
                                    user_attempts = userQuery.get().getSugAttempts();
                                    user_success = userQuery.get().getSugSuccess();
                                    logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
                                }
                                //Update user attempts and user success on mongodb
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugAttempts", user_attempts));
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugSuccess", user_success));

                            }
                            //Get personality of user
                            String personality = userQuery.get().getPersonality().getTypeStr();

                            Double userProb = 0.0;
                            double sum_prob;
                            switch (personality) {
                                case "Extraversion":
                                    sum_prob = GetProperties.getCompEx() + GetProperties.getSelfEx() + GetProperties.getSugEx();
                                    if (sum_prob!= 0.0d) {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugEx() / sum_prob);
                                    }
                                    else{
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugEx());
                                    }
                                    break;
                                case "Agreeableness":
                                    sum_prob = GetProperties.getCompAg() + GetProperties.getSelfAg() + GetProperties.getSugAg();
                                    if (sum_prob != 0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugAg()/sum_prob);
                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugAg());
                                    }
                                    break;
                                case "Openness":
                                    sum_prob = GetProperties.getCompOp() + GetProperties.getSelfOp() + GetProperties.getSugOp();
                                    if (sum_prob !=0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugOp()/sum_prob);
                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugOp());
                                    }
                                    break;
                                case "Conscientiousness":
                                    sum_prob = GetProperties.getCompCons() + GetProperties.getSelfCons() + GetProperties.getSugCons();
                                    if (sum_prob !=0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugCons()/sum_prob);

                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugCons());
                                    }
                                    break;
                                case "Neuroticism":
                                    sum_prob = GetProperties.getCompN() + GetProperties.getSelfN() + GetProperties.getSugN();
                                    if(sum_prob!=0.0d) {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugN() / sum_prob);
                                    }
                                    else{
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSugN());
                                    }
                                    break;
                            }
                            //Update user probability of each strategy, user attempts and user success on mongodb
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugProb", userProb));

                        } else if ("comparison".equals(id.toString())) {
                            Integer user_attempts;
                            Integer user_success;
                            if (total_user_feedback > 0){
                                user_success = userFeedbackSucess;
                                user_attempts = total_user_feedback;
                                number_of_times_sent = total_strategy_feedback;
                                number_of_success = strategysucess;
                            }
                            else {
                                //Get total attemps and successes
                                //Find all saved trips of current user with persusasive message (suggestion)
                                BasicDBObject searchTripQuery = new BasicDBObject();
                                searchTripQuery.put("viewed", true);
                                searchTripQuery.put("userId", userid);
                                searchTripQuery.put("body.additionalInfo.additionalProperties.strategy", "comparison");
                                List tripsIdsComp = trips.distinct("requestId", searchTripQuery);
                                Integer successComp = tripsIdsComp.size();
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", successComp));
                                try {
                                    //Get total attemps and successes
                                    user_attempts = userQuery.get().getCompAttempts();
                                    user_success = userQuery.get().getCompSuccess();

                                } catch (Exception e) {
                                    //Create the fields
                                    logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
                                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compAttempts", 0));
                                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", 0));
                                    user_attempts = userQuery.get().getCompAttempts();
                                    user_success = userQuery.get().getCompSuccess();
                                }
                                //Update strategy attempts and success on mongodb
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compAttempts", user_attempts));
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", user_success));
                            }

                            Double userProb = 0.0;
                            //String personality = userQuery.get().getPersonality().getTypeStr();
                            String personality  = getPersonalityType(userid.toString(), mongoDatastore);
                            double sum_prob;
                            switch (personality) {
                                case "Extraversion":
                                    sum_prob = GetProperties.getCompEx() + GetProperties.getSelfEx() + GetProperties.getSugEx();
                                    if (sum_prob!= 0.0d) {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompEx() / sum_prob);
                                    }
                                    else{
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompEx());
                                    }
                                    break;
                                case "Agreeableness":
                                    sum_prob = GetProperties.getCompAg() + GetProperties.getSelfAg() + GetProperties.getSugAg();
                                    if (sum_prob != 0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompAg()/sum_prob);
                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompAg());
                                    }
                                    break;
                                case "Openness":
                                    sum_prob = GetProperties.getCompOp() + GetProperties.getSelfOp() + GetProperties.getSugOp();
                                    if (sum_prob !=0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompOp()/sum_prob);
                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompOp());
                                    }
                                    break;
                                case "Conscientiousness":
                                    sum_prob = GetProperties.getCompCons() + GetProperties.getSelfCons() + GetProperties.getSugCons();
                                    if (sum_prob !=0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompCons()/sum_prob);

                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompCons());
                                    }
                                    break;
                                case "Neuroticism":
                                    sum_prob = GetProperties.getCompN() + GetProperties.getSelfN() + GetProperties.getSugN();
                                    if(sum_prob!=0.0d) {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompN() / sum_prob);
                                    }
                                    else{
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getCompN());
                                    }
                                    break;
                            }
                            //Update user probability of each strategy, user attempts and user success on mongodb
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compProb", userProb));

                        } else if ("self-monitoring".equals(id.toString())) {
                            Integer user_attempts;
                            Integer user_success;
                            if (total_user_feedback > 0){
                                user_success = userFeedbackSucess;
                                user_attempts = total_user_feedback;
                                number_of_times_sent = total_strategy_feedback;
                                number_of_success = strategysucess;
                            }
                            else {
                                //Get total attemps and successes
                                //Find all saved trips of current user with persuasive message (suggestion)
                                BasicDBObject searchTripQuery = new BasicDBObject();
                                searchTripQuery.put("viewed", true);
                                searchTripQuery.put("userId", userid);
                                searchTripQuery.put("body.additionalInfo.additionalProperties.strategy", "self-monitoring");
                                List tripsIdsSelf = trips.distinct("requestId", searchTripQuery);
                                //DBCursor tripsIdsSelf = trips.find(searchTripQuery);
                                Integer successSelf = tripsIdsSelf.size();
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfSuccess", successSelf));
                                try {
                                    //Get total attemps and successes
                                    user_attempts = userQuery.get().getSelfAttempts();
                                    user_success = userQuery.get().getSelfSuccess();

                                } catch (Exception e) {
                                    //Create the fields
                                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfAttempts", 0));
                                    mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfSuccess", 0));
                                    user_attempts = userQuery.get().getSelfAttempts();
                                    user_success = userQuery.get().getSelfSuccess();
                                    logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
                                }

                                //Update strategy attempts and success on mongodb
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfAttempts", user_attempts));
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfSuccess", user_success));
                            }
                            Double userProb = 0.0;
                            String personality = userQuery.get().getPersonality().getTypeStr();
                            double sum_prob;
                            switch (personality) {
                                case "Extraversion":
                                    sum_prob = GetProperties.getCompEx() + GetProperties.getSelfEx() + GetProperties.getSugEx();
                                    if (sum_prob!= 0.0d) {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfEx() / sum_prob);
                                    }
                                    else{
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfEx());
                                    }
                                    break;
                                case "Agreeableness":
                                    sum_prob = GetProperties.getCompAg() + GetProperties.getSelfAg() + GetProperties.getSugAg();
                                    if (sum_prob != 0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfAg()/sum_prob);
                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfAg());
                                    }
                                    break;
                                case "Openness":
                                    sum_prob = GetProperties.getCompOp() + GetProperties.getSelfOp() + GetProperties.getSugOp();
                                    if (sum_prob !=0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfOp()/sum_prob);
                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfOp());
                                    }
                                    break;
                                case "Conscientiousness":
                                    sum_prob = GetProperties.getCompCons() + GetProperties.getSelfCons() + GetProperties.getSugCons();
                                    if (sum_prob !=0.0d){
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfCons()/sum_prob);

                                    }
                                    else {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfCons());
                                    }
                                    break;
                                case "Neuroticism":
                                    sum_prob = GetProperties.getCompN() + GetProperties.getSelfN() + GetProperties.getSugN();
                                    if(sum_prob!=0.0d) {
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfN() / sum_prob);
                                    }
                                    else{
                                        userProb = calculateUserProbability(number_of_times_sent, number_of_success, user_success, user_attempts, GetProperties.getSelfN());
                                    }
                                    break;
                            }

                            //Update user probability of each strategy, user attempts and user success on mongodb
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfProb", userProb));

                        }

                    }

                } catch (Exception e) {
                    logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);

                }
            } catch (Exception e) {
                logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
            }
        }


    }

    //Calculate the probability of a single user selecting the recommended route
    //based Kaptein Approach (Binomial random variable)

    //Likelihood
    //n denotes the number of tries to persuade the user using the specific strategy and
    //k denotes the number of success of the specific strategy

    //Prior
    //m denotes the prior probability of success (StrategyProbabilitySuccess based on model )
    //M denotes the number of attempts of prior.

    public static  double calculateUserProbability(int total_attempts, int total_successes, int k, int n, double m){

        //Set M to 10
        int M = 10;
        System.out.println((k+M*m)/(n+M));

        return (k+M*m)/(n+M);
    }

    public String getPersonalityType(String id, Datastore mongoDatastore) throws UnknownHostException {

        //Datastore mongoDatastore;
        //mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        Personality pers = mongoDatastore.createQuery(User.class).field("id").equal(id).get().getPersonality();
        String pref_mode= pers.getPreferredMode();

        if (!pers.isScores_calculated()){
            //calculate scores
            double extraversion_score = ( reverse(pers.getQ1()) + pers.getQ6() )/2.0;
            double agreeableness_score = ( pers.getQ2() + reverse(pers.getQ7() ) )/2.0;
            double conscientiousness_score = ( reverse(pers.getQ3() ) + pers.getQ8() )/2.0;
            double neuroticism_score = ( reverse(pers.getQ4()) + pers.getQ9() )/2.0;
            double openness_score = (reverse(pers.getQ5()) + pers.getQ10() )/2.0;
            pers.setExtraversion(extraversion_score);
            pers.setAgreeableness(agreeableness_score);
            pers.setConsientiousness(conscientiousness_score);
            pers.setNeuroticism(neuroticism_score);
            pers.setOpenness(openness_score);
            pers.setScores_calculated(true);
            //Find max score
            List<String> personalities =  Arrays.asList("Extraversion", "Agreeableness", "Conscientiousness", "Neuroticism", "Openness");
            List<Double> scores = Arrays.asList( pers.getExtraversion(), pers.getAgreeableness(), pers.getConsientiousness(), pers.getNeuroticism(), pers.getOpenness() );
            double max = 0.0;
            for(int i=0; i<personalities.size(); i++){
                if (scores.get(i) > max) {
                    max= scores.get(i);
                    pers.setTypeStr(personalities.get(i));
                    pers.setType(max);
                }
            }

            Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal(id);
            //Update the personality of user.
            Personality personality = new Personality();
            personality.setTypeStr(pers.getTypeStr());
            personality.setType(pers.getType());
            personality.setQ1(pers.getQ1());
            personality.setQ2(pers.getQ2());
            personality.setQ3(pers.getQ3());
            personality.setQ4(pers.getQ4());
            personality.setQ5(pers.getQ5());
            personality.setQ6(pers.getQ6());
            personality.setQ7(pers.getQ7());
            personality.setQ8(pers.getQ8());
            personality.setQ9(pers.getQ9());
            personality.setQ10(pers.getQ10());
            personality.setConsientiousness(pers.getConsientiousness());
            personality.setAgreeableness(pers.getAgreeableness());
            personality.setOpenness(pers.getOpenness());
            personality.setNeuroticism(pers.getNeuroticism());
            personality.setExtraversion(pers.getExtraversion());
            personality.setScores_calculated(true);
            personality.setPreferredMode(pref_mode);
            personality.setMaxPreferredBikeDistance(pers.getMaxPreferredBikeDistance());
            personality.setMaxPreferredWalkDistance(pers.getMaxPreferredWalkDistance());

            UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("personality",personality);
            mongoDatastore.update(query, ops);
        }

        return pers.getTypeStr();
    }

    private Double reverse(Double score){
        Double reversed;
        if (score == 1.0d){
            reversed = 5.0;
        }
        else if (score == 2.0d){
            reversed = 4.0;
        }
        else if (score == 4.0d){
            reversed = 2.0;
        }
        else if (score == 5.0d){
            reversed = 1.0;
        }
        else{
            reversed = 3.0;
        }
        return reversed;
    }

}

