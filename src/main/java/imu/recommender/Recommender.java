package imu.recommender;

import imu.recommender.helpers.UserPreferMode;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.RecommenderModes;
import imu.recommender.models.route.RouteModel;
import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.models.user.User;

public class Recommender {
	
	private Logger logger = Logger.getLogger(Recommender.class);
	RouteFormatRoot originalRouteFormatRoutes = null;
	List<RouteModel> routes = null;
	List<RouteModel> filteredRoutes = null;
	final ObjectMapper mapper = new ObjectMapper();
	
	public Recommender(){
		//nothing to do for now 
	}
	
	public Recommender(RouteFormatRoot originalRouteFormatRoutes) throws JsonParseException, JsonMappingException, IOException{
		this.originalRouteFormatRoutes = originalRouteFormatRoutes;
		//BehaviouralModel.U1(20.0, 20.0, );
		//getLocationValue(this.originalRouteFormatRoutes.getRequest().get().getFrom().getCoordinate().geometry.coordinates.get(0).toString(),
		//this.originalRouteFormatRoutes.getRequest().get().getFrom().getCoordinate().geometry.coordinates.get(1).toString());
		initialize();
	}
	
	private void initialize(){
		routes = new ArrayList<RouteModel>();		
		for (int i = 0; i < originalRouteFormatRoutes.getRoutes().size(); i++) {
			RouteModel recommenderRoute = new RouteModel(originalRouteFormatRoutes.getRoutes().get(i));
			recommenderRoute.calculateEmissions();
			recommenderRoute.setMode();
			routes.add(recommenderRoute);	
			logger.debug("route mode: " + recommenderRoute.getRoute().getAdditionalInfo().get("mode"));
		}
		filteredRoutes = new ArrayList<RouteModel>();
	}
	

