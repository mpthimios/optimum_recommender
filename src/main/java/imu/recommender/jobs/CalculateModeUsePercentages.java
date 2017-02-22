package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

public class CalculateModeUsePercentages implements Job {
	
	private Logger logger = Logger.getLogger(CalculateModeUsePercentages.class);
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
    	//List userTokens = m.distinct( "access_token", new BasicDBObject());
		List userIds = m.distinct( "id", new BasicDBObject());

    	for (Object id : userIds ){
			try{
				//URL obj = new URL(activitiesUrl+"?user="+id.toString());
				URL obj = new URL(activitiesUrl);
				con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");
				con.setRequestProperty("token",id.toString());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
    		try {
				DBObject user = m.findOne(id);

        		//con.setRequestProperty("user", (String) id.toString());
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
				double car_percent = 0.0;
				double pt_percent = 0.0;
				double bike_percent = 0.0;
				double walk_percent = 0.0;
				double bike_percent_GW = 0.0;
				double walk_percent_GW = 0.0;
				Integer total = 0;

				if (arr != null && arr.length() > 0 ) {
					Integer n_car = 0;
					Integer n_pt = 0;
					Integer n_bike = 0;
					Integer n_walk = 0;
					Integer n_bike_GW = 0;
					Integer n_walk_GW = 0;

					for (int i = 0; i < arr.length(); i++) {
						String mode = "";
						//String [] all_modes = {"car", "pt", "bike", "walk"};

						JSONObject object = arr.getJSONObject(i);

						Integer sensorActivity = Integer.parseInt(object.get("sensorActivity").toString());
						//Get location and date
						float lat = 12;
						float longitude = 23;
						Integer start = 12;
						Integer end = 13;

						//Boolean NiceWeather = WeatherInfo.isHistoricalWeatherNice(lat, longitude, start, end);
						Boolean NiceWeather = Boolean.TRUE;
						if (sensorActivity == 1) {
							mode = "bike";
						} else if (sensorActivity == 7 || sensorActivity == 2 || sensorActivity == 3 ) {
							mode = "walk";
						} else if (sensorActivity == 4) {
							mode = "sitting";
						} else if (sensorActivity == 5) {
							mode = "tilting";
						} else if (sensorActivity == 6) {
							mode = "question";
						} else if (sensorActivity == 8 || sensorActivity == 11 || sensorActivity == 12 || sensorActivity == 0) {
							mode = "car";
						} else if (sensorActivity == 9 || sensorActivity == 10) {
							mode = "pt";
						} else {
							mode = "question";
						}

						if (mode.equals("car")) {
							n_car++;

						}
						if (mode.equals("pt")) {
							n_pt++;
						}
						if (mode.equals("bike")) {
							n_bike++;
							if (NiceWeather) {
								n_bike_GW++;
							}
						}
						if (mode.equals("walk")) {
							n_walk++;
							if (NiceWeather) {
								n_walk_GW++;
							}
						}
					}
					//Calculate total activities
					total = n_bike+n_car+n_pt+n_walk;
					//Calculate percentages
					if(total!=0) {
						car_percent = ((double) (n_car * 100) / (double) total);
						pt_percent = ((double) (n_pt * 100) / (double) total);
						bike_percent = ((double) (n_bike * 100) / (double) total);
						walk_percent = ((double) (n_walk * 100) / (double) total);
						bike_percent_GW = ((double) (n_bike_GW * 100) / (double) total);
						walk_percent_GW = ((double) (n_walk_GW * 100) / (double) total);
					}
				}
				 
				 //percentages should be saved to mongo
				 Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
				 mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("total_activities", total),true);
				 ModeUsage modeUsage = new ModeUsage();
				 modeUsage.setWalk_percent(walk_percent);
				 modeUsage.setPt_percent(pt_percent);
				 modeUsage.setCar_percent(car_percent);
				 modeUsage.setBike_percent(bike_percent);
				 modeUsage.setWalk_percentGW(walk_percent_GW);
				 modeUsage.setBike_percentGW(bike_percent_GW);
				 UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("mode_usage", modeUsage);
				 mongoDatastore.update(query, ops, true);


			} catch (Exception e) {
				 e.printStackTrace();
				//percentages should be saved to mongo
				Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("total_activities", 0),true);
				ModeUsage modeUsage = new ModeUsage();
				modeUsage.setWalk_percent(0.0);
				modeUsage.setPt_percent(0.0);
				modeUsage.setCar_percent(0.0);
				modeUsage.setBike_percent(0.0);
				modeUsage.setWalk_percentGW(0.0);
				modeUsage.setBike_percentGW(0.0);
				UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("mode_usage", modeUsage);
				mongoDatastore.update(query, ops, true);

			}

    	}
		//Calculate Average Mode Use percentages
		DBCollection mongo = mongoDatastore.getCollection( User.class );
		for (Object current_id : userIds ) {
			try {
				double total_car_perc=0.0;
				double total_pt_perc=0.0;
				double total_bike_perc=0.0;
				double total_walk_perc=0.0;
				double total_bike_perc_GW=0.0;
				double total_walk_perc_GW=0.0;
				Integer total_users = 0;
				for (Object id : userIds ) {
					try {
						Query<User> user = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
						if ( !( (String)current_id).equals( (String) id) ) {
							try {
								total_car_perc = total_car_perc + user.get().getMode_usage().getCar_percent();
								total_bike_perc = total_bike_perc + user.get().getMode_usage().getBike_percent();
								total_pt_perc =total_pt_perc + user.get().getMode_usage().getPt_percent();
								total_walk_perc = total_walk_perc + user.get().getMode_usage().getWalk_percent();
								total_walk_perc_GW = total_walk_perc_GW + user.get().getMode_usage().getWalk_percentGW();
								total_bike_perc_GW = total_bike_perc_GW + user.get().getMode_usage().getBike_percentGW();
								total_users++;
							}
							catch (Exception e){
								//total_users++;
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						//
						e.printStackTrace();
					}
				}
				double bikeUsageComparedToOthers = total_bike_perc/(double)total_users;
				double ptUsageComparedToOthers = total_pt_perc/(double)total_users;
				double walkUsageComparedToOthers = total_walk_perc/(double)total_users;
				double carUsageComparedToOthers = total_car_perc/(double)total_users;
				double walkUsageComparedToOthersGW = total_walk_perc_GW/(double)total_users;
				double bikeUsageComparedToOthersGW = total_bike_perc_GW/(double)total_users;
				Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) current_id);
				//Update the AverageEmissions field
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("ptUsageComparedToOthers", ptUsageComparedToOthers),true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("carUsageComparedToOthers", carUsageComparedToOthers), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("bikeUsageComparedToOthers", bikeUsageComparedToOthers), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("walkUsageComparedToOthers", walkUsageComparedToOthers), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("bikeUsageComparedToOthersGW", bikeUsageComparedToOthersGW), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("walkUsageComparedToOthersGW", walkUsageComparedToOthersGW), true);

			} catch (Exception e) {
				e.printStackTrace();
				Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) current_id);
				//Update the AverageEmissions field
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("ptUsageComparedToOthers", 0.0),true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("carUsageComparedToOthers", 0.0), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("bikeUsageComparedToOthers", 0.0), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("walkUsageComparedToOthers", 0.0), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("bikeUsageComparedToOthersGW", 0.0), true);
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("walkUsageComparedToOthersGW", 0.0), true);

			}
		}
    }
}