package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import imu.recommender.helpers.MongoConnectionHelper;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by evangelia on 5/11/2017.
 */
public class PersuasiveMessages implements Job {
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

        //DBCollection m = mongoDatastore.getCollection( Strategy.class );
        DBCollection trips = mongoDatastore.getDB().getCollection("UserTrip");
        DBCollection routes = mongoDatastore.getDB().getCollection("UserRoute");
        //Update the probabilities of each strategy based on all users.
        //List strategies = m.distinct( "persuasive_strategy", new BasicDBObject());

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
        logger.debug("Total Suc:"+requestIdsSuccess.size());

        BasicDBObject RouteQuery1 = new BasicDBObject();
        RouteQuery1.put("route_feedback.helpful", false);
        DBCursor RequsetIdsFailed = routes.find(RouteQuery1);
        List<String> requestIdsFailed = new ArrayList<String>();
        while (RequsetIdsFailed.hasNext() ) {
            requestIdsFailed.add(RequsetIdsFailed.next().get("_id").toString());
        }
        logger.debug("Total Failed:"+requestIdsFailed.size());


        //Get success and feedback attempts
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat ("yyyy.MM.dd");
        String date ="2017.04.27";
        String date1 ="2017.05.12";
        try {
            Date startDate = simpleDateFormat.parse(date);
            Date endDate = simpleDateFormat.parse(date1);
            BasicDBObject TripQuery4 = new BasicDBObject("createdat",
                    new BasicDBObject("$gte",startDate).append("$lt",endDate ));


            TripQuery4.put("requestId", new BasicDBObject("$in", requestId));
            List UserFeedbackSucess = trips.distinct("requestId",TripQuery4);
            for (Object document : UserFeedbackSucess) {
                System.out.println(document);
            }
            Integer userFeedbackSucess = UserFeedbackSucess.size();
            logger.debug("Sucess Feedback"+userFeedbackSucess);

            //Get total strategy fail
            BasicDBObject TripQuery5 = new BasicDBObject("createdat",
                    new BasicDBObject("$gte",startDate).append("$lt",endDate ));
            TripQuery5.put("requestId", new BasicDBObject("$in", requestIdsFailed));
            List UserFeedbackFail = trips.distinct("requestId", TripQuery5);
            Integer userFeedbackFail = UserFeedbackFail.size();
            logger.debug("Failed Feedback"+userFeedbackFail);

            //Get total requests
            BasicDBObject TripQuery6 = new BasicDBObject("createdat",
                    new BasicDBObject("$gte",startDate).append("$lt",endDate ));
            //TripQuery6.put("body.additionalInfo.additionalProperties.message", new BasicDBObject("$exists", true));
            TripQuery6.put("body.additionalInfo.additionalProperties.message", "");
            /*TripQuery6.put("body.additionalInfo.additionalProperties.strategy", "suggestion");
            TripQuery6.put("body.additionalInfo.additionalProperties.strategy", "comparison");
            TripQuery6.put("body.additionalInfo.additionalProperties.strategy", "self-monitoring");*/
            List Requests = trips.distinct("requestId", TripQuery6);
            Integer requests = Requests.size();

            logger.debug("Total requests:"+requests);
            for (Object document : Requests) {
                System.out.println(document);
            }



        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

}
