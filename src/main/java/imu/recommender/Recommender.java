package imu.recommender;

import imu.recommender.helpers.*;

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
		List<RouteModel> rankedRoutes = rankBasedonUserPreferences(user);
		//List<RouteModel> rankedRoutes = this.rankBasedonCO2();
		routes.clear();
		routes = rankedRoutes;
		List<RouteModel> rankedSystemRoutes = rankBasedonSystemView(user);
		double SystemRank= 1.0;
		int i=0;
		routes.clear();
		routes = rankedSystemRoutes;
		List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
		for (RouteModel route : rankedSystemRoutes ){
			RouteModel m = new RouteModel(route.getRoute());
			//Route n = new Route();
			Route n = new Route().setFrom(route.getRoute().getFrom()).setBoundingBox(route.getRoute().getBoundingBox().get()).setTo(route.getRoute().getTo()).setDistanceMeters(route.getRoute().getDistanceMeters()).setDurationSeconds(route.getRoute().getDurationSeconds()).setEndTime(route.getRoute().getEndTime()).setSegments(route.getRoute().getSegments()).setStartTime(route.getRoute().getStartTime()).setOptimizedFor(route.getRoute().getOptimizedFor().toString()).setId(route.getRoute().getId().toString());
			Map<String, Object> additionalInfo = m.getRoute().getAdditionalInfo();
			additionalInfo.put("SystemRank", SystemRank);
			n.setAdditionalInfo(additionalInfo);
			m.setRoute(n);
			SystemRank++;
			rankedRoutes2.add(m);
		}
		System.out.println("-----");
		//System.out.println(rankedRoutes2.get(3).getRoute().getAdditionalInfo());
		System.out.println(rankedRoutes2.get(2).getRoute().getAdditionalInfo());
		//Implement Borda count
		ArrayList<Ballot> b = getBallots(rankedRoutes2);
		VotingSystem system = new Borda(b.toArray(new Ballot[b.size()]));
		System.out.println(system.results());
		String[] sortedRoutesId = system.getSortedCandidateList();
		List<RouteModel> FinalrankedRoutes = new ArrayList<RouteModel>();
		for (String e:sortedRoutesId){
			for (int k=0;k<rankedRoutes2.size();k++) {
				if ( rankedRoutes2.get(k).getRoute().getAdditionalInfo().get("routeId").toString().equals(e)){
					FinalrankedRoutes.add(rankedRoutes2.get(k));
					break;
				}
			}
		}
		System.out.println(FinalrankedRoutes);
		System.out.println("----");
		/*List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();

		TreeMap<Double, RouteModel> FinalRankedRoutes = new TreeMap<Double, RouteModel>();

		for (RouteModel route : routes){
			double SystemRank = route.getSystemRank();
			double UserRank = route.getUserPreferencesRank();
			double finalRank = (SystemRank + UserRank)/2;
			System.out.println(finalRank);
			FinalRankedRoutes.put(finalRank, route);

		}

		for (Map.Entry<Double, RouteModel> entry : FinalRankedRoutes.entrySet()){
			rankedRoutes2.add(entry.getValue());
		}*/

		routes.clear();
		routes = FinalrankedRoutes;
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
		
		Map<Integer, RouteModel> rankedRoutesMap = new LinkedHashMap<Integer, RouteModel>();
		for (Map.Entry<Double, Integer> entry : userPreferedModes.entrySet()){
			logger.debug("preference: " + entry.getKey() + " mode: " + entry.getValue());
			rankedRoutesMap.put(entry.getValue(), routes.get(0));
		}
		
		for (RouteModel route : routes){
			Integer number = route.getMode();
			rankedRoutesMap.replace(number,route);
			//rank.add(number,route);
		}
		
		for (Map.Entry<Integer, RouteModel> entry : rankedRoutesMap.entrySet()){
			rankedRoutes.add(entry.getValue());
		}

		double userPreferencesRank= 1.0;

		List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
		for (RouteModel route : rankedRoutes ){
			RouteModel m = new RouteModel(route.getRoute());
			Route n = new Route().setFrom(route.getRoute().getFrom()).setBoundingBox(route.getRoute().getBoundingBox().get()).setTo(route.getRoute().getTo()).setDistanceMeters(route.getRoute().getDistanceMeters()).setDurationSeconds(route.getRoute().getDurationSeconds()).setEndTime(route.getRoute().getEndTime()).setSegments(route.getRoute().getSegments()).setStartTime(route.getRoute().getStartTime()).setOptimizedFor(route.getRoute().getOptimizedFor().toString()).setId(route.getRoute().getId().toString());
			Map<String, Object> additionalInfo = m.getRoute().getAdditionalInfo();
			additionalInfo.put("userPreferencesRank", userPreferencesRank);
			n.setAdditionalInfo(additionalInfo);
			m.setRoute(n);
			userPreferencesRank++;
			rankedRoutes2.add(m);
		}
		return rankedRoutes2;
		
	}

	private List<RouteModel> rankBasedonSystemView(User user){

		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();
		Map<Integer, Double> rankedRoutesMap = new HashMap<Integer, Double>();
		//MyComparator comp=new MyComparator(rankedRoutesMap);
		//TreeMap<Integer, Double> rankedRoutesMap1 = new TreeMap<Integer, Double>();
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
			Integer route_distance=route.getRoute().getDistanceMeters();
			//Get the mode of route
			String mode = route.getRoute().getAdditionalInfo().get("mode").toString();
			route.setRouteId(r_id);
			r_id++;
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
			if(mode.equals("walk")){
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
			}
			else if(mode.equals("bicycle") || mode.equals("bikeSharing")){
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
				//rankedRoutesMap.put(RecommenderModes.BICYCLE, utility);
				rankedRoutesMap.put(routeId,utility);
			}
			else if(mode.equals("bike&ride")){
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
			}
			else if(mode.equals("pt")){
				context_utility = ( 0.5125*Duration + 0.0949*ManyCar + 0.315*Emissions +0.0775*NiceWeather)/4.0;
				emissions_utility = route.getEmissions()/max_emissions;
				utility = (context_utility + (1-emissions_utility) )/2;
				//rankedRoutesMap.put( RecommenderModes.PUBLIC_TRANSPORT, utility);
				rankedRoutesMap.put(routeId,utility);
			}
			else if(mode.equals("park&ride")){
				context_utility = ( 0.5152*Duration + 0.0901*ManyCar + 0.179*Emissions +0.2157*NiceWeather)/4.0;
				emissions_utility = route.getEmissions()/max_emissions;
				utility = (context_utility + (1-emissions_utility) )/2;
				//rankedRoutesMap.put( RecommenderModes.PARK_AND_RIDE, utility);
				rankedRoutesMap.put(routeId,utility);
			}
			else if(mode.equals("car")){
				context_utility = 0.0001;
				emissions_utility = route.getEmissions()/max_emissions;
				utility = (context_utility + (1-emissions_utility) )/2;
				//rankedRoutesMap.put( RecommenderModes.CAR,utility);
				rankedRoutesMap.put(routeId,utility);
			}
			//prepei na ta pros8esw ola pairnei mono an exoun allh timh
			//rankedRoutesMap.put(route,utility);
		}

		System.out.println(rankedRoutesMap);
		System.out.println(entriesSortedByValues(rankedRoutesMap));


		Map<Integer, RouteModel> rankedRoutesMap2 = new LinkedHashMap<Integer, RouteModel>();

		for (Map.Entry<Integer, Double> entry : rankedRoutesMap.entrySet()){
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
		List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
		for (RouteModel route : routes) {
			if (route.getRoute().getAdditionalInfo().get("mode") == target){
				try {
					mes = CalculateMessageUtilities.calculateForUser( this, route, user);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				mes = "";
			}
			route.setMessage(mes);
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
			double userpref = Double.parseDouble(route.getRoute().getAdditionalInfo().get("userPreferencesRank").toString());
			double system =  Double.parseDouble(route.getRoute().getAdditionalInfo().get("SystemRank").toString());
			for (int i = 0; i < (max-userpref)+1; i++) {
				ballots.add(new Ballot(route.getRoute().getAdditionalInfo().get("routeId").toString()));
			}
			for (int i = 0; i < (max-system)+1; i++) {
				ballots.add(new Ballot(route.getRoute().getAdditionalInfo().get("routeId").toString()));
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
