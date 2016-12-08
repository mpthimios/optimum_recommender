package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.user.ModeUsage;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.time.Instant;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

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
            e.printStackTrace();
            return;
        }

        DBCollection m = mongoDatastore.getCollection( User.class );
        List userTokens = m.distinct( "access_token", new BasicDBObject());

        try{
            URL obj = new URL(activitiesUrl);
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            //Get activities of the last week of the user.
            final ZonedDateTime input = ZonedDateTime.now();
            final ZonedDateTime startOfLastWeek = input.minusWeeks(1);

            long last_week = startOfLastWeek.toEpochSecond()*1000;
            long now = input.toEpochSecond()*1000;
            String urlParameters = "from="+last_week+"&to="+now;
            con.setRequestProperty("urlParameters", urlParameters);

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        for (Object accessToken : userTokens ){
            try {
                logger.debug((String) accessToken);
                con.setRequestProperty("token",(String) accessToken);
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
                //print result
                logger.debug(response.toString());

                JSONObject jsonObj  = new JSONObject(response.toString());
                logger.debug(jsonObj.getJSONArray("data"));
                JSONArray arr = jsonObj.getJSONArray("data");
                double total_emissions = 0.0;
                logger.debug(arr);
                for (int i = 0; i < arr.length(); i++) {
                    String mode="";
                    double emissions = 0.0;

                    JSONObject object = arr.getJSONObject(i);
                    Integer sensorActivity = Integer.parseInt(object.get("sensorActivity").toString());

                    if(sensorActivity ==1){
                        mode = "bike";
                    } else  if(sensorActivity==9 || sensorActivity == 10) {
                        mode = "pt";
                    } else if(sensorActivity==7|| sensorActivity==2 || sensorActivity==3 || sensorActivity==4){
                        mode = "walk";
                    } else  if(sensorActivity==8 || sensorActivity==11 || sensorActivity==12 || sensorActivity==0){
                        mode = "car";
                    }
                    else{
                        mode="question";
                    }
                    //Get Distance
                    double distance = 100;
                    if (mode.equals("car") ){
                        emissions = ( (double)(distance*110)/1000 );
                    }
                    if (mode.equals("pt") ){
                        emissions = ( (distance*25.5)/1000 );
                    }
                    if (mode.equals("bike") ){
                        emissions = 0;
                    }
                    if (mode.equals("walk") ){
                        emissions = 0;
                    }
                    total_emissions = total_emissions + emissions;
                    //logger.debug(arr.getJSONObject(i).get("mode"));
                }


                Query<User> query = mongoDatastore.createQuery(User.class).field("access_token").equal((String) accessToken);
                logger.debug(total_emissions);
                //Update the emissionsLastWeek field
                mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("emissionsLastWeek", total_emissions));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
