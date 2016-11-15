package imu.recommender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
				if((bike_owner) && (recommenderRoute.getRoute().getDistanceMeters()<3000)){
					filteredRoutes.add(recommenderRoute);
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode == RecommenderModes.WALK){
				if(recommenderRoute.getRoute().getDistanceMeters()<1000){
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
	
	private List<RouteModel> rankBasedonUserPreferences(){
		//todo 
		//get preference for this time of day (we should split the day in intervals)
		//if there are no preferences for this time of day get preferences for any time of day
		//if there are preferences use these preferences
		return null;
	}
	
	private List<RouteModel> rankBasedonSystemView(){
		//todo later
		return null;
	}
	
	public String getFilteredRoutesResponseStr(){
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
		return filtered_route.toString();
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
