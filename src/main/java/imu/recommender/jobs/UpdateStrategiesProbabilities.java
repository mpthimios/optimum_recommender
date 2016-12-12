package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
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
        //Update the probabilities of each strategy based on all users.
        List strategies = m.distinct( "persuasive_strategy", new BasicDBObject());
        //List userIds = m.distinct( "id", new BasicDBObject());
        System.out.println(strategies);
        for (Object id : strategies ) {
            System.out.println(id);
            try {
                //Get total attemps
                Query<Strategy> strategyQuery = mongoDatastore.createQuery(Strategy.class).field("persuasive_strategy").equal(id.toString());
                //Get total attemps and successes
                Integer number_of_times_sent = strategyQuery.get().getNumber_of_times_sent();
                Integer number_of_success = strategyQuery.get().getNumber_of_successes();

                //Calculate probability
                Double probability;
                if (number_of_success.equals(0) | number_of_times_sent.equals(0)){
                    probability = 0.0;
                }
                else {
                    probability = (double) number_of_success / (double) number_of_times_sent;
                }
                //Update probability on mongodb
                mongoDatastore.update(strategyQuery, mongoDatastore.createUpdateOperations(Strategy.class).set("probability", probability));

                //--------------------------------------------------------------------------------
                //Calculate probability of this strategy for each user.
                /*DBCollection users = mongoDatastore.getCollection( Strategy.class );
                //Update the probabilities of each strategy based on all users.
                List userIds = users.distinct( "id", new BasicDBObject());
                System.out.println(userIds);
                for (Object userid : strategies ) {


                }*/

            } catch (Exception e) {

            }
        }


    }

    //Calculate the probability of a single user selecting the recommended route
    //based Kaptein Approach (Binomial random variable)
    //n denotes the number of tries to persuade the user using the specific strategy and
    //p denotes the probability of success i.e. the probability of taking the recommended route.
    public static int getBinomial(int n, double p) {
        int x = 0;
        for(int i = 0; i < n; i++) {
            if(Math.random() < p)
                x++;
        }
        return x;
    }
    public static  int calculateUserProbability(String strategy, User user, int attempt){
        //Get n, p
        //n plh8os prospa9eiwn gia thn sugkekrimenh strathgikh
        //p pi8anothta epituxias ths sugkekrimenhs strathgikhs
        //p=epituxia/plh8os
        int n=30;
        double p=0.8;
        int StrategyProbability= getBinomial(n,p);
        return getBinomial(attempt+n,n-attempt+n*(1.0-p));
    }

}



