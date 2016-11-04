package imu.recommender.jobs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import imu.recommender.MongoConnectionHelper;
import imu.recommender.Recommender;
import imu.recommender.models.ModeUsage;
import imu.recommender.models.User;

public class CalculateModeUsePercentages implements Job {
	
	private Logger logger = Logger.getLogger(CalculateModeUsePercentages.class);
	private final String activitiesUrl = "http://traffic.ijs.si/NextPinDev/getActivities";
	
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
				 Integer n_car=0;
				 Integer n_pt=0;
				 Integer n_bike=0;
				 Integer n_walk=0;
				 logger.debug(arr);
				 for (int i = 0; i < arr.length(); i++) {
					 String [] all_modes = {"car", "pt", "bike", "walk"};
					 Random random = new Random();
	
					 // randomly selects an index from the arr
					 int select = random.nextInt(all_modes.length);
					 //Put the random selected mode to string
					 arr.getJSONObject(i).put("mode", all_modes[select]);
	
					 JSONObject object = arr.getJSONObject(i);
					 String mode = object.get("mode").toString();
					 //String a = objects.get("");
					 if (mode.equals("car") ){
						 n_car++;
					 }
					 if (mode.equals("pt") ){
						 n_pt++;
					 }
					 if (mode.equals("bike") ){
						 n_bike++;
					 }
					 if (mode.equals("walk") ){
						 n_walk++;
					 }
					 //logger.debug(arr.getJSONObject(i).get("mode"));
				 }
				 //Calculate percentages
				 double car_percent = ( (double)(n_car*100)/(double) arr.length());
				 double pt_percent = ( (double)(n_pt*100)/(double) arr.length());
				 double bike_percent = ( (double)(n_bike*100)/(double) arr.length());
				 double walk_percent = ( (double)(n_walk*100)/(double) arr.length());
				 
				 //test
//				 double car_percent = 10.0;
//				 double pt_percent = 20.0;
//				 double bike_percent = 30.0;
//				 double walk_percent = 40.0;
				 
				 //percentages should be saved to mongo
				 logger.debug(car_percent);
				 logger.debug(pt_percent);
				 logger.debug(bike_percent);
				 logger.debug(walk_percent);
				 
				 Query<User> query = mongoDatastore.createQuery(User.class).field("access_token").equal((String) accessToken);
				 ModeUsage modeUsage = new ModeUsage();
				 modeUsage.setWalk_percent(walk_percent);
				 modeUsage.setPt_percent(pt_percent);
				 modeUsage.setCar_percent(car_percent);
				 modeUsage.setBike_percent(bike_percent);
				 UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("mode_usage", modeUsage);
				 mongoDatastore.update(query, ops);
				 
			 } catch (Exception e) {
				 e.printStackTrace();
			 }

    	}
    }
}