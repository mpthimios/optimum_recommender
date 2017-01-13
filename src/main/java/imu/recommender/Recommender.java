package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import imu.recommender.helpers.*;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import java.util.*;

import java.io.IOException;


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
		logger.debug("---------Setting Routes----------");
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
			logger.debug(i + ". route mode: " + recommenderRoute.getRoute().getAdditionalInfo().get("mode") +
					" emissions: " + recommenderRoute.getEmissions());
			recommenderRoute.setRouteId(i+1);
			routes.add(recommenderRoute);			
		}
		logger.debug("routes size: " + routes.size());
		logger.debug("---------END of Setting Routes----------");
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
		logger.debug("filtering routes for user - before size: " + routes.size());
		for (int i = 0; i < routes.size(); i++) {
			RouteModel recommenderRoute = routes.get(i);			
			boolean car_owner = true;
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
					logger.debug("filtered car route for non car owner");
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
				else{
					logger.debug("filtered bicycle route - bike owner: " + bike_owner + " route distance: " 
							+ recommenderRoute.getRoute().getDistanceMeters() + " max bike distance: " + GetProperties.getMaxBikeDistance());
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode == RecommenderModes.WALK){
				if(recommenderRoute.getRoute().getDistanceMeters()<GetProperties.getMaxWalkingDistance()){
					filteredRoutes.add(recommenderRoute);
				}
				else{
					logger.debug("filtered walk route - distance: " 
							+ recommenderRoute.getRoute().getDistanceMeters() + " max walk distance: " + GetProperties.getMaxWalkingDistance());
				}
			}
			else {
				filteredRoutes.add(recommenderRoute);
			}
		}
		routes.clear();
		for (RouteModel entry : filteredRoutes){
			routes.add(entry);				
		}
		logger.debug("filtering routes for user - after size: " + routes.size());
	}
	
	public void rankRoutesForUser (User user, Datastore mongoDatastore){
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
		List<RouteModel> FinalRankedRoutes = new ArrayList<RouteModel>(Collections.nCopies(routes.size(), null));
		for (String entry : sortedRoutesId){
			logger.debug(entry);
		}
		for (RouteModel route : routes){
			String routeId = String.valueOf(route.getRouteId());		
			//int routeIndex = Arrays.binarySearch(sortedRoutesId, routeId);
			int routeIndex = Arrays.asList(sortedRoutesId).indexOf(routeId);
			FinalRankedRoutes.set(routeIndex, route);
		}		
		logger.debug(FinalRankedRoutes);
		
		routes.clear();
		routes = FinalRankedRoutes;
		selectTargetRouteandAddMessageForUser(user, mongoDatastore);
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
		
		double rank = 0.0;
		for (Map.Entry<Double, ArrayList<RouteModel>> entry : tmp.entrySet()){			
			for (RouteModel route : entry.getValue()){
				route.setBehaviouralModelRank(rank);
				rankedRoutes.add(route);
				rank++;
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
		LinkedHashMap<RouteModel, Double> rankedRoutesMap = new LinkedHashMap<RouteModel, Double>();
		
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
			
			Integer routeId = route.getRouteId();
			//Return 1 if context exists else return 0
			double BikeDistance = boolToDouble(Context.withinBikeDistance(route_distance));
			double WalkDistance = boolToDouble(Context.withinWalkingDistance(route_distance));
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
				case (int)RecommenderModes.WALK:
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
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.BICYCLE:
				case (int)RecommenderModes.BIKE_SHARING:					
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
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.BIKE_AND_RIDE:
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
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.PUBLIC_TRANSPORT:
					context_utility = ( 0.5125*Duration + 0.0949*ManyCar + 0.315*Emissions +0.0775*NiceWeather)/4.0;
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.PUBLIC_TRANSPORT, utility);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.PARK_AND_RIDE:
				case (int)RecommenderModes.PARK_AND_RIDE_WITH_BIKE:
					context_utility = ( 0.5152*Duration + 0.0901*ManyCar + 0.179*Emissions +0.2157*NiceWeather)/4.0;
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.PARK_AND_RIDE, utility);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.CAR:
					context_utility = 0.0001;
					emissions_utility = route.getEmissions()/max_emissions;
					utility = (context_utility + (1-emissions_utility) )/2;
					//rankedRoutesMap.put( RecommenderModes.CAR,utility);
					rankedRoutesMap.put(route, utility);
				default:					
					rankedRoutesMap.put(route, 0.0);
					break;
			}
			
			//prepei na ta pros8esw ola pairnei mono an exoun allh timh
			//rankedRoutesMap.put(route,utility);
		}		
		
		rankedRoutesMap = entriesSortedByValues(rankedRoutesMap);
		
		double rank = 0.0;
		for (Map.Entry<RouteModel, Double> entry : rankedRoutesMap.entrySet()){		
			entry.getKey().setSystemRank(rank);
			rankedRoutes.add(entry.getKey());
			rank++;			
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
		double rank = 0.0;
		for (Map.Entry<Double, ArrayList<RouteModel>> entry : co2RankedRoutes.entrySet()){
			logger.debug("CO2: " + entry.getKey());
			for (RouteModel route : entry.getValue()){
				route.setCO2EmissionsRank(rank);
				rankedRoutes.add(route);				
				rank++;
			}
		}
		//logger.debug("rankedRoutes: " + rankedRoutes.size() + " routes");
		return rankedRoutes;
		
	}
	
	private void selectTargetRouteandAddMessageForUser(User user, Datastore mongoDatastore){
		//Select target route and add message and strategy.
		List<String> targetList = user.getTargetList();
		logger.debug(targetList);
		String target = "";
		String mes="";
		String message = "";
		String strategy = "";
		List<String> contextList = new ArrayList<>();
		List<String> FinaltargetList = new ArrayList<>();
		for (int i = 0; i < targetList.size(); i++) {
			for (RouteModel route : routes) {

				if (route.getRoute().getAdditionalInfo().get("mode") == targetList.get(i)) {
					target = targetList.get(i);
					try {
						contextList = Context.getRelevantContextForUser(this, route, user, mongoDatastore);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if ( Context.GetRelevantContext(target, contextList)== Boolean.TRUE ){
						//break;
						FinaltargetList.add(target);
					}
				}
			}
		}
		List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
		int j=0;
		boolean SetMessage= false;
		while (message.isEmpty() && j<FinaltargetList.size() && SetMessage==false) {
			target = FinaltargetList.get(j);
			for (RouteModel route : routes) {
				if (route.getRoute().getAdditionalInfo().get("mode") == target) {
					try {
						contextList = Context.getRelevantContextForUser(this, route, user, mongoDatastore);
						mes = CalculateMessageUtilities.calculateForUser(contextList, user, target, mongoDatastore);
						message = mes.split("_")[0];
						strategy = mes.split("_")[1];
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					message = "";
					strategy = "";
				}
				if(!message.isEmpty()) {
					route.setMessage(message);
					route.setStrategy(strategy);
					rankedRoutes2.add(route);
					SetMessage=true;
				}
			}
			j++;
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
		logger.debug("--------Setting Ballots----------");
		for (RouteModel route : routes){			
			double userPreferenceRank = route.getUserPreferenceRank();
			double systemRank =  route.getSystemRank();
			double behaviouralModelRank =  route.getBehaviouralModelRank();
			logger.debug(" route mode: " + route.getMode() + " userPreferenceRank: " + userPreferenceRank +
					" systemRank: " + systemRank + " behaviouralModelRank: " + behaviouralModelRank);
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
		logger.debug("--------END Setting Ballots----------");
		return ballots;
	}

	static <K,V extends Comparable<? super V>>
	LinkedHashMap<K, V> entriesSortedByValues(LinkedHashMap<K,V> map) {

		List<Map.Entry<K, V>> entries =
				  new ArrayList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
		  public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b){
		    return a.getValue().compareTo(b.getValue());
		  }
		});
		LinkedHashMap<K, V> sortedEntries = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : entries) {
			sortedEntries.put(entry.getKey(), entry.getValue());
		}
		return sortedEntries;
	}


}
