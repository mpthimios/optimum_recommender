package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.request.Request;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.UnknownHostException;

import static imu.recommender.Context.logger;

/**
 * Created by evangelia on 27/3/2018.
 */
public class CountRequestGroup implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        Datastore mongoDatastore;

        try {
            mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
            return;
        }

        DBCollection trips = mongoDatastore.getDB().getCollection("UserTrip");
        DBCollection requests = mongoDatastore.getDB().getCollection("Request");

        BasicDBObject RouteQuery = new BasicDBObject();
        RouteQuery.put("body.additionalInfo.additionalProperties.feature", "message");

        BasicDBObject fields = new BasicDBObject();
        fields.put("_id", 1);
        DBCursor requestIdsSuccess = trips.find(RouteQuery, fields );
        Integer messages = requestIdsSuccess.size();


        BasicDBObject RouteQuery2 = new BasicDBObject();
        RouteQuery2.put("body.additionalInfo.additionalProperties.feature", "graph");

        BasicDBObject fields2 = new BasicDBObject();
        fields2.put("_id", 1);
        DBCursor requestIdsSuccess2 = trips.find(RouteQuery2, fields2 );
        Integer graphs = requestIdsSuccess2.size();


        BasicDBObject RouteQuery3 = new BasicDBObject();
        RouteQuery3.put("body.additionalInfo.additionalProperties.feature", "MessageAndGraph");

        BasicDBObject fields3 = new BasicDBObject();
        fields3.put("_id", 1);
        DBCursor requestIdsSuccess3 = trips.find(RouteQuery3, fields3 );
        Integer combination = requestIdsSuccess3.size();

        logger.debug("messages"+messages);
        logger.debug("graphs"+graphs);
        logger.debug("combiantion"+combination);

        Query<Request> Query = mongoDatastore.createQuery(Request.class);

        mongoDatastore.update(Query, mongoDatastore.createUpdateOperations(Request.class).set("numberOfMessages", messages));
        mongoDatastore.update(Query, mongoDatastore.createUpdateOperations(Request.class).set("numberOfGraphs", graphs));
        mongoDatastore.update(Query, mongoDatastore.createUpdateOperations(Request.class).set("numberOfCombination", combination));







    }
}
