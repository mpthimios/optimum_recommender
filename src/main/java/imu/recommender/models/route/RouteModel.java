package imu.recommender.models.route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.RequestHandler;
import imu.recommender.helpers.RecommenderModes;

public class RouteModel {

	private Logger logger = Logger.getLogger(RouteModel.class);
	private Route route = new Route();
	private int routeId;
	private double emissions = 0.0;
	private double behaviouralModelUtility = 0.0;
	private int mode;
	private double userPreferencesRank = 0.0;
	private double SystemRank = 0.0;
	private String message = "";

	
	public RouteModel (Route route){
		this.route = route;
	}
	
	public void setMode(){
		
		List<String> Modes = new ArrayList<String>();
		for (int j=0; j< this.getRoute().getSegments().size(); j++) {
			RouteSegment segment = this.getRoute().getSegments().get(j);
			String mode = segment.getModeOfTransport().getGeneralizedType().toString();
					//.getGeneralizedType().toString();
			if (!Modes.contains(mode)) {
				Modes.add(mode);
			}
		}
		String mode="";

		logger.debug("adding mode");
		if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") && Modes.contains("BICYCLE")  ){
			this.setMode(RecommenderModes.PARK_AND_RIDE_WITH_BIKE);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];			
		}
		else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") ){
			this.setMode(RecommenderModes.PARK_AND_RIDE);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];	
		}
		else if (Modes.contains("BICYCLE") && Modes.contains("FOOT") && Modes.contains("PUBLIC_TRANSPORT") ){
			this.setMode(RecommenderModes.BIKE_AND_RIDE);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") ){
			this.setMode(RecommenderModes.PUBLIC_TRANSPORT);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("BICYCLE") && Modes.contains("FOOT") ){
			this.setMode(RecommenderModes.BICYCLE);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("CAR") && Modes.contains("FOOT") ){
			this.setMode(RecommenderModes.CAR);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("FOOT") && Modes.size()==1 ){
			this.setMode(RecommenderModes.WALK);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("BICYCLE") && Modes.size()==1 ){
			this.setMode(RecommenderModes.BICYCLE);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("CAR") && Modes.size()==1 ){
			this.setMode(RecommenderModes.CAR);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.size()==1 ){
			this.setMode(RecommenderModes.PUBLIC_TRANSPORT);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else {
			this.setMode(RecommenderModes.UNKNOWN);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		
		Map<String, Object> additionalInfoRouteRequest = route.getAdditionalInfo();
		additionalInfoRouteRequest.put("mode", mode);
		route.setAdditionalInfo(additionalInfoRouteRequest);
		

	}
	
	public void calculateEmissions(){
		for (int j = 0; j < route.getSegments().size(); j++) {
			RouteSegment segment = route.getSegments().get(j);
			String mode = segment.getModeOfTransport().getGeneralizedType().toString();
			String detailed_mode = segment.getModeOfTransport().getDetailedType().toString();
			Integer distance = segment.getDistanceMeters();
			emissions = emissions + CalculateSegmentEmissions(distance, mode, detailed_mode);
		}
		Map<String, Object> additionalInfo = route.getAdditionalInfo();
		additionalInfo.put("emissions", emissions);
		this.route.setAdditionalInfo(additionalInfo);
		
	}
	
	private double CalculateSegmentEmissions(Integer distance, String travel_mode, String detailed_mode) {
		double emissions=0.0;
		
		logger.debug("calculating emissions for: ");
		logger.debug("distance: " + distance + " travel_mode: " + travel_mode + " detailed_mode: " + detailed_mode);
		if (travel_mode.equals("FOOT") ){
			emissions = 0;
		}
		if (travel_mode.equals("BICYCLE") ){
			emissions = 0;
		}
		if (travel_mode.equals("PUBLIC_TRANSPORT") ) {

			if (detailed_mode.equals("Optional[SUBWAY]")) {
				emissions = ( (double)(distance*20)/1000 );
			}
			if (detailed_mode.equals("Optional[HEAVY_RAIL]")) {
				emissions = ( (double)(distance*50)/1000 );
			}
			if (detailed_mode.equals("Optional[BUS]")) {
				emissions = ( (distance*25.5)/1000 );
			}
		}
		if (travel_mode.equals("CAR") ){
			emissions = ( (double)(distance*110)/1000 );
		}

		return emissions;

	}

	public double getEmissions() {
		return emissions;
	}

	public void setEmissions(double emissions) {
		this.emissions = emissions;
	}

	public Route getRoute() {
		return route;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public double getBehaviouralModelUtility() {
		return behaviouralModelUtility;
	}

	public void setBehaviouralModelUtility(double behaviouralModelUtility) {
		this.behaviouralModelUtility = behaviouralModelUtility;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public double getUserPreferencesRank() {
		return userPreferencesRank;
	}

	public void setUserPreferencesRank(double userPreferencesRank) {
		this.userPreferencesRank = userPreferencesRank;
		Map<String, Object> additionalInfo = this.route.getAdditionalInfo();		 
		additionalInfo.put("UserPreferencesRank", userPreferencesRank);
		this.route.setAdditionalInfo(additionalInfo);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
		Map<String, Object> additionalInfo = this.route.getAdditionalInfo();		 
		additionalInfo.put("message", message);
		this.route.setAdditionalInfo(additionalInfo);
	}

	public double getSystemRank() {
		return SystemRank;
	}

	public void setSystemRank(double SystemRank) {
		this.SystemRank = SystemRank;
		Map<String, Object> additionalInfo = this.route.getAdditionalInfo();
		additionalInfo.put("SystemRank", SystemRank);
		this.route.setAdditionalInfo(additionalInfo);
	}

	public int getRouteId() {
		return routeId;
	}

	public void setRouteId(int routeId) {

		this.routeId = routeId;
		Map<String, Object> additionalInfo = this.route.getAdditionalInfo();
		additionalInfo.put("routeId", routeId);
		this.route.setAdditionalInfo(additionalInfo);
	}
	
	
}
