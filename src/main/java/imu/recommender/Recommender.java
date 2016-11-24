package imu.recommender;

import imu.recommender.helpers.UserPreferMode;
import java.lang.reflect.Array;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
	
	public Recommender(){
		//nothing to do for now 
	}
	
	public Recommender(RouteFormatRoot originalRouteFormatRoutes) throws JsonParseException, JsonMappingException, IOException{
		this.originalRouteFormatRoutes = originalRouteFormatRoutes;
		initialize();
	}
	
	private void initialize(){
		routes = new ArrayList<RouteModel>();		
		for (int i = 0; i < originalRouteFormatRoutes.getRoutes().size(); i++) {
			RouteModel recommenderRoute = new RouteModel(originalRouteFormatRoutes.getRoutes().get(i));
			recommenderRoute.calculateEmissions();
			recommenderRoute.setMode();
			routes.add(recommenderRoute);						    
		}
		filteredRoutes = new ArrayList<RouteModel>();
	}
	
	//filterRoutes function
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
	
	private double U1(){
		double ASC1 = 0.0;
		double TravelCostgeneric = -0.0779;
		double TravelTime1 = -0.0606;
		
		return 0.0;
	}
	
	private double U2(){
		double ASC2 = 0.821;
		double TravelCostgeneric = -0.0779;
		double TravelTime2 = -0.0568;
		
		return 0.0;
	}
	
	private double U3(){
		double ASC3 = -1.31;
		double TravelCostgeneric = -0.0779;
		double TravelTime3 = -0.0568; //???
		
		return 0.0;
	}
	
	private double U4(){
		double ASC3 = 1.92;
		double TravelCostgeneric = -0.0779;
		double TravelTime4 = -0.0714;
		
		return 0.0;
	}
	
	private double AspEnv(){
		double g0 = 4.08;
		double g1 = 0.0361;
		double g2 = -0.797;
		double g3 = 1.41;
		double g4 = -2.10;
		
		return 0.0;
	}

	public List<RouteModel> rankBasedonUserPreferences(List<RouteModel> routes, User user){
		//todo
		//get preference for this time of day (we should split the day in intervals)

		//if there are no preferences for this time of day get preferences for any time of day
		List<RouteModel> rankedRoutes = new ArrayList<RouteModel>();

		UserPreferMode[] modes = new UserPreferMode[4];

		try {

			UserPreferMode car = new UserPreferMode("car", (int) user.getMode_usage().getCar_percent());
			UserPreferMode bike = new UserPreferMode("bike", (int) user.getMode_usage().getBike_percent());
			UserPreferMode walk = new UserPreferMode("walk", (int) user.getMode_usage().getWalk_percent());
			UserPreferMode pt = new UserPreferMode("pt", (int) user.getMode_usage().getPt_percent());

			modes[0]=car;
			modes[1]=bike;
			modes[2]=walk;
			modes[3]=pt;
		}
		catch (Exception e){
			//if there are no preferences for any time of day get the default

			UserPreferMode car = new UserPreferMode("car", 4);
			UserPreferMode bike = new UserPreferMode("bike", 3);
			UserPreferMode walk = new UserPreferMode("walk", 2);
			UserPreferMode pt = new UserPreferMode("pt", 1);

			modes[0]=car;
			modes[1]=bike;
			modes[2]=walk;
			modes[3]=pt;

		}


		Arrays.sort(modes);

		int i=0;
		for(UserPreferMode temp: modes){
			System.out.println("mode " + ++i + " : " + temp.getMode() +
					", Percentage : " + temp.getPercentage());
		}

		List<RouteModel> FirstListRoutes = new ArrayList<RouteModel>();
		List<RouteModel> SecondListRoutes = new ArrayList<RouteModel>();
		List<RouteModel> ThirdListRoutes = new ArrayList<RouteModel>();
		List<RouteModel> ForthListRoutes = new ArrayList<RouteModel>();

		for (RouteModel route : routes){
			String m = route.getRoute().getAdditionalInfo().get("mode").toString();
			if (m.equals(modes[0].getMode() ) ){
				FirstListRoutes.add(route);
			}
			if (m.equals(modes[1].getMode() ) ){
				SecondListRoutes.add(route);
			}
			if (m.equals(modes[2].getMode() ) ){
				ThirdListRoutes.add(route);
			}
			if (m.equals(modes[3].getMode() ) ){
				ForthListRoutes.add(route);
			}

		}
		rankedRoutes.addAll(FirstListRoutes);
		rankedRoutes.addAll(SecondListRoutes);
		rankedRoutes.addAll(ThirdListRoutes);
		rankedRoutes.addAll(ForthListRoutes);
		// if there are preferences use these preferences
		return rankedRoutes;
	}
	
	private List<RouteModel> rankBasedonSystemView(){
		//todo later
		return null;
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
