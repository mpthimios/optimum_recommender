package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.location.Address;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import imu.recommender.helpers.MongoConnectionHelper;
import imu.recommender.logs.UserRouteLog;
import imu.recommender.models.user.OwnedVehicle;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Timestamp;
import java.util.*;

//import at.ac.ait.ariadne.routeformat.Sproute.Status;

public class RequestHandler extends HttpServlet{

	private static boolean PRINT_JSON = true;
	private static Logger logger = Logger.getLogger(RequestHandler.class);
	private static String exampleFile = "src/main/resources/route.txt";

	public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		
		//get the datastore
		Datastore mongoDatastore = null;
		try {
			mongoDatastore = MongoConnectionHelper.getMongoDatastore();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.error("could not get mongo datastore " + e1.getMessage(), e1);
		}
		
		//prepare the response
		// Set the response message's MIME type
	    response.setContentType("application/json; charset=UTF-8");
	    response.setCharacterEncoding("utf-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = null;
		try {
			out = response.getWriter();
		} catch (Exception e1) {
			logger.error("could not get response writer " + e1.getMessage(), e1);
		}

		String userID = "";
		User user = null;
		Boolean Baseline = Boolean.FALSE;
		Boolean Classification = Boolean.FALSE;
		Boolean NewFeature = Boolean.TRUE;
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		String requestBody = "";
		try {
			requestBody = getBody(request);
		} catch (Exception e1) {
			logger.error("could not get request body " + e1.getMessage(), e1);
		}
		//logger.debug(requestBody);
		Timestamp received = new Timestamp(System.currentTimeMillis());
		
		try{
			userID = request.getHeader("X-USER-ID");
			logger.debug("X-USER-ID");
			logger.debug(userID);
			logger.error("Starting searching for user:"+new Timestamp(System.currentTimeMillis()));
			user = User.findById(userID);
			logger.error("Found user:"+new Timestamp(System.currentTimeMillis()));

			RouteFormatRoot originalRoutes = mapper.readValue(requestBody, RouteFormatRoot.class);
			RouteFormatRoot recommendedRoutes;
			RouteFormatRoot recommendedRoutes2;
			Recommender recommenderRoutes= new Recommender(originalRoutes, user);
			recommenderRoutes.filterDuplicates();
			if (Baseline == Boolean.FALSE) {
				if (Classification == Boolean.TRUE) {
					//Create 2 groups of users. If user belongs to group A rank routes and add message
					user.classify(user, mongoDatastore);
					System.out.println(user.getPersuasion());
					if ("A".equals(user.getPersuasion())) {
						boolean filtered = recommenderRoutes.filterRoutesForUser(user);
						if (NewFeature == Boolean.TRUE){
							recommenderRoutes.rankRoutesForUserNew(user, mongoDatastore);
						}
						else {
							recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
						}
						recommendedRoutes = recommenderRoutes.getRankedRoutesResponse();
						if (filtered) {
							recommenderRoutes.addMessage(user, mongoDatastore);
						}
					} else {
						recommendedRoutes = recommenderRoutes.getRankedRoutesResponse();

					}
				}else {
					//Rank routes and add persuasive message
					//recommendedRoutes = recommenderRoutes.getRankedRoutesResponse();
					boolean filtered = recommenderRoutes.filterRoutesForUser(user);
					if (NewFeature == Boolean.TRUE){
						recommenderRoutes.rankRoutesForUserNew(user, mongoDatastore);
					}
					else {
						recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
					}					
					System.out.print(filtered);
					if (filtered) {
						recommenderRoutes.addMessage(user, mongoDatastore);
					}
					//Add Graph
					recommenderRoutes.addGraph(user, mongoDatastore);
					
					recommendedRoutes = recommenderRoutes.getRankedRoutesResponse();
				}
			}
			else {
				recommendedRoutes = recommenderRoutes.getRankedRoutesResponse();


			}

			String recommendedRoutesStr = mapper.writeValueAsString(recommendedRoutes);


			//Calculate Persononalized routes and saved the result into RouteCriteriaViolation Collection of mongodb

			/*RouteFormatRoot PersonalizedRoutes;
			//Recommender Routes= new Recommender(originalRoutes, user);
			//Routes.filterDuplicates();
			boolean filtered = recommenderRoutes.filterRoutesForUser(user);
			recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
			PersonalizedRoutes = recommenderRoutes.getRankedRoutesResponse();
			if (filtered) {
				logger.debug("---Adding message----:"+new Timestamp(System.currentTimeMillis()));
				recommenderRoutes.addMessage(user, mongoDatastore);
				logger.debug("Message added:"+new Timestamp(System.currentTimeMillis()));
			}

			String personalizedRoutesStr = mapper.writeValueAsString(PersonalizedRoutes);

			logger.debug("----Start saving to UserCriteriaViolation"+new Timestamp(System.currentTimeMillis()));
			System.out.print("----Start saving to UserCriteriaViolation"+new Timestamp(System.currentTimeMillis()));

			DBObject origin =(DBObject) JSON.parse(requestBody);

			UserRouteCriteriaViolation routeCriteriaViolation = new UserRouteCriteriaViolation();
			routeCriteriaViolation.setUserId(userID);
			routeCriteriaViolation.setPersonalizededResults((DBObject)JSON.parse(personalizedRoutesStr));
			routeCriteriaViolation.setOriginalResults(origin);
			routeCriteriaViolation.setCreatedDate(new Date());
			mongoDatastore.save(routeCriteriaViolation);*/
            DBObject origin =(DBObject) JSON.parse(requestBody);

			logger.debug("----Start saving to UserRoute"+new Timestamp(System.currentTimeMillis()));
			System.out.println("----Start saving to UserRoute"+new Timestamp(System.currentTimeMillis()));

			UserRouteLog routeLog = new UserRouteLog();
			routeLog.setUserId(userID);
			routeLog.setOriginalResults(origin);
			routeLog.setRecommendedResults((DBObject)JSON.parse(recommendedRoutesStr));
			routeLog.setCreatedDate(new Date());
			mongoDatastore.save(routeLog);

			//Trigger UpdateProbabilitiesJob

			/*Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

			scheduler.start();

			JobDetail jobDetail = newJob(UpdateStrategiesProbabilities.class).build();

			Trigger trigger = newTrigger()
					.startNow()
					.build();

			scheduler.scheduleJob(jobDetail, trigger);*/

						
			out.println(recommendedRoutesStr);
			//logger.error("Received Routes:"+recommendedRoutesStr);
			logger.error("Received Routes:"+received);
			logger.error("Response:"+new Timestamp(System.currentTimeMillis()));
		}
		catch (Exception e){
				logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
				logger.debug("user not found");
				
				Recommender recommenderRoutes = null;
				try {
					recommenderRoutes = new Recommender(mapper.readValue(requestBody, RouteFormatRoot.class), user);
					RouteFormatRoot response_route = recommenderRoutes.getOriginalRouteFormatRoutes();
					logger.debug(response_route);
					for (int i = 0; i < response_route.getRoutes().size(); i++) {
						String mes ="";
						if (response_route.getRoutes().get(i).getAdditionalInfo().get("mode") == "car") {
							mes = "";
						} else {
							try {
								//mes = CalculateMessageUtilities.calculateForUser(response_route, route, user);
							} catch (Exception ex) {
								logger.error("Exception while filtering duplicate routes: " + ex.getMessage(), ex);
							}
						}
						Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
						additionalInfoRouteRequest.put("mode", response_route.getRoutes().get(i).getAdditionalInfo().get("mode"));
						additionalInfoRouteRequest.put("message", mes);
						response_route.getRoutes().get(i).setAdditionalInfo(additionalInfoRouteRequest);
					}
					RouteFormatRoot final_route = new RouteFormatRoot()
							.setRequestId(response_route.getRequestId())
							.setRouteFormatVersion(response_route.getRouteFormatVersion())
							.setProcessedTime(response_route.getProcessedTime())
							.setStatus(response_route.getStatus())
							.setCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem())
							.setRequest(response_route.getRequest().get())
							.setRoutes(response_route.getRoutes());
					//String geoJson = mapper.writeValueAsString(routeResponseStr.toString());
					String geoJson = "";
					try {
						geoJson = mapper.writeValueAsString(final_route);
					} catch (Exception e1) {
						logger.error("Exception while getting json response string: " + e1.getMessage(), e1);
					}
					//Save routes to UserRouteLog collection of mongodb
					try {
						logger.debug("----Start saving to UserRoute" + new Timestamp(System.currentTimeMillis()));
						System.out.println("----Start saving to UserRoute" + new Timestamp(System.currentTimeMillis()));

						DBObject origin = (DBObject) JSON.parse(requestBody);

						UserRouteLog routeLog = new UserRouteLog();
						routeLog.setUserId(userID);
						routeLog.setOriginalResults(origin);
						routeLog.setRecommendedResults((DBObject) JSON.parse(geoJson));
						routeLog.setCreatedDate(new Date());
						mongoDatastore.save(routeLog);
					}
					catch (Exception e2){
						logger.debug("Exception while saving to UserRouteLog:"+e2.getMessage(),e2);
					}
					out.println(geoJson);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					logger.error("Could not get recommender routes: " + e1.getMessage(), e1);
					out.println("");
				}
				
		}		
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		RouteFormatRoot routes = null;
		try {
			routes = mapper.readValue(new File(exampleFile), RouteFormatRoot.class);
		} catch (Exception e1) {
			logger.error("Exception while getting routes from mapper: " + e1.getMessage(), e1);
		}

