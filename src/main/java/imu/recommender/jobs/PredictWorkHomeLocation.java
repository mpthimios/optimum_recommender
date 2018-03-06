package imu.recommender.jobs;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.models.user.User;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by evangelia on 16/1/2018.
 */
public class PredictWorkHomeLocation implements Job {
    private Logger logger = Logger.getLogger(CalculateModeUsePercentages.class);
    private final String FrequentUrl = "http://traffic.ijs.si/NextPin/getFrequent";
    private final String VisitByHoursUrl = "http://traffic.ijs.si/NextPin/analytics/visitsByHoursOfWeek";

    public void execute(final JobExecutionContext ctx)
            throws JobExecutionException {

        HttpURLConnection con;
        HttpURLConnection con2;
        Datastore mongoDatastore;

        try {
            mongoDatastore = MongoConnectionHelper.getMongoDatastore();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
            return;
        }

        DBCollection m = mongoDatastore.getCollection( User.class );
        //List userTokens = m.distinct( "access_token", new BasicDBObject());
        List userIds = m.distinct( "id", new BasicDBObject());

        for (Object id : userIds ){
            try{
                //URL obj = new URL(activitiesUrl+"?user="+id.toString());
                URL obj = new URL(FrequentUrl);
                con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("token",id.toString());
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
                return;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
                return;
            }
            try {

                //con.setRequestProperty("user", (String) id.toString());
                int responseCode = con.getResponseCode();
                logger.debug("\nSending 'GET' request to URL : " + FrequentUrl);
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

                /*JSONObject jsonObj  = new JSONObject(response.toString());
                logger.debug(jsonObj.getJSONArray("data"));
                JSONArray arr = jsonObj.getJSONArray("data");*/
                JSONArray arr = new JSONArray(response.toString());
                ArrayList<Integer> locationIds = new ArrayList<Integer>();
                ArrayList<String> locationlat = new ArrayList<String>();
                ArrayList<String> locationlong = new ArrayList<String>();
                Integer maxWorkVisits=0;
                Integer maxHomeVisits=0;

                JSONArray sortedJsonArray = new JSONArray();

                List<JSONObject> jsonValues = new ArrayList<JSONObject>();
                for (int i = 0; i < arr.length(); i++) {
                    jsonValues.add(arr.getJSONObject(i));
                }
                Collections.sort( jsonValues, new Comparator<JSONObject>() {
                    private static final String KEY_NAME = "freq";

                    @Override
                    public int compare(JSONObject a, JSONObject b) {
                        Integer valA = 0;
                        Integer valB = 0;

                        try {
                            valA = (Integer) Integer.parseInt(a.get(KEY_NAME).toString());
                            valB = (Integer) Integer.parseInt(b.get(KEY_NAME).toString());
                        }
                        catch (JSONException e) {
                            //do something
                        }

                        //return valA.compareTo(valB);
                        //if you want to change the sort order, simply use the following:
                        return -valA.compareTo(valB);
                    }
                });

                for (int i = 0; i < arr.length(); i++) {
                    sortedJsonArray.put(jsonValues.get(i));
                }


                if (sortedJsonArray != null && sortedJsonArray.length() > 0 ) {
                    Integer limit;
                    if(sortedJsonArray.length()>3){
                        limit=2;
                    }
                    else{
                        limit=1;
                    }
                    //limit
                    for (int i = 0; i < sortedJsonArray.length(); i++) {

                        JSONObject object = sortedJsonArray.getJSONObject(i);
                        Integer locationId = Integer.parseInt(object.get("location_id").toString());
                        Integer frequency = Integer.parseInt(object.get("freq").toString());
                        String latitude = object.get("latitude").toString();
                        String longitude = object.get("longitude").toString();


                        logger.debug(locationId);
                        logger.debug(frequency);

                        locationIds.add(locationId);
                        locationlat.add(latitude);
                        locationlong.add(longitude);


                    }

                    for (int i = 0; i < locationIds.size(); i++) {

                        //URL obj2 = new URL(VisitByHoursUrl+"/"+locationIds.get(i).toString()+"?token="+id.toString());
                        URL obj2 = new URL(VisitByHoursUrl+"/"+locationIds.get(i).toString()+"?token="+id.toString());
                        //URL obj2 = new URL(VisitByHoursUrl);
                        con2 = (HttpURLConnection) obj2.openConnection();
                        con2.setRequestMethod("GET");
                        con2.setRequestProperty("token",id.toString());
                        //con2.setRequestProperty("token","luka");
                        con2.setRequestProperty("location_id",locationIds.get(i).toString());
                        //String urlParameters = "location_id="+locationIds.get(i).toString();
                        //con2.setRequestProperty("urlParameters", urlParameters);
                        logger.debug("location"+locationIds.get(i).toString());

                        int responseCode2 = con2.getResponseCode();
                        logger.debug("\nSending 'GET' request to URL : " + VisitByHoursUrl);
                        //logger.debug("Response Code : " + responseCode2);

                        BufferedReader in2 = new BufferedReader(
                                new InputStreamReader(con2.getInputStream()));
                        String inputLine2;
                        StringBuffer response2 = new StringBuffer();
                        while ((inputLine2 = in2.readLine()) != null) {
                            response2.append(inputLine2);
                        }
                        in2.close();
                        //print result
                        logger.debug(response2.toString());

                        JSONArray jsonObj2  = new JSONArray(response2.toString());
                        Integer total_visits=0;
                        Integer total_daily_visits=0;

                        for (int j = 0; j < jsonObj2.length(); j++) {
                            JSONObject object2 = jsonObj2.getJSONObject(j);
                            //logger.debug(object2);
                            Integer day_of_week = Integer.parseInt(object2.get("day_of_week").toString());
                            Integer hour_of_day = Integer.parseInt(object2.get("hour_of_day").toString());
                            Integer visit_count = Integer.parseInt(object2.get("visit_count").toString());

                            if (day_of_week<5 && (hour_of_day<9 || hour_of_day>17)){
                                total_visits=total_visits+visit_count;
                            }

                            if (day_of_week<5 && (hour_of_day>9 || hour_of_day<17)){
                                total_daily_visits=total_daily_visits+visit_count;
                            }

                        }
                        logger.debug(total_daily_visits);
                        logger.debug(total_visits);
                        if (total_daily_visits>total_visits){
                            //work place
                            if(total_daily_visits>maxWorkVisits){
                                maxWorkVisits=total_daily_visits;
                                String[] WorkLocation = {
                                        locationlat.get(i),
                                        locationlong.get(i)
                                };
                                Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                                UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("predictedWork", WorkLocation);
                                mongoDatastore.update(query, ops);
                                logger.debug(total_daily_visits+"----work---");
                            }
                        }
                        else {
                            //home place
                            if(total_visits>maxHomeVisits) {
                                maxHomeVisits = total_visits;
                                String[] HomeLocation = {
                                        locationlat.get(i),
                                        locationlong.get(i)
                                };
                                Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                                UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("predictedHome", HomeLocation);
                                mongoDatastore.update(query, ops);
                                logger.debug(total_visits+"----home---");
                            }
                        }
                        /*if (total_daily_visits > 5){
                            //work
                            String[] location = {
                                    locationlat.get(i),
                                    locationlong.get(i)
                                     };

                            Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                            UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("predictedWork", location);
                            mongoDatastore.update(query, ops);
                            logger.debug("--work");
                        }

                        if (total_visits > 5){
                            //home
                            String[] location = {
                                    locationlat.get(i),
                                    locationlong.get(i)
                            };

                            Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) id);
                            UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("predictedHome", location);
                            mongoDatastore.update(query, ops);
                            logger.debug("--home");

                        }*/
                    }
                }


            } catch (Exception e) {
                logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);

            }


        }
    }

    public static double Distance( double lat1,double lat2, double lon1, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

}
