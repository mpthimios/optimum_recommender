package imu.recommender;

import java.io.*;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.swing.text.Segment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import at.ac.ait.ariadne.routeformat.*;
//import at.ac.ait.ariadne.routeformat.Sproute.Status;

import at.ac.ait.ariadne.routeformat.geojson.GeoJSONFeature;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONFeatureCollection;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONLineString;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONPolygon;
import at.ac.ait.ariadne.routeformat.location.Address;
import imu.recommender.models.Demographics;
import imu.recommender.models.OwnedVehicle;
import imu.recommender.models.Personality;
import imu.recommender.models.User;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import at.ac.ait.ariadne.routeformat.RouteFormatRoot;

import java.util.Optional;

import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import javafx.geometry.BoundingBox;

import org.apache.log4j.Logger;
import org.bitpipeline.lib.owm.WeatherForecastResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import sun.font.TrueTypeFont;

import static imu.recommender.CalculateMessageUtilities.calculate;
import static java.lang.System.out;

public class Recommender extends HttpServlet{

	private static boolean PRINT_JSON = true;
	private Logger logger = Logger.getLogger(Recommender.class);

	public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

		//ObjectMapper mapper = new ObjectMapper();
		//mapper.registerModule(new Jdk8Module());
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		RouteFormatRoot routes = mapper.readValue(getBody(request), RouteFormatRoot.class);

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();


