package imu.recommender;

import imu.recommender.helpers.*;

import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import imu.recommender.models.message.Message;
import imu.recommender.models.strategy.Strategy;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import imu.recommender.models.route.RouteModel;
import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.models.user.User;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class Recommender {
	
	private Logger logger = Logger.getLogger(Recommender.class);
	RouteFormatRoot originalRouteFormatRoutes = null;
	List<RouteModel> routes = null;
	List<RouteModel> filteredRoutes = null;
	final ObjectMapper mapper = new ObjectMapper();
	User user = null;

	public Recommender(){
		//nothing to do for now 
	}
	
	public Recommender(RouteFormatRoot originalRouteFormatRoutes, User user) throws JsonParseException, JsonMappingException, IOException{
		this.originalRouteFormatRoutes = originalRouteFormatRoutes;
		this.user = user;
		initialize();
	}
	
	private void initialize(){
		routes = new ArrayList<RouteModel>();		
		for (int i = 0; i < originalRouteFormatRoutes.getRoutes().size(); i++) {
			RouteModel recommenderRoute = new RouteModel(originalRouteFormatRoutes.getRoutes().get(i));
			recommenderRoute.calculateEmissions();
			recommenderRoute.setMode();
			if (user != null){
				recommenderRoute.setBehaviouralModelUtility(BehaviouralModel.calculateBhaviouralModelUtility(recommenderRoute, user));
			}
			else{
				recommenderRoute.setBehaviouralModelUtility(0.0);
			}
			routes.add(recommenderRoute);
			logger.debug("route mode: " + recommenderRoute.getRoute().getAdditionalInfo().get("mode") +
					" emissions: " + recommenderRoute.getEmissions());
			recommenderRoute.setRouteId(i+1);
			routes.add(recommenderRoute);
			logger.debug("route mode: " + recommenderRoute.getRoute().getAdditionalInfo().get("mode"));
			logger.debug("route id: " + recommenderRoute.getRoute().getAdditionalInfo().get("routeId"));
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
	public void filterRoutesForUser(User user){
		
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
			logger.debug("MODE: " + mode);
			if ((mode == RecommenderModes.CAR) || (mode == RecommenderModes.PARK_AND_RIDE)) {
				if(!car_owner) {
					continue;
				}
				else {
					filteredRoutes.add(recommenderRoute);
				}
			}
//					//Filter out bike modes for users that don’t own a bike and for routes containing biking more than 3 Km
			else if (mode == RecommenderModes.BICYCLE){
				if((bike_owner) && (recommenderRoute.getRoute().getDistanceMeters()<GetProperties.getMaxBikeDistance())){
					filteredRoutes.add(recommenderRoute);
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode == RecommenderModes.WALK){
				logger.debug("recommenderRoute.getRoute().getDistanceMeters(): " + recommenderRoute.getRoute().getDistanceMeters());
				logger.debug("GetProperties.getMaxWalkingDistance(): " + GetProperties.getMaxWalkingDistance());
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

		
		List<RouteModel> rankedRoutesBasedonUserPreferences = rankBasedonUserPreferences(user, routes);
		
		//List<RouteModel> rankedRoutes = this.rankBasedonCO2();		
		
		List<RouteModel> rankedRoutesBasedOnSystemUtility = rankBasedonSystemView(user, routes);
		
		List<RouteModel> rankedRoutesBasedOnBehaviouralModel = rankBasedonBehaviouralModel(user, routes);
				
		//Implement Borda count
		ArrayList<Ballot> ballot = getBallots(routes);
		VotingSystem votingSystem = new Borda(ballot.toArray(new Ballot[ballot.size()]));
		logger.debug(votingSystem.results());
		String[] sortedRoutesId = votingSystem.getSortedCandidateList();
		List<RouteModel> FinalRankedRoutes = new ArrayList<RouteModel>(routes.size());

		for (RouteModel route : routes){
			String routeId = String.valueOf(route.getRouteId());
			int routeIndex = Arrays.binarySearch(sortedRoutesId, routeId);
			FinalRankedRoutes.add(routeIndex, route);
		}		
		logger.debug(FinalRankedRoutes);
		logger.debug("----");
		
		routes.clear();
		routes = FinalRankedRoutes;
		selectTargetRouteandAddMessageForUser(user);
	}
	
	private List<RouteModel> rankBasedonBehaviouralModel(User user, List<RouteModel> routes){

		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();
		TreeMap<Double, ArrayList<RouteModel>> tmp = new TreeMap<Double, ArrayList<RouteModel>>();

		for (RouteModel route : routes){
			if (tmp.containsKey(route.getBehaviouralModelUtility())){
				tmp.get(route.getBehaviouralModelUtility()).add(route);
			}
			else{
				ArrayList<RouteModel> newArrayList = new ArrayList<RouteModel>();
				newArrayList.add(route);
				tmp.put(route.getBehaviouralModelUtility(), newArrayList);
			}
		}
		
		for (Map.Entry<Double, ArrayList<RouteModel>> entry : tmp.entrySet()){
			logger.debug("CO2: " + entry.getKey());
			for (RouteModel route : entry.getValue()){
				rankedRoutes.add(route);
				logger.debug("route Mode: " + route.getRoute().getAdditionalInfo().get("mode"));
			}
		}

		return rankedRoutes;
	}
	
	private List<RouteModel> rankBasedonUserPreferences(User user, List<RouteModel> routes){
		//if there are no preferences for this time of day get preferences for any time of day
		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();

		TreeMap<Double, Integer> userPreferedModes = new TreeMap<Double, Integer>();
		try {
			userPreferedModes.put(user.getMode_usage().getCar_percent(), RecommenderModes.CAR);
			userPreferedModes.put(user.getMode_usage().getBike_percent(), RecommenderModes.BICYCLE);
			userPreferedModes.put(user.getMode_usage().getWalk_percent(), RecommenderModes.WALK);
			userPreferedModes.put(user.getMode_usage().getPt_percent(), RecommenderModes.PUBLIC_TRANSPORT);
//			userPreferedModes.put(10.0, RecommenderModes.BIKE_AND_RIDE);
//			userPreferedModes.put(12.0, RecommenderModes.PARK_AND_RIDE_WITH_BIKE);
//			userPreferedModes.put(6.0, RecommenderModes.PARK_AND_RIDE);
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
		
		Map<Integer, RouteModel> rankedRoutesMap = new LinkedHashMap<Integer, RouteModel>();
		for (Map.Entry<Double, Integer> entry : userPreferedModes.entrySet()){
			logger.debug("preference: " + entry.getKey() + " mode: " + entry.getValue());
			rankedRoutesMap.put(entry.getValue(), routes.get(0));
		}
		
		for (RouteModel route : routes){
			Integer number = route.getMode();
			rankedRoutesMap.replace(number,route);			
		}
		
		double userPreferenceRank = 1.0;
		for (Map.Entry<Integer, RouteModel> entry : rankedRoutesMap.entrySet()){
			RouteModel route = entry.getValue();
			route.setUserPreferenceRank(userPreferenceRank);
			rankedRoutes.add(route);
			userPreferenceRank++;
		}

		return rankedRoutes;
		
	}

	private List<RouteModel> rankBasedonSystemView(User user, List<RouteModel> routes){

		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();
		Map<Integer, Double> rankedRoutesMap = new HashMap<Integer, Double>();
		
		double max_emissions=0.0;
		//Find max emissions
		for (RouteModel route : routes){
			if (route.getEmissions() > max_emissions){
				max_emissions=route.getEmissions();
			}
		}
		int r_id=1;
		for (RouteModel route : routes){
			//Get distance of route
			Integer route_distance = route.getRoute().getDistanceMeters();
			//Get the mode of route
			int mode = route.getMode();
			//route.setRouteId(r_id);
			//r_id++;
			Integer routeId = route.getRouteId();
			//Return 1 if context exists else return 0
			double BikeDistance = boolToDouble(CalculateMessageUtilities.withinBikeDistance(route_distance));
			double WalkDistance = boolToDouble(CalculateMessageUtilities.withinWalkingDistance(route_distance));
			double ManyPT = boolToDouble(user.tooManyPublicTransportRoutes());
			double ManyCar = boolToDouble(user.tooManyCarRoutes());
			double Emissions = boolToDouble(user.emissionsIncreasing());
			//double NiceWeather = boolToDouble(WeatherInfo.isWeatherNice(lat, lon, city))
			double NiceWeather = 1.0;
			double Duration = 0.0;

			//Calculate utility of route based on the mode
			double context_utility = 0.0;
			double emissions_utility = 0.0;
			double utility = 0.0;
			switch (mode) {
				case RecommenderModes.WALK:
					if (ManyCar == 1.0){
						context_utility = ( 0.4218*WalkDistance + 0.3228*Duration + 0.0456*ManyCar + 0.0777*Emissions +0.1321*NiceWeather)/5.0;
					}
					else if (ManyPT == 1.0){
						context_utility = ( 0.4074*WalkDistance + 0.3157*Duration + 0.0353*ManyPT + 0.0776*Emissions +0.164*NiceWeather)/5.0;
					}
					else {
						context_utility = ( 0.4*WalkDistance + 0.3*Duration + 0.1*Emissions +0.2*NiceWeather)/4.0;
					}
					emissions_utility = 0.0;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put(RecommenderModes.WALK, utility);
					rankedRoutesMap.put(routeId,utility);
					break;
				case RecommenderModes.BICYCLE:
				case RecommenderModes.BIKE_SHARING:					
					if (ManyCar == 1.0){
						context_utility = ( 0.422*BikeDistance + 0.3228*Duration + 0.0456*ManyCar + 0.0777*Emissions +0.1321*NiceWeather)/5.0;
					}
					else if (ManyPT == 1.0){
						context_utility = ( 0.4074*BikeDistance + 0.3157*Duration + 0.0353*ManyPT + 0.0776*Emissions +0.164*NiceWeather)/5.0;
					}
					else {
						context_utility = ( 0.4*BikeDistance + 0.3*Duration + 0.1*Emissions +0.2*NiceWeather)/4.0;
					}
					emissions_utility = 0.0;
					utility = (context_utility + (1-emissions_utility) )/2;					
					rankedRoutesMap.put(routeId,utility);
					break;
				case RecommenderModes.BIKE_AND_RIDE:
					if (ManyCar == 1.0) {
						context_utility = (0.0901 * ManyCar + 0.5152 * Duration + 0.179 * Emissions + 0.2157 * NiceWeather) / 4.0;
					}
					else if (ManyPT == 1.0) {
						context_utility = (0.049 * ManyCar + 0.5193 * Duration + 0.1958 * Emissions + 0.2359 * NiceWeather) / 4.0;
					}
					else{
						context_utility = ( 0.422*BikeDistance + 0.3228*Duration + 0.0777*Emissions +0.1321*NiceWeather)/4.0;
					}
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.BIKE_AND_RIDE, utility);
					rankedRoutesMap.put(routeId,utility);
					break;
				case RecommenderModes.PUBLIC_TRANSPORT:
					context_utility = ( 0.5125*Duration + 0.0949*ManyCar + 0.315*Emissions +0.0775*NiceWeather)/4.0;
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.PUBLIC_TRANSPORT, utility);
					rankedRoutesMap.put(routeId,utility);
					break;
				case RecommenderModes.PARK_AND_RIDE:
				case RecommenderModes.PARK_AND_RIDE_WITH_BIKE:
					context_utility = ( 0.5152*Duration + 0.0901*ManyCar + 0.179*Emissions +0.2157*NiceWeather)/4.0;
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.PARK_AND_RIDE, utility);
					rankedRoutesMap.put(routeId,utility);
					break;
				case RecommenderModes.CAR:
					context_utility = 0.0001;
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.CAR,utility);
					rankedRoutesMap.put(routeId,utility);
				default:
					break;
			}
			
			//prepei na ta pros8esw ola pairnei mono an exoun allh timh
			//rankedRoutesMap.put(route,utility);
		}

		logger.debug(rankedRoutesMap);
		logger.debug(entriesSortedByValues(rankedRoutesMap));

		Map<Integer, Double> routeMap =  new LinkedHashMap<Integer, Double>();
		for(int k=0; k<entriesSortedByValues(rankedRoutesMap).size(); k++){
			routeMap.put( entriesSortedByValues(rankedRoutesMap).get(k).getKey(), entriesSortedByValues(rankedRoutesMap).get(k).getValue());
		}

		System.out.println(routeMap);

		Map<Integer, RouteModel> rankedRoutesMap2 = new LinkedHashMap<Integer, RouteModel>();

		for (Map.Entry<Integer, Double> entry : routeMap.entrySet()){
			logger.debug("mode: " + entry.getKey() + " utility: " + entry.getValue());
			rankedRoutesMap2.put(entry.getKey(), routes.get(entry.getKey()-1));
		}

		for (Map.Entry<Integer, RouteModel> entry : rankedRoutesMap2.entrySet()){
			rankedRoutes.add(entry.getValue());
		}

		return rankedRoutes;
	}


	private List<RouteModel> rankBasedonCO2(){
		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();

		TreeMap<Double, ArrayList<RouteModel>> co2RankedRoutes = new TreeMap<Double, ArrayList<RouteModel>>();
		
		for (RouteModel route : routes){
			if (co2RankedRoutes.containsKey(route.getEmissions())){
				co2RankedRoutes.get(route.getEmissions()).add(route);
			}
			else{
				co2RankedRoutes.put(route.getEmissions(), new ArrayList<RouteModel>());
				co2RankedRoutes.get(route.getEmissions()).add(route);
			}
		}

		for (Map.Entry<Double, ArrayList<RouteModel>> entry : co2RankedRoutes.entrySet()){
			logger.debug("CO2: " + entry.getKey());
			for (RouteModel route : entry.getValue()){
				rankedRoutes.add(route);
				logger.debug("route Mode: " + route.getRoute().getAdditionalInfo().get("mode"));
			}
		}
		//logger.debug("rankedRoutes: " + rankedRoutes.size() + " routes");
		return rankedRoutes;
		
	}
	
	private void selectTargetRouteandAddMessageForUser(User user){
		//Select target route and add message and strategy.
		List<String> targetList = user.getTargetList();
		logger.debug(targetList);
		String target = "";
		String mes="";
		String message = "";
		String strategy = "";
		for (int i = 0; i < targetList.size(); i++) {
			for (RouteModel route : routes) {
				//logger.debug(targetList.get(i));
				//logger.debug(route.getRoute().getAdditionalInfo().get("mode"));
				if (route.getRoute().getAdditionalInfo().get("mode") == targetList.get(i)) {
					target = targetList.get(i);
					break;
				}
			}
		}
		logger.debug(target);
		List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
		for (RouteModel route : routes) {
			if (route.getRoute().getAdditionalInfo().get("mode") == target){
				try {
					mes = CalculateMessageUtilities.calculateForUser( this, route, user);
					message = mes.split("_")[0];
					strategy = mes.split("_")[1];
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				message = "";
				strategy = "";
			}
			route.setMessage(message);
			route.setStrategy(strategy);
			rankedRoutes2.add(route);
		}
		routes.clear();
		routes=rankedRoutes2;

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

	//This function converts boolean to Integer
	private double boolToDouble( boolean b ) {
		if (b)
			return 1.0;
		return 0.0;
	}

	private ArrayList<Ballot> getBallots(List<RouteModel> routes){
		//Add routes as candidates
		ArrayList<Ballot> ballots = new ArrayList<Ballot>();
		int max= routes.size();

		for (RouteModel route : routes){
			double userPreferenceRank = route.getUserPreferenceRank();
			double systemRank =  route.getSystemRank();
			double behaviouralModelRank =  route.getBehaviouralModelRank();
			for (int i = 0; i < (max-userPreferenceRank)+1; i++) {
				ballots.add(new Ballot(String.valueOf(route.getRouteId())));
			}
			for (int i = 0; i < (max-systemRank)+1; i++) {
				ballots.add(new Ballot(String.valueOf(route.getRouteId())));
			}
			for (int i = 0; i < (max-behaviouralModelRank)+1; i++) {
				ballots.add(new Ballot(String.valueOf(route.getRouteId())));
			}		
		}
		return ballots;
	}

	static <K,V extends Comparable<? super V>>
	List<Map.Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

		List<Map.Entry<K,V>> sortedEntries = new ArrayList<Map.Entry<K,V>>(map.entrySet());

		Collections.sort(sortedEntries,
				new Comparator<Map.Entry<K,V>>() {
					@Override
					public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
						return e2.getValue().compareTo(e1.getValue());
					}
				}
		);

		return sortedEntries;
	}


}
