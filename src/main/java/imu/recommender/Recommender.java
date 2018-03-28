package imu.recommender;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import imu.recommender.helpers.*;
import imu.recommender.models.request.Request;
import imu.recommender.models.request.UserRequestPerGroup;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.round;


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
	
	public Recommender(RouteFormatRoot originalRouteFormatRoutes, User user) throws IOException{
		this.originalRouteFormatRoutes = originalRouteFormatRoutes;
		this.user = user;
		initialize();
	}
	
	private void initialize(){
		routes = new ArrayList<>();
		logger.debug("---------Setting Routes----------");
		String[] location = {
				originalRouteFormatRoutes.getRoutes().get(0).getFrom().getCoordinate().getGeometry().getCoordinates().get().asNewList().get(0).toString(),
				originalRouteFormatRoutes.getRoutes().get(0).getFrom().getCoordinate().getGeometry().getCoordinates().get().asNewList().get(1).toString()
		};
		double locationValue = BehaviouralModel.getLocationValue(location[0], location[1]);
		for (int i = 0; i < originalRouteFormatRoutes.getRoutes().size(); i++) {
			RouteModel recommenderRoute = new RouteModel(originalRouteFormatRoutes.getRoutes().get(i));			
			recommenderRoute.setModeandCalculateEmissions();			
			
			if (user != null){
				recommenderRoute.setBehaviouralModelUtility(BehaviouralModel.calculateBhaviouralModelUtility(recommenderRoute, user, locationValue));
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
		filteredRoutes = new ArrayList<>();
	}
	

	//filterDuplicates 
	public void filterDuplicates(){
		logger.debug("filtering duplicates - before size: " + routes.size());
		LinkedHashMap<String, RouteModel> uniquesHash = new LinkedHashMap<>();
		try{
			for (RouteModel route : routes){
				//String key = String.valueOf(round(route.getRoute().getDistanceMeters()/1000.0)) + mapper.writeValueAsString(route.getRoute().getSegments());
				String key = String.valueOf(round(route.getRoute().getDistanceMeters()/1000.0)) + mapper.writeValueAsString(route.getRoute().getAdditionalInfo().get("mode")) + mapper.writeValueAsString(route.getRoute().getSegments().size());;
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
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
		}
	}
	
	//filterRoutes for User function
	public boolean filterRoutesForUser(User user){
		logger.debug("filtering routes for user - before size: " + routes.size());
		for (int i = 0; i < routes.size(); i++) {
			RouteModel recommenderRoute = routes.get(i);			
			boolean carOwner = false;
			boolean bikeOwner = false;
			//Get vehicles of user from mongodb
			if (user.getOwned_vehicles() != null ) {
				for (int k = 0; k < user.getOwned_vehicles().size(); k++) {
					if ("car".equals(user.getOwned_vehicles().get(k).getType())) {
						carOwner = true;
					}
					if ("bicycle".equals(user.getOwned_vehicles().get(k).getType())) {
						bikeOwner = true;
					}
				}
			}
			
			//Filter out routes based on walking and bike distance preference.
			double maxBikeDistance = user.getPersonality().convertMaxBikeDistance()+ 0.2*user.getPersonality().convertMaxBikeDistance();
			double maxWalkDistance = user.getPersonality().convertMaxWalkDistance()+ 0.2*user.getPersonality().convertMaxWalkDistance();

			//Find the mode of the route searching segments of the route
			int mode = recommenderRoute.getMode();
			//Filter out routes
			//Filter out car and park and ride modes for users that don’t own a car.
			logger.debug("MODE: " + mode);
			if ((mode == RecommenderModes.CAR) || (mode == RecommenderModes.PARK_AND_RIDE)) {
				if(!carOwner || recommenderRoute.getBikeDistance() > (int) maxBikeDistance || recommenderRoute.getWalkingDistance() > (int) maxWalkDistance) {
					logger.debug("filtered car route for non car owner - route walk distance: "+recommenderRoute.getWalkingDistance()+"max walk distance: "
							+ maxWalkDistance+" route bike distance:"+recommenderRoute.getBikeDistance()+" max bike distance:"+maxBikeDistance);					
				}
				else {
					filteredRoutes.add(recommenderRoute);
				}
			}
//					//Filter out bike modes for users that don’t own a bike and for routes containing biking more than 3 Km
			else if (mode == RecommenderModes.BICYCLE){
				if((bikeOwner) && (recommenderRoute.getBikeDistance() <= (int) maxBikeDistance) && (recommenderRoute.getWalkingDistance()<= (int) maxWalkDistance)){
					filteredRoutes.add(recommenderRoute);
				}
				else{
					logger.debug("filtered bicycle route - bike owner: " + bikeOwner + " route distance: "
							+ recommenderRoute.getRoute().getDistanceMeters() + " max bike distance: " + maxBikeDistance);
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode == RecommenderModes.WALK){
				if( recommenderRoute.getBikeDistance() <= (int) maxBikeDistance && recommenderRoute.getWalkingDistance()<= (int) maxWalkDistance){
					filteredRoutes.add(recommenderRoute);
				}
				else{
					logger.debug("filtered walk route - distance: " 
							+ recommenderRoute.getWalkingDistance() + " max walk distance: "
							+ maxWalkDistance +" route bike distance:"+recommenderRoute.getBikeDistance()+" max bike distance:"+maxBikeDistance);
				}
			}
			else{
				if( recommenderRoute.getBikeDistance() <= (int) maxBikeDistance && recommenderRoute.getWalkingDistance()<= (int) maxWalkDistance){
					filteredRoutes.add(recommenderRoute);
				}
				else{
					logger.debug("filtered based user preference - walk distance: "
							+ recommenderRoute.getWalkingDistance() + " max walk distance: "
							+ maxWalkDistance +" route bike distance:"+recommenderRoute.getWalkingDistance()+" max bike distance:"+maxBikeDistance);
				}
			}
		}
		//Filter routes based user preferrence
		if (!filteredRoutes.isEmpty() ){
			routes.clear();
			for (RouteModel entry : filteredRoutes){
				routes.add(entry);
			}
			logger.debug("filtering routes for user - after size: " + routes.size());
			return true;

		}
		else {
			return false;
		}
	}
	
	public void rankRoutesForUser (User user, Datastore mongoDatastore){
		rankBasedonUserPreferences(user, routes);
		rankBasedonSystemView(user, routes);
		
		rankBasedonBehaviouralModel(routes);
				
		//Implement Borda count
		ArrayList<Ballot> ballot = getBallots(routes);
		VotingSystem votingSystem = new Borda(ballot.toArray(new Ballot[ballot.size()]));
		logger.debug(votingSystem.results());
		String[] sortedRoutesId = votingSystem.getSortedCandidateList();
		List<RouteModel> FinalRankedRoutes = new ArrayList<>(Collections.nCopies(routes.size(), null));
		for (String entry : sortedRoutesId){
			logger.debug(entry);
		}
		for (RouteModel route : routes){
			String routeId = String.valueOf(route.getRouteId());
			int routeIndex = Arrays.asList(sortedRoutesId).indexOf(routeId);
			FinalRankedRoutes.set(routeIndex, route);
		}		
		logger.debug(FinalRankedRoutes);
		
		routes.clear();
		routes = FinalRankedRoutes;
	}

	public void rankRoutesForUserNew (User user, Datastore mongoDatastore){

		List<RouteModel> FinalRankedRoutes = new ArrayList<>(Collections.nCopies(routes.size(), null));
		List<RouteModel> RankedRoutes = new ArrayList<>(Collections.nCopies(routes.size()-1, null));

		for(int i = 0;i<routes.size();i++){
			RouteModel recommenderRoute = routes.get(i);
			if (i==0){
				FinalRankedRoutes.set(i, recommenderRoute);
			}
			else {
				RankedRoutes.set(i-1, recommenderRoute);
			}

		}

		if (RankedRoutes.size()>=2) {

			rankBasedonUserPreferences(user, RankedRoutes);

			rankBasedonSystemView(user, RankedRoutes);

			rankBasedonBehaviouralModel(RankedRoutes);

			//Implement Borda count
			ArrayList<Ballot> ballot = getBallots(RankedRoutes);
			VotingSystem votingSystem = new Borda(ballot.toArray(new Ballot[ballot.size()]));
			logger.debug(votingSystem.results());
			String[] sortedRoutesId = votingSystem.getSortedCandidateList();
			//List<RouteModel> FinalRankedRoutes = new ArrayList<>(Collections.nCopies(routes.size(), null));
			for (String entry : sortedRoutesId) {
				logger.debug(entry);
			}
			for (RouteModel route : RankedRoutes) {
				String routeId = String.valueOf(route.getRouteId());
				int routeIndex = Arrays.asList(sortedRoutesId).indexOf(routeId);
				FinalRankedRoutes.set(routeIndex+1, route);
			}
		}
		else if (RankedRoutes.size()==1) {

			FinalRankedRoutes.set(1,RankedRoutes.get(0));
		}
		logger.debug(FinalRankedRoutes);

		routes.clear();
		routes = FinalRankedRoutes;
	}


	public void addMessage(User user, Datastore mongoDatastore, String tripPurpose, Boolean graph) throws JSONException {

		/*if (check_if_we_need_to_add_message(user,mongoDatastore,N)) {
			selectTargetRouteandAddMessageForUser(user, mongoDatastore);
		}*/
		selectTargetRouteandAddMessageForUser(user, mongoDatastore, tripPurpose, graph);
	}
	
	private void rankBasedonBehaviouralModel(List<RouteModel> routes){

		List<RouteModel> rankedRoutes = new ArrayList<>();
		TreeMap<Double, ArrayList<RouteModel>> tmp = new TreeMap<>();

		for (RouteModel route : routes){
			if (tmp.containsKey(route.getBehaviouralModelUtility())){
				tmp.get(route.getBehaviouralModelUtility()).add(route);
			}
			else{
				ArrayList<RouteModel> newArrayList = new ArrayList<>();
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
	}
	
	private void rankBasedonUserPreferences(User user, List<RouteModel> routes){
		//if there are no preferences for this time of day get preferences for any time of day
		List<RouteModel> rankedRoutes = new ArrayList<>();

		TreeMap<Double, Integer> userPreferedModes = new TreeMap<>();

		try {
			Boolean car= Boolean.FALSE;
			Boolean walk= Boolean.FALSE;
			Boolean bike= Boolean.FALSE;
			Boolean pt= Boolean.FALSE;
			if (user.getMode_usage().getCar_percent()>0.0){
				userPreferedModes.put(user.getMode_usage().getCar_percent(), RecommenderModes.CAR);
				car=Boolean.TRUE;
			}
			if (user.getMode_usage().getBike_percent()>0.0){
				userPreferedModes.put(user.getMode_usage().getBike_percent(), RecommenderModes.BICYCLE);
				bike=Boolean.TRUE;
			}
			if (user.getMode_usage().getWalk_percent()>0.0){
				userPreferedModes.put(user.getMode_usage().getWalk_percent(), RecommenderModes.WALK);
				walk=Boolean.TRUE;
			}
			if (user.getMode_usage().getPt_percent()>0.0){
				userPreferedModes.put(user.getMode_usage().getPt_percent(), RecommenderModes.PUBLIC_TRANSPORT);
				pt=Boolean.TRUE;
			}
			if (routes.get(0).findCountry().equals("Vienna")){
				if(pt.equals(Boolean.FALSE)){
					userPreferedModes.put(0.004,RecommenderModes.PUBLIC_TRANSPORT);
				}
				if(walk.equals(Boolean.FALSE)){
					userPreferedModes.put(0.003,RecommenderModes.WALK);
				}
				if(car.equals(Boolean.FALSE)){
					userPreferedModes.put(0.002,RecommenderModes.CAR);
				}
				if(bike.equals(Boolean.FALSE)){
					userPreferedModes.put(0.001,RecommenderModes.BICYCLE);
				}
			}
			else if (user.getLanguage().equals("Ljubljana")){
				if(car.equals(Boolean.FALSE)){
					userPreferedModes.put(0.004,RecommenderModes.CAR);
				}
				if(walk.equals(Boolean.FALSE)){
					userPreferedModes.put(0.003,RecommenderModes.WALK);
				}
				if(pt.equals(Boolean.FALSE)){
					userPreferedModes.put(0.002,RecommenderModes.PUBLIC_TRANSPORT);
				}
				if(bike.equals(Boolean.FALSE)){
					userPreferedModes.put(0.001,RecommenderModes.BICYCLE);
				}
			}
			else if (user.getLanguage().equals("Birmingham")){
				if(car.equals(Boolean.FALSE)){
					userPreferedModes.put(0.004,RecommenderModes.CAR);
				}

				if(walk.equals(Boolean.FALSE)){
					userPreferedModes.put(0.003,RecommenderModes.WALK);
				}
				if(pt.equals(Boolean.FALSE)){
					userPreferedModes.put(0.002,RecommenderModes.PUBLIC_TRANSPORT);
				}
				if(bike.equals(Boolean.FALSE)){
					userPreferedModes.put(0.001,RecommenderModes.BICYCLE);
				}
			}
			else {
				if(pt.equals(Boolean.FALSE)){
					userPreferedModes.put(0.004,RecommenderModes.PUBLIC_TRANSPORT);
				}
				if(walk.equals(Boolean.FALSE)){
					userPreferedModes.put(0.003,RecommenderModes.WALK);
				}
				if(car.equals(Boolean.FALSE)){
					userPreferedModes.put(0.002,RecommenderModes.CAR);
				}
				if(bike.equals(Boolean.FALSE)){
					userPreferedModes.put(0.001,RecommenderModes.BICYCLE);
				}
			}
		}
		catch (Exception e){
			//if there are no preferences for any time of day get the default
			if (user.getLanguage().equals("de")){
				userPreferedModes.put(20.0,RecommenderModes.PUBLIC_TRANSPORT);
				userPreferedModes.put(19.0,RecommenderModes.WALK);
				userPreferedModes.put(18.0,RecommenderModes.CAR);
				userPreferedModes.put(17.0,RecommenderModes.BICYCLE);

			}
			if (user.getLanguage().equals("slo")){
				userPreferedModes.put(20.0,RecommenderModes.CAR);
				userPreferedModes.put(19.0,RecommenderModes.WALK);
				userPreferedModes.put(18.0,RecommenderModes.PUBLIC_TRANSPORT);
				userPreferedModes.put(17.0,RecommenderModes.BICYCLE);

			}
			if (user.getLanguage().equals("en")){
				userPreferedModes.put(20.0,RecommenderModes.CAR);
				userPreferedModes.put(19.0,RecommenderModes.WALK);
				userPreferedModes.put(18.0,RecommenderModes.PUBLIC_TRANSPORT);
				userPreferedModes.put(17.0,RecommenderModes.BICYCLE);

			}
			else {
				userPreferedModes.put(20.0,RecommenderModes.PUBLIC_TRANSPORT);
				userPreferedModes.put(19.0,RecommenderModes.WALK);
				userPreferedModes.put(18.0,RecommenderModes.CAR);
				userPreferedModes.put(17.0,RecommenderModes.BICYCLE);
			}
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
		}
		
		Map<Double, RouteModel> rankedRoutesMap = new LinkedHashMap<>();

		Double utility=0.0;
		for (RouteModel route : routes){
			Integer number = route.getMode();
			for (Map.Entry<Double, Integer> entry : userPreferedModes.entrySet()){
				//logger.debug("preference: " + entry.getKey() + " mode: " + entry.getValue());
				if (entry.getValue().equals(number)) {
					utility=entry.getKey();
				}
			}
			rankedRoutesMap.put(utility,route);
		}

		double userPreferenceRank = 0.0;
		for (Map.Entry<Double, RouteModel> entry : rankedRoutesMap.entrySet()){
			RouteModel route = entry.getValue();
			route.setUserPreferenceRank(userPreferenceRank);
			rankedRoutes.add(route);
			userPreferenceRank++;
		}
		
	}

	private void rankBasedonSystemView(User user, List<RouteModel> routes){

		List<RouteModel> rankedRoutes = new ArrayList<>();
		LinkedHashMap<RouteModel, Double> rankedRoutesMap = new LinkedHashMap<>();
		
		double maxEmissions=0.0;
		//Find max emissions
		for (RouteModel route : routes){
			if (route.getEmissions() > maxEmissions){
				maxEmissions=route.getEmissions();
			}
		}
		for (RouteModel route : routes){
			//Get distance of route
			Integer routeDistance = route.getRoute().getDistanceMeters();
			//Get the mode of route
			int mode = route.getMode();

			//Return 1 if context exists else return 0
			int BikeDistance = boolToInt(Context.withinBikeDistance(routeDistance));
			int WalkDistance = boolToInt(Context.withinWalkingDistance(routeDistance));
			int ManyPT = boolToInt(user.tooManyPublicTransportRoutes());
			int ManyCar = boolToInt(user.tooManyCarRoutes());
			int Emissions = boolToInt(user.emissionsIncreasing());
			Double rewardPoints = calculatePoints(route, user);
			int ReachingPriceTarget = boolToInt(Context.CheckReachingPrizeTarget(user, rewardPoints));
			//double NiceWeather = boolToDouble(WeatherInfo.isWeatherNice(lat, lon, city))
			double NiceWeather = 1.0;
			double Duration = 0.0;

			//Calculate utility of route based on the mode
			double context_utility;
			double emissions_utility;
			double utility;
			switch (mode) {
				case (int)RecommenderModes.WALK:
					if (ManyCar == 1){
						context_utility = 0.3512*WalkDistance + 0.2382*Duration + 0.0389*ManyCar + 0.0625*Emissions +0.1011*NiceWeather +0.2081*ReachingPriceTarget;
					}
					else if (ManyPT == 1){
						context_utility = 0.3324*WalkDistance + 0.2343*Duration + 0.0344*ManyPT + 0.0621*Emissions +0.1123*NiceWeather+ 0.2245*ReachingPriceTarget;
					}
					else {
						context_utility = 0.4*WalkDistance + 0.3*Duration + 0.1*Emissions +0.1*NiceWeather+ 0.1*ReachingPriceTarget;
					}
					emissions_utility = 0.0;
					utility = (1/3.0)*context_utility + (1-emissions_utility)*(2/3.0);
					//rankedRoutesMap.put(RecommenderModes.WALK, utility);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.BICYCLE:
				case (int)RecommenderModes.BIKE_SHARING:					
					if (ManyCar == 1){
						context_utility = 0.3512*BikeDistance + 0.2382*Duration + 0.0389*ManyCar + 0.0625*Emissions +0.1011*NiceWeather+0.2081*ReachingPriceTarget;
					}
					else if (ManyPT == 1){
						context_utility = 0.3324*BikeDistance + 0.2343*Duration + 0.0344*ManyPT + 0.0621*Emissions +0.1123*NiceWeather+0.2245*ReachingPriceTarget;
					}
					else {
						context_utility = 0.4*BikeDistance + 0.3*Duration + 0.1*Emissions +0.1*NiceWeather+ 0.1*ReachingPriceTarget;
					}
					emissions_utility = 0.0;
					utility = (1/3.0)*context_utility + (1-emissions_utility)*(2/3.0);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.BIKE_AND_RIDE:
					if (ManyCar == 1) {
						context_utility = 0.0557 * ManyCar + 0.4376 * Duration + 0.1282 * Emissions + 0.2043 * NiceWeather+0.1742*ReachingPriceTarget;
					}
					else if (ManyPT == 1) {
						context_utility = 0.0296 * ManyPT + 0.4385 * Duration + 0.1334 * Emissions + 0.2243 * NiceWeather+0.1742*ReachingPriceTarget;
					}
					else{
						context_utility = 0.4*BikeDistance + 0.3*Duration + 0.1*Emissions +0.1*NiceWeather + 0.1*ReachingPriceTarget;
					}
					emissions_utility = route.getEmissions()/maxEmissions;
					utility = (1/3.0)*context_utility + (1-emissions_utility)*(2/3.0);
					//rankedRoutesMap.put( RecommenderModes.BIKE_AND_RIDE, utility);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.PUBLIC_TRANSPORT:
					context_utility =  0.4323*Duration + 0.0845*ManyCar + 0.1935*Emissions +0.0361*NiceWeather+25.36*ReachingPriceTarget;
					emissions_utility = route.getEmissions()/maxEmissions;
					utility = (1/3.0)*context_utility + (1-emissions_utility)*(2/3.0);
					//rankedRoutesMap.put( RecommenderModes.PUBLIC_TRANSPORT, utility);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.PARK_AND_RIDE:
				case (int)RecommenderModes.PARK_AND_RIDE_WITH_BIKE:
					context_utility = 0.4376*Duration + 0.05571*ManyCar + 0.1282*Emissions +0.2043*NiceWeather+0.1742*ReachingPriceTarget;
					emissions_utility = route.getEmissions()/maxEmissions;
					utility = (1/3.0)*context_utility + (1-emissions_utility)*(2/3.0);
					//rankedRoutesMap.put( RecommenderModes.PARK_AND_RIDE, utility);
					rankedRoutesMap.put(route, utility);
					break;
				case (int)RecommenderModes.CAR:
					context_utility = 0.0001;
					emissions_utility = route.getEmissions()/maxEmissions;
					utility = (1/3.0)*context_utility + (1-emissions_utility)*(2/3.0);
					//rankedRoutesMap.put( RecommenderModes.CAR,utility);
					rankedRoutesMap.put(route, utility);
					break;
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
	
	private void selectTargetRouteandAddMessageForUser(User user, Datastore mongoDatastore, String tripPurpose, Boolean graph) throws JSONException {
		//Select target route and add message and strategy.
		List<String> targetList = user.getTargetList();
		logger.debug(targetList);
		String target = "";
		String mes="";
		String message = "";
		String strategy = "";
		String messageId = "";
		List<String> contextList = new ArrayList<>();
		List<String> FinaltargetList = new ArrayList<>();
		//Get duration and cost of the recommended route by routing engine
		Double duration =(double) routes.get(0).getRoute().getDurationSeconds()/60;
		Double segment_cost = 0.0;
		Double cost=0.0;
		for (int j = 0; j < routes.get(0).getRoute().getSegments().size(); j++) {
			RouteSegment segment = routes.get(0).getRoute().getSegments().get(j);
			try {
				segment_cost =  Double.parseDouble(segment.getAdditionalInfo().get("additionalProperties").toString().split("estimatedCost=")[1].split("}")[0]);
			}
			catch (Exception e){
				logger.debug(e);
				segment_cost = 0.0;
			}
			cost = segment_cost + cost;
		}
		for (int i = 0; i < targetList.size(); i++) {
			/*for (Iterator<String> i = someList.iterator(); i.hasNext();) {
				String item = i.next();
				System.out.println(item);
			}*/
			for (Iterator<RouteModel> routeItem = routes.iterator(); routeItem.hasNext();) {
				logger.debug("------");
				logger.debug(routes.size());
				RouteModel route = routeItem.next();
				logger.debug(route.getRoute().getAdditionalInfo().get("mode"));
				if (route.getRoute().getAdditionalInfo().get("mode") == targetList.get(i)) {
					target = targetList.get(i);
					try {
						Double rewardPoints = calculatePoints(route, user);
						contextList = Context.getRelevantContextForUser(this, route, user, mongoDatastore, rewardPoints);
					} catch (Exception e) {
						logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
					}
					if (Context.GetRelevantContext(target, contextList) == Boolean.TRUE) {
						//break;
						FinaltargetList.add(target);
					}
				}
			}
		}
		List<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
		int j=0;
		boolean SetMessage= false;
		if (FinaltargetList.size()>0) {
			while (message.isEmpty() && j < FinaltargetList.size() && !SetMessage) {
				target = FinaltargetList.get(j);
				rankedRoutes2 = new ArrayList<RouteModel>();
				for (Iterator<RouteModel> routeItem = routes.iterator(); routeItem.hasNext(); ) {
					//for (RouteModel route : routes) {
					RouteModel route = routeItem.next();
					if (route.getRoute().getAdditionalInfo().get("mode") == target && !SetMessage) {
						try {
							Double rewardPoints = calculatePoints(route, user);
							logger.debug("reward points" + rewardPoints);
							contextList = Context.getRelevantContextForUser(this, route, user, mongoDatastore, rewardPoints);
							mes = CalculateMessageUtilities.calculateForUser(contextList, user, target, mongoDatastore, rewardPoints);
							message = mes.split("_")[0];
							strategy = mes.split("_")[1];
							messageId = mes.split("_")[2];
							logger.debug(messageId + "----");
						} catch (Exception e) {
							logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
						}
					} else {
						message = "";
						strategy = "";
						messageId = "";
					}
					if (!message.isEmpty()) {
						Boolean displayGraph = Boolean.FALSE;
						Boolean displayMessage = check_if_we_need_to_add_message(user, mongoDatastore, route, tripPurpose, cost, duration);
						logger.debug("Display" + displayMessage);
						if (graph.equals(Boolean.TRUE)) {
							displayGraph = addGraph(user, mongoDatastore, strategy);
						}
						if (displayMessage && displayGraph) {
							route.setMessage(message);
							route.setStrategy(strategy);
							route.setMessageId(messageId);
							route.setContext(contextList);
							route.setFeature("MessageAndGraph");
							//set popup_display false
							route.setPopup(user.getFeedback(user.getId(), mongoDatastore));
							logger.debug("-------Feedback----" + user.getFeedback(user.getId(), mongoDatastore));
							SetMessage = true;
						} else if (displayMessage) {
							route.setMessage(message);
							route.setStrategy(strategy);
							route.setMessageId(messageId);
							route.setContext(contextList);
							route.setFeature("message");
							//set popup_display false
							route.setPopup(user.getFeedback(user.getId(), mongoDatastore));
							logger.debug("-------Feedback----" + user.getFeedback(user.getId(), mongoDatastore));
							SetMessage = true;
						}

					}
					rankedRoutes2.add(route);
				}
				j++;
			}
			routes.clear();
			routes = rankedRoutes2;
			if (SetMessage == false && graph.equals(Boolean.TRUE)) {
				Boolean displayGraph = addGraph(user, mongoDatastore, "");
				if (displayGraph.equals(Boolean.TRUE)) {
					Boolean SetGraph = Boolean.FALSE;
					int k =0;
					for (Iterator<RouteModel> routeItem = routes.iterator(); routeItem.hasNext(); ) {
						target = targetList.get(k);
						RouteModel route = routeItem.next();
						if (route.getRoute().getAdditionalInfo().get("mode") == target && !SetGraph) {
							route.setMessage("");
							route.setFeature("graph");
							SetGraph = Boolean.TRUE;
						}
						rankedRoutes2.add(route);
						k++;
					}
					routes.clear();
					routes = rankedRoutes2;
				}
			}
		}
		else{
			logger.debug("NO TARGET FOR MESSAGE");
			rankedRoutes2 = new ArrayList<RouteModel>();
			//Boolean addMessage=Boolean.TRUE;
			Integer integer = 0;
			for (RouteModel route : routes) {
				if (integer.equals(1)) {
					message = "";
					strategy = "";
					messageId = "";

					route.setMessage(message);
					route.setStrategy(strategy);
					route.setMessageId(messageId);
					//set popup_display false
					route.setPopup(false);
					//addMessage = Boolean.FALSE;
				}
				integer++;
				rankedRoutes2.add(route);
			}
		routes.clear();
		routes = rankedRoutes2;
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
				.setAdditionalInfo(originalRouteFormatRoutes.getAdditionalInfo())
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
	
	//This function converts boolean to Integer
		private int boolToInt( boolean b ) {
			if (b)
				return 1;
			return 0;
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
		    return b.getValue().compareTo(a.getValue());
		  }
		});
		LinkedHashMap<K, V> sortedEntries = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : entries) {
			sortedEntries.put(entry.getKey(), entry.getValue());
		}
		return sortedEntries;
	}

	public void addPersuasiveFeature(User user,  Datastore mongoDatastore) throws JSONException {

		String purpose = getPurpose();
		logger.debug(purpose);
		Integer N;
		//If trip purpose is leisure high intensity of intervantions
		/*if (purpose.equals("leisure")){
			N=5;
		}
		//If trip purpose is non-leisure low intensity of intervantions
		else {
			N=3;
		}*/

		Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal( user.getId());
		String group;
		if (!query.field("group").exists().asList().isEmpty()) {  // .asList().isEmpty()
			//group = query.get().getGroup();
			group=user.getGroup();
			logger.debug(group);
		}
		else {
			//Query<User> query1 = mongoDatastore.createQuery(User.class).field("id").equal( user.getId());
			if (mongoDatastore.createQuery(Request.class).field("numberOfUsersGroupA").exists().asList().isEmpty()) {
				Request request = new Request();
				mongoDatastore.save(request);
			}
			Integer groupA = mongoDatastore.createQuery(Request.class).get().getNumberOfUsersGroupA();
			Integer groupB = mongoDatastore.createQuery(Request.class).get().getNumberOfUsersGroupB();
			Integer groupC = mongoDatastore.createQuery(Request.class).get().getNumberOfUsersGroupC();

			Integer minNumber = groupA;
			group = "groupA";
			if (minNumber > groupB) {
				minNumber = groupB;
				group = "groupB";
			}
			if (minNumber > groupC) {
				minNumber = groupC;
				group = "groupC";
			}
			Query<User> userQuery = mongoDatastore.createQuery(User.class).field("id").equal(user.getId());
			mongoDatastore.update(userQuery, mongoDatastore.createUpdateOperations(User.class).set("group", group), false);
			if(group.equals("groupA")){
				groupA=groupA+1;
				Query<Request> requestQuery = mongoDatastore.createQuery(Request.class);
				mongoDatastore.update(requestQuery, mongoDatastore.createUpdateOperations(Request.class).set("numberOfUsersGroupA", groupA), false);
			}
			else if(group.equals("groupB")){
				groupB=groupB+1;
				Query<Request> requestQuery = mongoDatastore.createQuery(Request.class);
				mongoDatastore.update(requestQuery, mongoDatastore.createUpdateOperations(Request.class).set("numberOfUsersGroupB", groupB), false);
			}
			else if(group.equals("groupC")){
				groupC=groupC+1;
				mongoDatastore.createQuery(Request.class).get().setNumberOfUsersGroupC(groupC);
				Query<Request> requestQuery = mongoDatastore.createQuery(Request.class);
				mongoDatastore.update(requestQuery, mongoDatastore.createUpdateOperations(Request.class).set("numberOfUsersGroupC", groupC), false);
			}
		}

		logger.debug(user.getGroup());
		//groupA ---> Combination,  groupB ----> Graph,  groupC ----->Message


		if(group.equals("groupA")){
			//Combination of Graph and Message
			addMessage(user,mongoDatastore,purpose,Boolean.TRUE);

		}
		else if(group.equals("groupB")){
			ArrayList rankedRoutes2 = new ArrayList<RouteModel>();
			List targetList = user.getTargetList();

			Boolean displayGraph = addGraph(user, mongoDatastore, "");
			if (displayGraph.equals(Boolean.TRUE)) {
				Boolean SetGraph = Boolean.FALSE;
				Integer k=0;
				for (Iterator<RouteModel> routeItem = routes.iterator(); routeItem.hasNext(); ) {
					String target = targetList.get(k).toString();
					RouteModel route = routeItem.next();
					if (route.getRoute().getAdditionalInfo().get("mode") == target && !SetGraph) {
						route.setMessage("");
						route.setFeature("graph");
						SetGraph = Boolean.TRUE;
					}
					rankedRoutes2.add(route);
					k++;
				}
				routes.clear();
				routes = rankedRoutes2;

			}
		}
		else if(group.equals("groupC")){
			//Message only
			addMessage(user, mongoDatastore,purpose,Boolean.FALSE);

		}
	}

	public Boolean addGraph(User user, Datastore mongoDatastore, String strategy) throws JSONException {


		JSONObject graph = new JSONObject();

		graph.put("type", "horizontalBar");

		JSONArray datasets = new JSONArray();
		//Map<String, Object> datasets =new HashMap<String, Object>();


		JSONArray labels = new JSONArray();

		//Get personality of user
		String personality = null;

		if (strategy.equals("") || strategy.equals("reward") || strategy.equals("suggestion")){
			try {
				personality = user.getUserPersonalityType(user.getId(), mongoDatastore);
				//Get the most convincing persuasive strategy
				List<String> strategies = user.getBestPersuasiveStrategy(personality);
				strategy = strategies.get(0);
				if (strategy.equals("suggestion") || strategy.equals("reward")) {
					strategy = strategies.get(1);
					if (strategy.equals("suggestion") || strategy.equals("reward")) {
						strategy = strategies.get(2);
					}
				}

			} catch (UnknownHostException e) {
				strategy = "comparison";
			}

		}

		if (GetProperties.getTestGraphs().equals(Boolean.TRUE)) {

			DBCollection routes = mongoDatastore.getDB().getCollection("UserRouteLog");
			BasicDBObject TripQuery = new BasicDBObject();
			TripQuery.put("userId", user.getId());
			BasicDBObject fields = new BasicDBObject();
			fields.put("recommendedResults.additionalInfo.strategy", 1);

			List<DBObject> trip = routes.find(TripQuery, fields).sort(new BasicDBObject("$natural", -1)).limit(10).toArray();
			String previous_strategy = trip.get(0).get("recommendedResults").toString();
			if (previous_strategy.contains("comparison")) {
				strategy = "self-monitoring";
			}
			if (previous_strategy.contains("self-monitoring")) {
				strategy = "comparison";
			}
		}
		Boolean AddGraph = Boolean.FALSE;
		if (strategy.equals("comparison")) {
			Double transportUsage = user.getMode_usage().getPt_percent() + user.getMode_usage().getBike_percent() + user.getMode_usage().getWalk_percent();
			Double othersUsage = user.getPtUsageComparedToOthers() + user.getWalkUsageComparedToOthers() + user.getBikeUsageComparedToOthers();
			if (transportUsage > 0.0 && othersUsage > 0.0 && othersUsage>transportUsage) {
				AddGraph = Boolean.TRUE;
			}
		} else if (strategy.equals("self-monitoring")) {
			if ( (user.getMode_usage_last_week().getWalk_percent() > 0.0 || user.getMode_usage_last_week().getBike_percent() > 0.0 || user.getMode_usage_last_week().getCar_percent() > 0.0 || user.getMode_usage_last_week().getPt_percent() > 0.0 ) && ( user.getMode_usage_previous_week().getWalk_percent() > 0.0 || user.getMode_usage_previous_week().getBike_percent() > 0.0 || user.getMode_usage_previous_week().getCar_percent() > 0.0 || user.getMode_usage_previous_week().getPt_percent() > 0.0) ) {
				AddGraph = Boolean.TRUE;
			}
		}

		//if (AddGraph.equals(Boolean.TRUE)) {
		if (check_graph(strategy, mongoDatastore).equals(Boolean.TRUE) && AddGraph.equals(Boolean.TRUE)) {

			String graphTitle = "";


			if (strategy.equals("comparison")) {

				JSONArray you = new JSONArray();
				you.put("You");
				you.put("");
				labels.put(you);
				JSONArray others = new JSONArray();
				others.put("Optimum");
				others.put("Users");
				labels.put(others);

				JSONObject dataset = new JSONObject();
				dataset.put("label", "Use of green transportation modes");

				JSONArray data = new JSONArray();
				data.put((user.getMode_usage().getPt_percent() + user.getMode_usage().getBike_percent() + user.getMode_usage().getWalk_percent()) / 3.0);
				data.put((user.getPtUsageComparedToOthers() + user.getWalkUsageComparedToOthers() + user.getBikeUsageComparedToOthers()) / 3.0);
				dataset.put("data", data);

				JSONArray background = new JSONArray();
				background.put("rgba(255, 99, 132, 0.2)");
				background.put("rgba(54, 162, 235, 0.2)");

				dataset.put("backgroundColor", background);

				JSONArray border = new JSONArray();
				border.put("rgba(255,99,132,1)");
				border.put("rgba(54, 162, 235, 1)");

				dataset.put("borderColor", border);
				dataset.put("borderWidth", "1");

				datasets.put(dataset);

				graphTitle = "Your green transportation behaviour vs others";
			} else if (strategy.equals("self-monitoring") && AddGraph.equals(Boolean.TRUE)) {
				labels.put("This week");
				labels.put("Last week");

				JSONObject dataset1 = new JSONObject();
				dataset1.put("label", "Walk");
				dataset1.put("backgroundColor", "rgb(255, 159, 64)");
				JSONArray data = new JSONArray();
				data.put(user.getMode_usage_last_week().getWalk_percent());
				data.put(user.getMode_usage_previous_week().getWalk_percent());
				dataset1.put("data", data);

				//bicycle data
				datasets.put(dataset1);

				JSONObject dataset2 = new JSONObject();
				dataset2.put("label", "Bicycle");
				dataset2.put("backgroundColor", "rgb(75, 192, 192)");
				JSONArray data2 = new JSONArray();
				data2.put(user.getMode_usage_last_week().getBike_percent());
				data2.put(user.getMode_usage_previous_week().getBike_percent());
				dataset2.put("data", data2);

				datasets.put(dataset2);

				//pt data
				JSONObject dataset3 = new JSONObject();
				dataset3.put("label", "Public transport");
				dataset3.put("backgroundColor", "rgb(54, 162, 235)");
				JSONArray data3 = new JSONArray();
				data3.put(user.getMode_usage_last_week().getPt_percent());
				data3.put(user.getMode_usage_previous_week().getPt_percent());
				dataset3.put("data", data3);

				datasets.put(dataset3);

				//car data
				JSONObject dataset4 = new JSONObject();
				dataset4.put("label", "Car");
				dataset4.put("backgroundColor", "rgb(255, 99, 132)");

				JSONArray data4 = new JSONArray();
				data4.put(user.getMode_usage_last_week().getCar_percent());
				data4.put(user.getMode_usage_previous_week().getCar_percent());
				dataset4.put("data", data4);

				datasets.put(dataset4);

				graphTitle = "Your transportation behavior over the last two weeks";
			}

				JSONObject beginAtZeroYAxes = new JSONObject();
				beginAtZeroYAxes.put("beginAtZero", "true");

				JSONObject ticks = new JSONObject();
				ticks.put("ticks", beginAtZeroYAxes);

				JSONArray yAxes = new JSONArray();
				yAxes.put(ticks);

				JSONObject scales = new JSONObject();
				scales.put("yAxes", yAxes);


				JSONObject title = new JSONObject();
				title.put("display", "true");
				title.put("text", graphTitle);

				scales.put("yAxes", yAxes);

				JSONObject options = new JSONObject();
				options.put("maintainAspectRatio", "false");
				options.put("responsive", "false");
				options.put("scales", scales);
				options.put("title", title);

				JSONObject data = new JSONObject();
				data.put("labels", labels);
				data.put("datasets", datasets);

				JSONObject item = new JSONObject();
				item.put("type", "horizontalBar");
				item.put("data", data);
				item.put("options", options);

				Map<String, java.lang.Object> map = new TreeMap<String, java.lang.Object>();
				java.lang.Object m = item.toString();

				Map<String, Object> additionalInfo = originalRouteFormatRoutes.getAdditionalInfo();
				additionalInfo.put("graphData", m);
				additionalInfo.put("strategy", strategy);
				additionalInfo.put("userGroup",user.getGroup());
				originalRouteFormatRoutes.setAdditionalInfo(additionalInfo);

			//Update UserRequestPerGroup Collection
			UserRequestPerGroup requestPerGroup = new UserRequestPerGroup();
			requestPerGroup.setGroup(user.getGroup());
			requestPerGroup.setStrategy(strategy);
			requestPerGroup.setUserId(user.getId());
			requestPerGroup.setTimestamp(new Timestamp(System.currentTimeMillis()));
			//requestPerGroup.setRequestId();
			mongoDatastore.save(requestPerGroup);
			return Boolean.TRUE;
			/*ArrayList<RouteModel> rankedRoutes2 = new ArrayList<RouteModel>();
			Integer i = 0;
			for (Iterator<RouteModel> routeItem = routes.iterator(); routeItem.hasNext();) {
				//for (RouteModel route : routes) {
				//RouteModel route = routeItem.next();
				RouteModel route = routes.get(i);
				logger.debug(i);
				if (i==2) {
					route.setMessage(" ");
					route.setStrategy("");
					route.setMessageId("");

					//set popup_display false
					route.setPopup(user.getFeedback(user.getId(),mongoDatastore));
					logger.debug("-------Feedback----"+user.getFeedback(user.getId(),mongoDatastore));

				}
				i++;
				routeItem.next();
				rankedRoutes2.add(route);
			}
			routes.clear();
			routes = rankedRoutes2;*/


		}
		else {
			return Boolean.FALSE;
		}


	}

	public Boolean check_graph(String strategy,Datastore mongoDatastore) {

		//check if the graph displayed last X hours.
		Integer X=GetProperties.getHours();
		//get the current timestamp and the timestamp of the latestUpdate
		Timestamp now = new Timestamp(System.currentTimeMillis());
		//Compare if the timestamp is less than X hours.
		DBCollection routes = mongoDatastore.getDB().getCollection("UserRequestPerGroup");
		BasicDBObject TripQuery = new BasicDBObject();
		TripQuery.put("userId", user.getId());
		TripQuery.put("Strategy", strategy);
		BasicDBObject fields = new BasicDBObject();
		fields.put("Timestamp", 1);

		List<DBObject> request = routes.find(TripQuery, fields).sort(new BasicDBObject("$natural", -1)).limit(10).toArray();
		if (request.isEmpty()){
			return Boolean.TRUE;
		}
		else {
			logger.debug(request.get(0).get("Timestamp"));
			String dateString = request.get(0).get("Timestamp").toString();
			DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy",Locale.ENGLISH);
			try {
				Date date = format.parse(dateString);
				long milliseconds = abs(date.getTime() - now.getTime());
				int hours   = (int) ((milliseconds / (1000*60*60)) % 24);
				return hours > X;
			} catch (ParseException e) {
				e.printStackTrace();
				return Boolean.FALSE;
			}
		}
	}

	public Boolean check_if_we_need_to_add_message(User user,Datastore mongoDatastore, RouteModel route, String tripPurpose, Double cost, Double duration) {
		double[] vectorA = new double[2];
		double[] vectorB = new double[2];
		vectorA[0]=cost;
		vectorA[1]=duration;
		//Calculate target route cost
		vectorB[1]= (double) route.getRoute().getDurationSeconds()/60;
		Double target_segment_cost = 0.0;
		Double target_cost=0.0;
		for (int j = 0; j < route.getRoute().getSegments().size(); j++) {
			RouteSegment target_segment = route.getRoute().getSegments().get(j);
			try {
				target_segment_cost =  Double.parseDouble(target_segment.getAdditionalInfo().get("additionalProperties").toString().split("estimatedCost=")[1].split("}")[0]);
			}
			catch (Exception e){
				logger.debug(e);
				target_segment_cost = 0.0;
			}
			target_cost = target_segment_cost + target_cost;
		}

		vectorB[0]=target_cost;

		//Integer count = user.getCount();
		Integer total_commuting=user.getTotal_commuting();
		Integer total_leisure=user.getTotal_leisure();

		Integer displayed_commuting=user.getDisplayed_commuting();
		Integer displayed_leisure=user.getDisplayed_leisure();
		logger.debug(vectorA[0]);
		logger.debug(vectorB[0]);
		logger.debug(vectorA[1]);
		logger.debug(vectorB[1]);

		if (tripPurpose.equals("leisure")){
			if (cosineSimilarity(vectorA,vectorB)>0.7){
				logger.debug("Cosine similarity:"+cosineSimilarity(vectorA,vectorB));
				Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
				UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_leisure", total_leisure).set("displayed_leisure", displayed_leisure);
				mongoDatastore.update(query, ops);
				return Boolean.TRUE;
			}
			else{
				if (total_leisure==0){
					total_leisure = total_leisure+1;
					displayed_leisure=displayed_leisure+1;
					Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
					UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_leisure", total_leisure).set("displayed_leisure", displayed_leisure);
					mongoDatastore.update(query, ops);
					return Boolean.TRUE;
				}
				else {
					if (displayed_leisure / total_leisure < 0.5) {
						total_leisure = total_leisure + 1;
						displayed_leisure = displayed_leisure + 1;
						user.setTotal_leisure(total_leisure);
						user.setDisplayed_leisure(displayed_leisure);
						Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
						UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_leisure", total_leisure).set("displayed_leisure", displayed_leisure);
						mongoDatastore.update(query, ops);
						return Boolean.TRUE;
					} else {
						total_leisure = total_leisure + 1;
						Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
						UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_leisure", total_leisure);
						return Boolean.FALSE;
					}
				}
			}
		}
		else if (tripPurpose.equals("non-leisure")){
			if (cosineSimilarity(vectorA,vectorB)>0.7){
				logger.debug("Cosine similarity:"+cosineSimilarity(vectorA,vectorB));
				Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
				UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_commuting", total_commuting).set("displayed_commuting", displayed_commuting);
				mongoDatastore.update(query, ops);
				return Boolean.TRUE;
			}
			else {
				if(total_commuting==0){
					displayed_commuting = displayed_commuting + 1;
					total_commuting = total_commuting + 1;
					Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
					UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_commuting", total_commuting).set("displayed_commuting", displayed_commuting);
					mongoDatastore.update(query, ops);
					return Boolean.TRUE;
				}
				else {
					if ((double)displayed_commuting / total_commuting < 0.2) {
						displayed_commuting = displayed_commuting + 1;
						total_commuting = total_commuting + 1;
						user.setDisplayed_commuting(displayed_commuting);
						user.setDisplayed_commuting(total_commuting);
						Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
						UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_commuting", total_commuting).set("displayed_commuting", displayed_commuting);
						mongoDatastore.update(query, ops);
						return Boolean.TRUE;
					} else {
						total_commuting = total_commuting + 1;
						user.setDisplayed_commuting(total_commuting);
						Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal((String) user.getId());
						UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("total_commuting", total_commuting);
						mongoDatastore.update(query, ops);
						return Boolean.FALSE;
					}
				}
			}
		}
		else {
			return Boolean.TRUE;
			/*total_unknown_purpose = total_unknown_purpose+1;
			if (displayed_unknown_purpose/total_unknown_purpose>0.5){
				displayed_unknown_purpose=displayed_unknown_purpose+1;
				return Boolean.TRUE;
			}
			else {
				return Boolean.FALSE;
			}*/
		}

	}

	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < vectorA.length; i++) {
			dotProduct += vectorA[i] * vectorB[i];
			normA += Math.pow(vectorA[i], 2);
			normB += Math.pow(vectorB[i], 2);
		}
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	/*public Boolean check_if_we_need_to_add_message(User user,Datastore mongoDatastore, Integer N) {

		DBCollection routes = mongoDatastore.getDB().getCollection("UserRoute");
		BasicDBObject TripQuery = new BasicDBObject();
		TripQuery.put("userId", user.getId());
		TripQuery.put("route_feedback.helpful", Boolean.TRUE);
		Integer positiveFeedback = routes.find(TripQuery).size();

		BasicDBObject TripQuery2 = new BasicDBObject();
		TripQuery2.put("userId", user.getId());
		TripQuery2.put("route_feedback.helpful", Boolean.FALSE);
		Integer negativeFeedback = routes.find(TripQuery2).size();

		Integer no_answer= abs(negativeFeedback - positiveFeedback);
		Integer count = user.getCount();
		if (no_answer>N){
			Integer message_display= count%(no_answer-(N-1));
			if (message_display.equals(0)){
				count=1;
				user.setCount(count);
				return Boolean.TRUE;
			}
			else {
				count++;
				user.setCount(count);
				return Boolean.FALSE;
			}
		}
		else {
			return Boolean.TRUE;
		}

	}*/

	public String getPurpose() {
		String purpose = "non-leisure";
		String[] location = {
				originalRouteFormatRoutes.getRoutes().get(0).getTo().getCoordinate().getGeometry().getCoordinates().get().asNewList().get(0).toString(),
				originalRouteFormatRoutes.getRoutes().get(0).getTo().getCoordinate().getGeometry().getCoordinates().get().asNewList().get(1).toString()
		};
		//iF lat log is work or home purpose non-leisure else check Fourspuare
		if (Arrays.equals(location, user.getPredictedHome()) || Arrays.equals(location, user.getPredictedWork())){
			purpose = "non-leisure";
		}
		else{
			HttpURLConnection con;
			String client_id=GetProperties.getClient_id();
			String client_secret=GetProperties.getClient_secret();
			String url = "https://api.foursquare.com/v2/venues/search?client_id=" + client_id +
					"&client_secret=" + client_secret +
					"&v=20170801" +
					//"&ll=37.940321,23.697201" +
					//"&limit=10" +
					"&ll=" + location[0] + "," + location[1] + "&limit=10" +
					"&radius=30" +
					"&intent=browse";
			String params = "client_id=" + client_id +
					"&client_secret=" + client_secret +
					"&v=20170801" +
					//"&ll=37.940321,23.697201" +
					//"&limit=10" +
					"&ll=" + location[0] + "," + location[1] + "&limit=10" +
					"&radius=30&" +
					"&intent=browse";

			try {
				URL obj = new URL(url);
				con = (HttpURLConnection) obj.openConnection();
				con.setRequestMethod("GET");

				con.setRequestProperty("urlParameters", params);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
				return purpose;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
				return purpose;
			}

			//con.setRequestProperty("token",(String) accessToken);
			//con.setRequestProperty("user", (String) id.toString());
			try {
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

				JSONObject jsonObj = new JSONObject(response.toString());
				JSONObject res = jsonObj.getJSONObject("response");
				JSONArray arr = res.getJSONArray("venues");
				logger.debug(arr);

				List PurposeList=new ArrayList();
				JSONArray sortedJsonArray = new JSONArray();
				List<JSONObject> jsonList = new ArrayList<JSONObject>();

				for (int i = 0; i < arr.length(); i++) {

					JSONObject object = arr.getJSONObject(i);
					//String mode = getMode(object);
					JSONArray categories = object.getJSONArray("categories");
					String prefix = categories.getJSONObject(0).getJSONObject("icon").get("prefix").toString();

					String catId = categories.getJSONObject(0).get("id").toString();

					String distance = object.getJSONObject("location").getString("distance");
					logger.debug(distance);

					String categ = prefix.split("categories_v2/")[1];
					String category = categ.split("/")[0];
					logger.debug(category);
					List list1 = new ArrayList();
					list1.add("arts_entertainment");
					list1.add("food");
					list1.add("nightlife");
					list1.add("parks_outdoors");
					List education = new ArrayList();
					education.add("4bf58dd8d48988d1a1941735");
					education.add("4bf58dd8d48988d1b2941735");
					education.add("4bf58dd8d48988d1b4941735");
					education.add("4bf58dd8d48988d1ac941735");
					List events = new ArrayList();
					events.add("52f2ab2ebcbc57f1066b8b3b");
					events.add("5267e4d9e4b0ec79466e48c7");
					events.add("5267e4d9e4b0ec79466e48d1");
					events.add("5267e4d9e4b0ec79466e48c8");
					events.add("52741d85e4b0d5d1e3c6a6d9");
					List shops = new ArrayList();
					shops.add("52f2ab2ebcbc57f1066b8b56");
					shops.add("56aa371be4b08b9a8d5734d3");
					shops.add("4bf58dd8d48988d1f9941735");
					shops.add("52f2ab2ebcbc57f1066b8b1c");
					shops.add("58daa1558bbb0b01f18ec206");
					if (list1.contains(category)) {
						purpose = "leisure";
					} else if (category.equals("education")) {
						if (education.contains(catId)) {
							purpose = "leisure";
						} else {
							purpose = "non-leisure";
						}
					} else if (category.equals("events")) {
						if (events.contains(catId)) {
							purpose = "leisure";
						} else {
							purpose = "non-leisure";
						}
					} else if (category.equals("shops")) {
						if (shops.contains(catId)) {
							purpose = "non-leisure";
						} else {
							purpose = "leisure";
						}
					} else if (category.equals("building")) {
						purpose = "non-leisure";
					} else {
						purpose = "non-leisure";
					}
					PurposeList.add(purpose);

				}
				if(PurposeList.size()>0) {

					purpose = PurposeList.get(0).toString();
				}
				else{
					purpose = "unknown";
				}


			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return purpose;
	}

	public Double calculatePoints(RouteModel route,User user){
		Double rewardPoints=0.0;
		String pilot = user.getPilot();

		if (pilot==null){
			return rewardPoints;
		}
		Integer bikeTime = route.calculateBikeDuration();
		Integer PtTime = route.calculatePtDuration();
		Integer walkTime = route.calculateWalkingDuration();
		if(pilot.equals("VIE")){
			rewardPoints = 1.25*PtTime+2.5*bikeTime+2.5*walkTime;
		}
		if(pilot.equals("LJU")){
			rewardPoints = 3.0*PtTime+6.0*bikeTime+6.0*walkTime;
		}
		if(pilot.equals("BRI")){
			rewardPoints = 2.0*PtTime+4.0*bikeTime+4.0*walkTime;
		}
		logger.debug(rewardPoints);
		return rewardPoints;

	}


	}