		String mes = null;

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = null;
		try {
			out = response.getWriter();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.error("Exception while allocating a output writer to write the response message into the network socket: " + e1.getMessage(), e1);
		}
		boolean PRINT_JSON = true;


	    // Write the response message, in an HTML page
	    if (PRINT_JSON){
	    	Datastore mongoDatastore = null;
			try {
				mongoDatastore = MongoConnectionHelper.getMongoDatastore();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				logger.error("Exception while getting mongo datastore: " + e1.getMessage(), e1);
			}
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
				address.get().getStreetName();
			}
			Optional<Address> address_dest=route1.getTo().getAddress();

			if (address.isPresent() && address_dest.isPresent()) {
				out.println("<h3>Alternatives Routes from " + address.get().getStreetName().get() + " to " + address_dest.get().getStreetName().get() + "					:</h3>");
			}
			try {
				out.println("<h3>Alternatives Routes from " + route1.getFrom().getAddress().toString() + " to " + route1.getTo().getAddress().toString()+ "					:</h3>");
				
//				logger.debug(routes);
//				RouteFormatRoot response_route = filterRoutes(routes);
//				logger.debug(routes.getRoutes().size());
//				List<Route> Trips = new ArrayList<Route>();
//				for (int i = 0; i < response_route.getRoutes().size(); i++) {
//					Route route = response_route.getRoutes().get(i);
//					if (response_route.getRoutes().get(i).getAdditionalInfo().get("mode")=="car"){
//						mes =  "";
//					}
//					else{
//						mes = calculate(response_route, route);
//					}
//					Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
//					additionalInfoRouteRequest.put("mode", response_route.getRoutes().get(i).getAdditionalInfo().get("mode"));
//					additionalInfoRouteRequest.put("message", mes);
//					//double emissions = CalculateEmissions(route);
//					//additionalInfoRouteRequest.put("emissions", emissions);
//					//Route r = new Route().setFrom(route.getFrom()).setTo(route.getTo()).setOptimizedFor(route.getOptimizedFor().get()).setAdditionalInfo(additionalInfoRouteRequest).setStartTime(route.getStartTime()).setEndTime(route.getEndTime()).setDistanceMeters(route.getDistanceMeters()).setDurationSeconds(route.getDurationSeconds()).setSegments(route.getSegments());
//							//.setRequestId(response_route.getRequestId()).setRouteFormatVersion(response_route.getRouteFormatVersion()).setProcessedTime(response_route.getProcessedTime()).setStatus(response_route.getStatus()).setCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).setRequest(response_route.getRequest().get()).setRoutes(Trips);
//
//					response_route.getRoutes().get(i).setAdditionalInfo(additionalInfoRouteRequest);
//					//Route r = Route.builder().withFrom(route.getFrom()).withTo(route.getTo()).withOptimizedFor(route.getOptimizedFor().get()).withAdditionalInfo(additionalInfoRouteRequest).withDepartureTime(route.getStartTime()).withArrivalTime(route.getEndTime()).withDistanceMeters(route.getDistanceMeters()).withDurationSeconds(route.getDurationSeconds()).withSegments(route.getSegments()).build();
//					//addTrip(r, Trips);
//					RouteFormatRoot final_route = new RouteFormatRoot().setRequestId(response_route.getRequestId()).setRouteFormatVersion(response_route.getRouteFormatVersion()).setProcessedTime(response_route.getProcessedTime()).setStatus(response_route.getStatus()).setCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).setRequest(response_route.getRequest().get()).setRoutes(response_route.getRoutes());
//
//					out.println("<p>"+final_route.getRoutes().get(i).getAdditionalInfo().get("message")+"</p>");
//					out.println("<p>"+final_route.getRoutes().get(i).getAdditionalInfo().get("emissions")+"</p>");
//					out.println("<p>Choice " + (i + 1) + ":<span style='padding-left:68px;'>" +
//							"</span>"+ final_route.getRoutes().get(i).getAdditionalInfo().get("mode")+"<span style='padding-left:68px;'></span>"
//							+ final_route.getRoutes().get(i).getDurationSeconds() + "sec</p>");
//
//				}

			} catch (Exception e) {
				logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
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

	private static String getBody(HttpServletRequest request) throws IOException {

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
	            	logger.error("Exception while closing buffered reader: " + ex.getMessage(), ex);
	            }
	        }
	    }

	    body = stringBuilder.toString();
	    return body;
	}

}
