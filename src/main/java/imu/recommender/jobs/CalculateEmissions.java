package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by evangelie on 11/11/2016.
 */
public class CalculateEmissions implements Job {
    private Logger logger = Logger.getLogger(CalculateEmissions.class);
    private final String activitiesUrl = "http://traffic.ijs.si/NextPin/getActivities";

    @Override
    public void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        HttpURLConnection con;
        Datastore mongoDatastore;

        try {
            mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            logger.debug(e.getMessage());
            return;
        }

        DBCollection m = mongoDatastore.getCollection( User.class );
        List userIds = m.distinct( "id", new BasicDBObject());

        //Get activities of the last week of the user.
        final ZonedDateTime input = ZonedDateTime.now();
        final ZonedDateTime startOfLastWeek = input.minusDays(GetProperties.getdays());

        long last_week = startOfLastWeek.toEpochSecond()*1000;
        long now = input.toEpochSecond()*1000;


        //for (Object accessToken : userTokens ){
        for (Object id : userIds ){
            try {
                logger.debug((String) id);

                try{
                    URL obj = new URL(activitiesUrl+"?user="+id.toString()+"&from="+last_week+"&to="+now);
                    con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");

                    String urlParameters = "from="+last_week+"&to="+now;
                    con.setRequestProperty("urlParameters", urlParameters);

                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    logger.debug(e.getMessage());
                    return;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    logger.debug(e);
                    return;
                }

                int responseCode = con.getResponseCode();
                logger.debug("\nSending 'GET' request to URL : " + activitiesUrl);
                logger.debug("Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonObj  = new JSONObject(response.toString());
                logger.debug(jsonObj.getJSONArray("data"));
                JSONArray arr = jsonObj.getJSONArray("data");
                double total_emissions = 0.0;
                double average_min_car = 0.0;
                double average_min_pt = 0.0;
                double average_min_bike = 0.0;
                double average_min_walk = 0.0;
                if (arr != null && arr.length() > 0 ) {

                    double min_car = 0;
                    double min_bike= 0;
                    double min_pt = 0;
                    double min_walk = 0;
                    for (int i = 0; i < arr.length(); i++) {

                        double emissions = 0.0;

                        JSONObject object = arr.getJSONObject(i);

                        String mode = getMode(object);

                        double duration = Double.parseDouble(object.get("duration").toString()) / 60000;

                        //Get Distance
                        double distance = Double.parseDouble(object.get("distance").toString());

                        if ("IN_CAR".equals(mode)) {
                            emissions = ((double) (distance * 110) / 1000);
                            min_car = min_car + duration;
                        }
                        if ("ON_TRAIN".equals(mode) || "IN_BUS".equals(mode) ) {
                            emissions = ((distance * 25.5) / 1000);
                            min_pt = min_pt + duration;
                        }
                        if ("ON_BICYCLE".equals("mode")) {
                            emissions = 0;
                            min_bike = min_bike + duration;
                        }
                        if ("ON_FOOT".equals(mode) || "WALKING".equals(mode)) {
                            emissions = 0;
                            min_walk = min_walk + duration;
                        }
                        total_emissions = total_emissions + emissions;
                    }

                    //Calculate average minutes per day for each mode
                    average_min_car = ((double) (min_car) / (double) GetProperties.getDuration());
                    average_min_pt = ((double) (min_pt) / (double) GetProperties.getDuration());
                    average_min_bike = ((double) (min_bike) / (double) GetProperties.getDuration());
                    average_min_walk = ((double) (min_walk) / (double) GetProperties.getDuration());

                }


                Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                //Update the emissionsLastWeek field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("emissionsLastWeek", total_emissions), true);

                //Update the AverageEmissions field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinBiked", average_min_bike), true);
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinDrived", average_min_car), true);
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinWalked", average_min_walk), true);
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinPT", average_min_pt), true);

            } catch (Exception e) {

                Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                //Update the emissionsLastWeek field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("emissionsLastWeek", 0.0), true);

                //Update the AverageEmissions field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinBiked", 0.0), true);
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinDrived", 0.0), true);
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinWalked", 0.0), true);
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("MinPT", 0.0), true);
                logger.debug(e.getMessage());
            }

        }
        //Calculate Average Emissions
        DBCollection mongo = mongoDatastore.getCollection( User.class );
        for (Object current_id : userIds ) {
            try {
                double total_emissions=0.0;
                Integer total_users = 0;
                for (Object id : userIds ) {
                    try {
                        Query<User> user = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                        if ( !( (String)current_id).equals( (String) id) ) {
                            try {
                                total_emissions = total_emissions + user.get().getEmissionsLastWeek();
                                total_users++;
                            }
                            catch (Exception e){
                                total_users++;
                                logger.debug(e.getMessage());
                            }
                        }
                    } catch (Exception e) {

                        logger.debug(e.getMessage());
                    }
                }
                double AverageEmissions = total_emissions/(double)total_users;
                Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) current_id);
                //Update the AverageEmissions field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("AverageEmissions", AverageEmissions), true);


            } catch (Exception e) {
                Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) current_id);
                //Update the AverageEmissions field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("AverageEmissions", 0.0), true);
                logger.debug(e.getMessage());

            }
        }


            }

    public String getMode(JSONObject object) throws JSONException {
        String mode = "";
        try {
            JSONObject sensor = object.getJSONObject("sensor_activity_all");
            Double max = 0.0;
            for (int j = 0; j < sensor.length(); j++)
            {
                String key = sensor.names().getString(j);
                Double value = Double.parseDouble(sensor.get(key).toString());
                if (value> max){
                    max = value;
                    mode = key;
                }
            }
        }
        catch (Exception e){
            logger.debug(e.getMessage());
            return "UNKNOWN";
        }

        return mode;
    }


}
