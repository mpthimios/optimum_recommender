package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.strategy.Strategy;
import imu.recommender.models.user.User;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by evangelie on 12/12/2016.
 */
public class UpdateStrategiesProbabilities  implements Job{
    @Override
    public void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        Datastore mongoDatastore;

        try {
            mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        DBCollection m = mongoDatastore.getCollection( Strategy.class );
        DBCollection trips = mongoDatastore.getDB().getCollection("UserTrip");
        //Update the probabilities of each strategy based on all users.
        List strategies = m.distinct( "persuasive_strategy", new BasicDBObject());

        for (Object id : strategies ) {
            try {
                //Get total attemps
                Query<Strategy> strategyQuery = mongoDatastore.createQuery(Strategy.class).field("persuasive_strategy").equal(id.toString());
                //Update the number of success
                //Find all saved trips with persusasive message (suggestion)
                BasicDBObject TripQuery = new BasicDBObject();
                TripQuery.put("favourite", true);
                TripQuery.put("body.additionalInfo.additionalProperties.strategy", strategyQuery.get().getPersuasive_strategy());
                DBCursor tripsIds = trips.find(TripQuery);
                Integer success = tripsIds.count();
                mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("number_of_successes", success));
                try {

                    //Get total attemps and successes
                    Integer number_of_times_sent = strategyQuery.get().getNumber_of_times_sent();
                    Integer number_of_success = strategyQuery.get().getNumber_of_successes();

                    //Calculate probability
                    Double probability;
                    if (number_of_success.equals(0) | number_of_times_sent.equals(0)) {
                        probability = 0.0;
                    } else {
                        probability = (double) number_of_success / (double) number_of_times_sent;
                    }
                    //Update probability on mongodb
                    mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("probability", probability));

                    //--------------------------------------------------------------------------------
                    //Calculate probability of this strategy for each user.
                    DBCollection users = mongoDatastore.getCollection(User.class);

                    List userIds = users.distinct("id", new BasicDBObject());

                    //Update the probabilities of each strategy based on all users.

                    for (Object userid : userIds) {
                        System.out.println(id);
                        if (id.toString().equals("suggestion ")) {
                            Integer user_attempts;
                            Integer user_success;
                            //Get total attemps
                            Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(userid);
                            //Get total successes
                            //Find all saved trips of current user with persusasive message (suggestion)
                            BasicDBObject searchTripQuery = new BasicDBObject();
                            searchTripQuery.put("favourite", true);
                            searchTripQuery.put("userId", userid);
                            //searchTripQuery.put("body.additionalInfo.additionalProperties.strategy", "suggestion ");
                            DBCursor tripIds = trips.find(searchTripQuery);
                            Integer StrategySuccess = tripIds.count();
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
                            }
                            Double userProb = 0.0;
                            userProb = calculateUserProbability(number_of_times_sent, number_of_success,user_success, user_attempts);

                            //Update user probability of each strategy, user attempts and user success on mongodb
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugProb", userProb));
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugAttempts", user_attempts));
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("sugSuccess", user_success));
                        } else if (id.toString().equals("comparison")) {
                            Integer user_attempts;
                            Integer user_success;
                            //Get total attemps
                            Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(userid);
                            //Get total successes
                            //Find all saved trips of current user with persusasive message (suggestion)
                            BasicDBObject searchTripQuery = new BasicDBObject();
                            searchTripQuery.put("favourite", true);
                            searchTripQuery.put("userId", userid);
                            //searchTripQuery.put("body.additionalInfo.additionalProperties.strategy", "comparison");
                            DBCursor tripsIdsComp = trips.find(searchTripQuery);
                            Integer successComp = tripsIdsComp.count();
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", successComp));
                            try {
                                //Get total attemps and successes
                                user_attempts = userQuery.get().getCompAttempts();
                                user_success = userQuery.get().getCompSuccess();

                            } catch (Exception e) {
                                //Create the fields
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compAttempts", 0));
                                mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", 0));
                                user_attempts = userQuery.get().getCompAttempts();
                                user_success = userQuery.get().getCompSuccess();
                            }

                            Double userProb = 0.0;
                            userProb = calculateUserProbability(number_of_times_sent, number_of_success,user_success, user_attempts);

                            //Update user probability of each strategy, user attempts and user success on mongodb
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compProb", userProb));
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compAttempts", user_attempts));
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("compSuccess", user_success));
                        } else if (id.toString().equals("self-monitoring")) {
                            Integer user_attempts;
                            Integer user_success;
                            //Get total attemps
                            Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(userid);
                            //Get total successes
                            //Find all saved trips of current user with persusasive message (suggestion)
                            BasicDBObject searchTripQuery = new BasicDBObject();
                            searchTripQuery.put("favourite", true);
                            searchTripQuery.put("userId", userid);
                            //searchTripQuery.put("body.additionalInfo.additionalProperties.strategy", "self-monitoring");
                            DBCursor tripsIdsSelf = trips.find(searchTripQuery);
                            Integer successSelf = tripsIdsSelf.count();
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
                            }
                            Double userProb = 0.0;

                            userProb = calculateUserProbability(number_of_times_sent, number_of_success,user_success, user_attempts);

                            //Update user probability of each strategy, user attempts and user success on mongodb
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfProb", userProb));
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfAttempts", user_attempts));
                            mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("selfSuccess", user_success));
                        }

                    }

                } catch (Exception e) {
                    System.out.println("error");

                }
            } catch (Exception e) {
                System.out.println("error");
            }
        }


    }

    //Calculate the probability of a single user selecting the recommended route
    //based Kaptein Approach (Binomial random variable)
    //n denotes the number of tries to persuade the user using the specific strategy and
    //p denotes the probability of success i.e. the probability of taking the recommended route.
    public static double getBinomial(int n, int p) {
        /*double x = 0.0;
        for(int i = 0; i < n; i++) {
            if(Math.random() < p)
                x++;
        }*/
        return (double) p/(n+p);
    }
    public static double getBinomialDouble(double n, int p) {
        /*double x = 0.0;
        for(int i = 0; i < n; i++) {
            if(Math.random() < p)
                x++;
        }*/
        return (double) p/(n+p);
    }
    public static  double calculateUserProbability(int total_attempts, int strategy_prob, int attempt, int user_attempts){
        //Get n, p
        //n plh8os prospa9eiwn gia thn sugkekrimenh strathgikh
        //p pi8anothta epituxias ths sugkekrimenhs strathgikhs
        //p=epituxia/plh8os
        //int n=30;
        //double p=0.8;
        double StrategyProbability= getBinomial(total_attempts, strategy_prob);
        System.out.println(StrategyProbability);
        //return getBinomial(attempt+user_attempts,total_attempts-attempt+user_attempts*(1-(int)StrategyProbability));
        int s = total_attempts+strategy_prob;
        double pi=1.0-StrategyProbability;

        return getBinomialDouble(user_attempts-attempt+pi*s,attempt+total_attempts+strategy_prob);
    }

}