	//filterDuplicates 
	public void filterDuplicates(){
		logger.debug("filtering duplicates - before size: " + routes.size());
		HashMap<String, RouteModel> uniquesHash = new HashMap<String, RouteModel>(); 
		try{
			for (RouteModel route : routes){
				String key = String.valueOf(route.getRoute().getDistanceMeters()) + mapper.writeValueAsString(route.getRoute().getSegments());
				if (!uniquesHash.containsKey(key)){
					uniquesHash.put(key, route);
					logger.debug("non duplicate route found");
				}
				else{
					logger.debug("DUPLICATE route found");
				}
			}
			routes.clear();
			for (Map.Entry<String, RouteModel> entry : uniquesHash.entrySet()){
				routes.add(entry.getValue());				
			}
			logger.debug("filtering duplicates - after size: " + routes.size());
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//filterRoutes for User function
	public void filterRoutesForUser (User user){
		logger.debug(routes.size());
		
		for (int i = 0; i < routes.size(); i++) {
			RouteModel recommenderRoute = routes.get(i);			
			boolean car_owner = false;
			boolean bike_owner = true;
			logger.debug(recommenderRoute.getRoute().getFrom());
			recommenderRoute.getRoute().getFrom().getCoordinate().geometry.coordinates.get(0);
			//Find the mode of the route searching segments of the route
			int mode = recommenderRoute.getMode();
			//Filter out routes
			//Filter out car and park and ride modes for users that don’t own a car.
			if(!car_owner) {
				if ((mode == RecommenderModes.CAR) || (mode == RecommenderModes.PARK_AND_RIDE)) {
					continue;
				} else {
					filteredRoutes.add(recommenderRoute);
				}
			}
			//Filter out bike modes for users that don’t own a bike and for routes containing biking more than 3 Km
			else if (mode == RecommenderModes.BICYCLE){
				if((bike_owner) && (recommenderRoute.getRoute().getDistanceMeters()<GetProperties.getMaxBikeDistance())){
					filteredRoutes.add(recommenderRoute);
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode == RecommenderModes.WALK){
				if(recommenderRoute.getRoute().getDistanceMeters()<GetProperties.getMaxWalkingDistance()){
					filteredRoutes.add(recommenderRoute);
				}				
			}
			else {
				filteredRoutes.add(recommenderRoute);
			}
		}
	}
	
	public void rankRoutesForUser (User user){
		//function aggregated
		//List<RouteModel> rankedRoutes = rankBasedonUserPreferences(user);
		List<RouteModel> rankedRoutes = this.rankBasedonCO2();
		routes.clear();
		routes = rankedRoutes;
		selectTargetRouteandAddMessageForUser(user);		
	}
	
	private List<RouteModel> rankBasedonBehaviouralModel(List<RouteModel> routes, User user){
		//todo	
		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();
		for (RouteModel route : routes){
			switch (route.getMode()){
				case RecommenderModes.WALK:
					break;
				case RecommenderModes.BICYCLE:
					break;
				case RecommenderModes.BIKE_AND_RIDE:
					break;
				case RecommenderModes.PUBLIC_TRANSPORT:
					break;
				case RecommenderModes.PARK_AND_RIDE_WITH_BIKE:
					break;
				case RecommenderModes.PARK_AND_RIDE:
					break;
				case RecommenderModes.CAR:
					break;
				default:
						break;
			}
		}
		
		return null;
	}
	
	private List<RouteModel> rankBasedonUserPreferences(User user){
		//if there are no preferences for this time of day get preferences for any time of day
		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();

		TreeMap<Double, Integer> userPreferedModes = new TreeMap<Double, Integer>();
		try {
			userPreferedModes.put(user.getMode_usage().getCar_percent(), RecommenderModes.CAR);
			userPreferedModes.put(user.getMode_usage().getBike_percent(), RecommenderModes.BICYCLE);
			userPreferedModes.put(user.getMode_usage().getWalk_percent(), RecommenderModes.WALK);
			userPreferedModes.put(user.getMode_usage().getPt_percent(), RecommenderModes.PUBLIC_TRANSPORT);
			userPreferedModes.put(10.0, RecommenderModes.BIKE_AND_RIDE);
			userPreferedModes.put(12.0, RecommenderModes.PARK_AND_RIDE_WITH_BIKE);
			userPreferedModes.put(6.0, RecommenderModes.PARK_AND_RIDE);
		}
		catch (Exception e){
			//if there are no preferences for any time of day get the default
			int i = 0;
			for (Integer order : RecommenderModes.recommenderModesOrder){
				userPreferedModes.put((double) order, i);
				i++;
			}
			e.printStackTrace();
		}
		
		Map<Integer, ArrayList<RouteModel>> rankedRoutesMap = new LinkedHashMap<Integer, ArrayList<RouteModel>>(); 
		for (Map.Entry<Double, Integer> entry : userPreferedModes.entrySet()){
			logger.debug("preference: " + entry.getKey() + " mode: " + entry.getValue());
			rankedRoutesMap.put(entry.getValue(), new ArrayList<RouteModel>());
		}
		
		for (RouteModel route : routes){
			rankedRoutesMap.get(route.getMode()).add(route);			
		}				
		
		for (Map.Entry<Integer, ArrayList<RouteModel>> entry : rankedRoutesMap.entrySet()){
			rankedRoutes.addAll(entry.getValue());
		}

		return rankedRoutes;
		
	}
	
	private List<RouteModel> rankBasedonSystemView(){
		//todo later
		return null;
	}
	
	private List<RouteModel> rankBasedonCO2(){
		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();

		TreeMap<Double, RouteModel> co2RankedRoutes = new TreeMap<Double, RouteModel>();
		
		for (RouteModel route : routes){
			co2RankedRoutes.put(route.getEmissions(), route);			
		}				
		
		for (Map.Entry<Double, RouteModel> entry : co2RankedRoutes.entrySet()){
			rankedRoutes.add(entry.getValue());
		}

		return rankedRoutes;
		
	}
	
	private void selectTargetRouteandAddMessageForUser(User user){
		//Select target route and add message
		List<String> targetList = user.getTargetList();
		logger.debug(targetList);
		String target = "";
		String mes="";
		for (int i = 0; i < targetList.size(); i++) {
			for (RouteModel route : routes) {
				logger.debug(targetList.get(i));
				logger.debug(route.getRoute().getAdditionalInfo().get("mode"));
				if (route.getRoute().getAdditionalInfo().get("mode") == targetList.get(i)) {
					target = targetList.get(i);
					break;
				}
			}
		}
		logger.debug(target);
		double userPreferencesRank = 1.0;
		for (RouteModel route : routes) {
			if (route.getRoute().getAdditionalInfo().get("mode") == target){
				try {
					mes = CalculateMessageUtilities.calculateForUser(this, route, user);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				mes = "";
			}
			route.setUserPreferencesRank(userPreferencesRank);
			userPreferencesRank++;
			route.setMessage(mes);			
		}
	}
	
	public RouteFormatRoot getFilteredRoutesResponse(){
		ArrayList<Route> routesList = new ArrayList<Route>();
		for (RouteModel route : filteredRoutes){
			routesList.add(route.getRoute());
		}
		RouteFormatRoot filtered_route = new RouteFormatRoot()
				.setRequestId(originalRouteFormatRoutes.getRequestId())
				.setRouteFormatVersion(originalRouteFormatRoutes.getRouteFormatVersion())
				.setProcessedTime(originalRouteFormatRoutes.getProcessedTime())
				.setStatus(originalRouteFormatRoutes.getStatus())
				.setCoordinateReferenceSystem(originalRouteFormatRoutes.getCoordinateReferenceSystem())
				.setRequest(originalRouteFormatRoutes.getRequest().get())
				.setRoutes(routesList);
		
		//return originalRouteFormatRoutes.toString();
		return filtered_route;
	}
	
	public RouteFormatRoot getRankedRoutesResponse(){
		ArrayList<Route> routesList = new ArrayList<Route>();
		for (RouteModel route : routes){
			routesList.add(route.getRoute());
		}
		RouteFormatRoot filtered_route = new RouteFormatRoot()
				.setRequestId(originalRouteFormatRoutes.getRequestId())
				.setRouteFormatVersion(originalRouteFormatRoutes.getRouteFormatVersion())
				.setProcessedTime(originalRouteFormatRoutes.getProcessedTime())
				.setStatus(originalRouteFormatRoutes.getStatus())
				.setCoordinateReferenceSystem(originalRouteFormatRoutes.getCoordinateReferenceSystem())
				.setRequest(originalRouteFormatRoutes.getRequest().get())
				.setRoutes(routesList);
		
		//return originalRouteFormatRoutes.toString();
		return filtered_route;
	}

	public RouteFormatRoot getOriginalRouteFormatRoutes() {
		return originalRouteFormatRoutes;
	}

	public void setOriginalRouteFormatRoutes(RouteFormatRoot originalRouteFormatRoutes) {
		this.originalRouteFormatRoutes = originalRouteFormatRoutes;
	}

	public List<RouteModel> getRoutes() {
		return routes;
	}

	public void setRoutes(List<RouteModel> routes) {
		this.routes = routes;
	}
	

}