	    if (PRINT_JSON){
	    	try {
				try {
					calculatePercentages("luka");
				} catch (Exception e) {
					e.printStackTrace();
				}
				GetProperties properties = new GetProperties();
				logger.debug(properties.getPasswordValues());
				RouteFormatRoot response_route = filtering(routes);
				List<Route> Trips = new ArrayList<Route>();
				for (int i = 0; i < response_route.getRoutes().size(); i++) {
					Route route = response_route.getRoutes().get(i);
					String mes="";
					if (response_route.getRoutes().get(i).getAdditionalInfo().get("mode")=="car"){
						mes =  "";
					}
					else{
						try {
							mes = calculate(response_route, route);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
					additionalInfoRouteRequest.put("mode", response_route.getRoutes().get(i).getAdditionalInfo().get("mode"));
					additionalInfoRouteRequest.put("message", mes);
					double emissions = CalculateEmissions(route);
					additionalInfoRouteRequest.put("emissions", emissions);
					response_route.getRoutes().get(i).setAdditionalInfo(additionalInfoRouteRequest);
					//Route r = Route.builder().withFrom(route.getFrom()).withTo(route.getTo()).withOptimizedFor(route.getOptimizedFor().get()).withAdditionalInfo(additionalInfoRouteRequest).withDepartureTime(route.getStartTime()).withArrivalTime(route.getEndTime()).withDistanceMeters(route.getDistanceMeters()).withDurationSeconds(route.getDurationSeconds()).withSegments(route.getSegments()).build();
					//addTrip(route, Trips);

				}
				//Integer min=response_route.getRequest().get().getAcceptedDelayMinutes().get();

				//RouteFormatRoot final_route = RouteFormatRoot.builder().withRequestId(response_route.getRequestId()).withRouteFormatVersion(response_route.getRouteFormatVersion()).withProcessedTime(response_route.getProcessedTime()).withStatus(response_route.getStatus()).withCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).withRequest(response_route.getRequest().get()).withRoutes(Trips).build();
				String routeResponseStr = response_route.toString();
				String geoJson = mapper.writeValueAsString(response_route);
				out.println(geoJson);
		    }
		    finally {

		    }
	    }
	    else{
	    	try {
		    	out.println("error");
		    }
		    finally {

		    }
	    }

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		String exampleFile = "src/main/resources/route.txt";
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		RouteFormatRoot newroute = new RouteFormatRoot();
		Route newr = new Route();
		RouteFormatRoot routes = mapper.readValue(new File(exampleFile), RouteFormatRoot.class);

		String mes = null;

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();
		boolean PRINT_JSON = true;


	    // Write the response message, in an HTML page
	    if (PRINT_JSON){
	    	Datastore mongoDatastore = MongoConnectionHelper.getMongoDatastore();
	    	User newUser = new User(); 	    	
	    	mongoDatastore.save(newUser);
	    	logger.debug(newUser.getId());
	    	
	    	ArrayList<OwnedVehicle> userVehicles = new ArrayList<OwnedVehicle>();
	    	OwnedVehicle newVehicle = new OwnedVehicle();
	    	userVehicles.add(newVehicle);
	    	
	    	Query<User> query = mongoDatastore.createQuery(User.class).field("_id").equal(newUser.getId());
	    	UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("owned_vehicles", userVehicles);

	    	mongoDatastore.update(query, ops);
	    
	    	RouteFormatRoot routeFormat = new RouteFormatRoot();
	    	logger.debug(routeFormat);
	    	//String routeResponseStr = RouteParser.routeToJson(route);
	    	//out.println(routeResponseStr);
			out.println("<!DOCTYPE html>");
			out.println("<html><head>");
			out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
			out.println("<title>Echo Servlet</title></head>");
			Route route1 = routes.getRoutes().get(0);
			logger.debug(routes.getRoutes().get(0));
			Optional<Address> address =route1.getFrom().getAddress();
			if (address.isPresent()){
				java.util.Optional<String> city = address.get().getCity();
				address.get().getStreetName();
			}
			Optional<Address> address_dest=route1.getTo().getAddress();

			if (address.isPresent() && address_dest.isPresent()) {
				out.println("<h3>Alternatives Routes from " + address.get().getStreetName().get() + " to " + address_dest.get().getStreetName().get() + "					:</h3>");
			}
			try {
				//filter route
				try {
					calculatePercentages("luka");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				logger.debug(routes);
				RouteFormatRoot response_route = filtering(routes);
				logger.debug(routes.getRoutes().size());
				List<Route> Trips = new ArrayList<Route>();
				for (int i = 0; i < response_route.getRoutes().size(); i++) {
					Route route = response_route.getRoutes().get(i);
					if (response_route.getRoutes().get(i).getAdditionalInfo().get("mode")=="car"){
						mes =  "";
					}
					else{
						mes = calculate(response_route, route);
					}
					Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
					additionalInfoRouteRequest.put("mode", response_route.getRoutes().get(i).getAdditionalInfo().get("mode"));
					additionalInfoRouteRequest.put("message", mes);
					double emissions = CalculateEmissions(route);
					additionalInfoRouteRequest.put("emissions", emissions);
					//Route r = new Route().setFrom(route.getFrom()).setTo(route.getTo()).setOptimizedFor(route.getOptimizedFor().get()).setAdditionalInfo(additionalInfoRouteRequest).setStartTime(route.getStartTime()).setEndTime(route.getEndTime()).setDistanceMeters(route.getDistanceMeters()).setDurationSeconds(route.getDurationSeconds()).setSegments(route.getSegments());
							//.setRequestId(response_route.getRequestId()).setRouteFormatVersion(response_route.getRouteFormatVersion()).setProcessedTime(response_route.getProcessedTime()).setStatus(response_route.getStatus()).setCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).setRequest(response_route.getRequest().get()).setRoutes(Trips);

					response_route.getRoutes().get(i).setAdditionalInfo(additionalInfoRouteRequest);
					//Route r = Route.builder().withFrom(route.getFrom()).withTo(route.getTo()).withOptimizedFor(route.getOptimizedFor().get()).withAdditionalInfo(additionalInfoRouteRequest).withDepartureTime(route.getStartTime()).withArrivalTime(route.getEndTime()).withDistanceMeters(route.getDistanceMeters()).withDurationSeconds(route.getDurationSeconds()).withSegments(route.getSegments()).build();
					//addTrip(r, Trips);
					RouteFormatRoot final_route = new RouteFormatRoot().setRequestId(response_route.getRequestId()).setRouteFormatVersion(response_route.getRouteFormatVersion()).setProcessedTime(response_route.getProcessedTime()).setStatus(response_route.getStatus()).setCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).setRequest(response_route.getRequest().get()).setRoutes(response_route.getRoutes());

					out.println("<p>"+final_route.getRoutes().get(i).getAdditionalInfo().get("message")+"</p>");
					out.println("<p>"+final_route.getRoutes().get(i).getAdditionalInfo().get("emissions")+"</p>");
					out.println("<p>Choice " + (i + 1) + ":<span style='padding-left:68px;'>" +
							"</span>"+ final_route.getRoutes().get(i).getAdditionalInfo().get("mode")+"<span style='padding-left:68px;'></span>"
							+ final_route.getRoutes().get(i).getDurationSeconds() + "sec</p>");

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
	    }
	    else{
	    	try {
				out.println("error");
		    }
		    finally {

		    }
	    }
	}

	public static String getBody(HttpServletRequest request) throws IOException {

	    String body = null;
	    StringBuilder stringBuilder = new StringBuilder();
	    BufferedReader bufferedReader = null;

	    try {
	        InputStream inputStream = request.getInputStream();
	        if (inputStream != null) {
	            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	            char[] charBuffer = new char[128];
	            int bytesRead = -1;
	            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
	                stringBuilder.append(charBuffer, 0, bytesRead);
	            }
	        } else {
	            stringBuilder.append("");
	        }
	    } catch (IOException ex) {
	        throw ex;
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException ex) {
	                throw ex;
	            }
	        }
	    }

	    body = stringBuilder.toString();
	    return body;
	}
	//Filtering function
	public RouteFormatRoot filtering(RouteFormatRoot routes){
		List<Route> Trips = new ArrayList<Route>();
		logger.debug(routes.getRoutes().size());
		for (int i = 0; i < routes.getRoutes().size(); i++) {
			Route trip = routes.getRoutes().get(i);
			boolean car_owner = false;
			boolean bike_owner = true;
			logger.debug(trip.getFrom());
			trip.getFrom().getCoordinate().geometry.coordinates.get(0);
			//Find the mode of the route searching segments of the route
			List<String> Modes = new ArrayList<String>();
			for (int j=0; j< trip.getSegments().size(); j++) {
				RouteSegment segment = trip.getSegments().get(j);
				String mode = segment.getModeOfTransport().toString();
						//.getGeneralizedType().toString();
				if (!Modes.contains(mode)) {
					Modes.add(mode);
				}
			}
			String mode="";
			logger.debug(Modes);
			if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") && Modes.contains("BICYCLE")  ){
				mode="park&ride_with_bike";
			}
			else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") ){
				mode="park&ride";
			}
			else if (Modes.contains("BICYCLE") && Modes.contains("FOOT") && Modes.contains("PUBLIC_TRANSPORT") ){
				mode="bike&ride";
			}
			else if (Modes.contains("PUBLIC_TRANSPORT-BUS 01") && Modes.contains("FOOT-FOOT") ){
				mode="pt";
			}
			else if (Modes.contains("CAR") && Modes.contains("FOOT") ){
				mode="car";
			}
			else if (Modes.contains("FOOT") && Modes.size()==1 ){
				mode="walk";
			}
			else if (Modes.contains("BICYCLE") && Modes.size()==1 ){
				mode="walk";
			}
			else if (Modes.contains("CAR") && Modes.size()==1 ){
				mode="car";
			}
			else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.size()==1 ){
				mode="pt";
			}
			else {
				mode="unknown";
			}
			//Filter out routes
			//Filter out car and park and ride modes for users that don’t own a car.
			if(!car_owner) {
				if (mode.equals("car") || mode.equals("park&ride")) {
					continue;
				} else {
					addTrip(trip, Trips, mode);
				}
			}
			//Filter out bike modes for users that don’t own a bike and for routes containing biking more than 3 Km
			else if (mode.equals("bike")){
				if((bike_owner) && (trip.getDistanceMeters()<3000)){
					addTrip(trip,Trips, mode);
				}
				else{
					continue;
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode.equals("walk")){
				if(trip.getDistanceMeters()<1000){
					addTrip(trip,Trips,mode);
				}
				else{
					continue;
				}
			}
			else {
				addTrip(trip,Trips);
			}


		}

		//Integer min=routes.getRequest().get().getAcceptedDelayMinutes().get();
		//RoutingRequest request = RoutingRequest.builder().withAcceptedDelayMinutes(min).build();


		RouteFormatRoot filtered_route = new RouteFormatRoot().setRequestId(routes.getRequestId()).setRouteFormatVersion(routes.getRouteFormatVersion()).setProcessedTime(routes.getProcessedTime()).setStatus(routes.getStatus()).setCoordinateReferenceSystem(routes.getCoordinateReferenceSystem()).setRequest(routes.getRequest().get()).setRoutes(Trips);
				//setOptimizedFor(trip.getOptimizedFor().toString())
		logger.debug(filtered_route);
		return filtered_route;

	}

	public void addTrip(Route trip, List<Route> Trips) {
		Trips.add(trip);
	}

	public void addTrip(Route trip, List<Route> Trips, String mode) {
		Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
		additionalInfoRouteRequest.put("mode", mode);
		logger.debug(additionalInfoRouteRequest);
		Route new_trip = new Route().setFrom(trip.getFrom()).setTo(trip.getTo()).setDistanceMeters(trip.getDistanceMeters()).setDurationSeconds(trip.getDurationSeconds()).setStartTime(trip.getStartTime()).setEndTime(trip.getEndTime()).setAdditionalInfo(additionalInfoRouteRequest);
		addTrip(new_trip, Trips);
		Trips.add(new_trip);
	}

	private void calculatePercentages(String user) throws IOException {
		String url = "http://traffic.ijs.si/NextPinDev/getActivities";

		 URL obj = new URL("http://traffic.ijs.si/NextPinDev/getActivities");
		 HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		 con.setRequestMethod("GET");


		//add request header
		con.setRequestProperty("token",user);

		int responseCode = con.getResponseCode();
		logger.debug("\nSending 'GET' request to URL : " + url);
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
		 //JSONObject jsonObj = new JSONObject(response);
		 //Iterator<String> keys = jsonObj.keys();
		 try {
			 JSONObject jsonObj  = new JSONObject(response.toString());

			 //JSONArray jsonarr= json.getJSONArray("address");
			 //String address = jsonarr.getJSONObject(0).getString("addressLine1");
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
			 //percentages should be saved to mongo
			 logger.debug(car_percent);
			 logger.debug(pt_percent);
			 logger.debug(bike_percent);
			 logger.debug(walk_percent);
		 } catch (JSONException e) {
			 e.printStackTrace();
		 }


	 }

	private void CalculateOtherUserPercentages(){
		//Connect to mongodb
		MongoClient mongo = null;
		try {
			mongo = new MongoClient("euprojects.net",3368);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		//Print all database names
		//logger.debug(mongo.getDatabaseNames());
		DB db = mongo.getDB("Optimum");
		DBCollection table = db.getCollection("OptimumUsers");
		//Select the messages where persuasive strategy is Reward
		BasicDBObject searchQuery = new BasicDBObject();

	}

	//Calculate emissions of route
	private double CalculateEmissions(Route trip) throws IOException {
		double emissions = 0.0;
		for (int j = 0; j < trip.getSegments().size(); j++) {
			RouteSegment segment = trip.getSegments().get(j);
			String mode = segment.getModeOfTransport().getGeneralizedType().toString();
			String detailed_mode = segment.getModeOfTransport().getDetailedType().toString();
			Integer distance = segment.getDistanceMeters();
			emissions = emissions + CalculateSegmentsEmissions(distance, mode, detailed_mode);
			}
		return emissions;
	}

	private double CalculateSegmentsEmissions(Integer distance, String travel_mode, String detailed_mode) throws IOException {
		double emissions=0.0;
		if (travel_mode.equals("FOOT") ){
			emissions = 0;
		}
		if (travel_mode.equals("BICYCLE") ){
			emissions = 0;
		}
		if (travel_mode.equals("PUBLIC_TRANSPORT") ) {

			if (detailed_mode.equals("SUBWAY")) {
				emissions = ( (double)(distance*20)/1000 );
			}
			if (detailed_mode.equals("HEAVY_RAIL")) {
				emissions = ( (double)(distance*50)/1000 );
			}
			if (detailed_mode.equals("BUS")) {
				emissions = ( (distance*25.5)/1000 );
			}
		}
		if (travel_mode.equals("CAR") ){
			emissions = ( (double)(distance*110)/1000 );
		}

		return emissions;

	}


}
