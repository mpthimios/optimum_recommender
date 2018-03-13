package imu.recommender.models.route;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.helpers.RecommenderModes;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteModel {

	private Logger logger = Logger.getLogger(RouteModel.class);
	private Route route = new Route();
	private int routeId;
	private double emissions = 0.0;
	private double behaviouralModelUtility = 0.0;
	private int mode;
	private double userPreferencesRank = 0.0;
	private double SystemRank = 0.0;
	private double UserPreferenceRank = 0.0;
	private double BehaviouralModelRank = 0.0;
	private double CO2EmissionsRank = 0.0;
	private String message = "";
	private String strategy = "";
	private String messageId = "";
	private List<String> context = new ArrayList<>();
	private boolean popup = false;
	private int walkingDistance = 0;
	private int bikeDistance = 0;

	
	public RouteModel (Route route){
		this.route = route;
		calculateWalkingDistance();
		calculateBikeDistance();
	}
	
	public void setModeandCalculateEmissions(){
		emissions = 0.0;
		List<String> Modes = new ArrayList<String>();
		for (int j=0; j< this.getRoute().getSegments().size(); j++) {
			RouteSegment segment = this.getRoute().getSegments().get(j);
			String segment_mode = segment.getModeOfTransport().getGeneralizedType().toString();
			String detailed_mode = segment.getModeOfTransport().getDetailedType().toString();
			Integer distance = segment.getDistanceMeters();
			if (segment_mode.equals("CAR")){
				logger.debug(detailed_mode+"-------");
				if(detailed_mode.equals("MOTORHOME")){
					segment_mode = "MOTORHOME";
				}
				Boolean sharing = segment.getModeOfTransport().getSharingType().isPresent();
				if (sharing){
					segment_mode = "CAR_SHARING";
				}
			}
			if (segment_mode.equals("BICYCLE")){
				Boolean sharing = segment.getModeOfTransport().getSharingType().isPresent();
				if (sharing){
					segment_mode = "BIKE_SHARING";
				}
			}
			emissions = emissions + CalculateSegmentEmissions(distance, segment_mode, detailed_mode);
			if (!Modes.contains(segment_mode)) {
				Modes.add(segment_mode);
			}
		}
		Map<String, Object> additionalInfo = route.getAdditionalInfo();
		additionalInfo.put("emissions", emissions);
		this.route.setAdditionalInfo(additionalInfo);
		String mode="";

		if(Modes.contains("MOTORHOME")){
			this.setMode(RecommenderModes.MOTORHOME);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") && Modes.contains("BICYCLE")  ){
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
		else if (Modes.contains("BICYCLE") && Modes.contains("FOOT") && Modes.size()==2 ){
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
		else if (Modes.contains("CAR_SHARING")){
			this.setMode(RecommenderModes.CAR_SHARING);
			mode = RecommenderModes.recommenderModesStr[this.getMode()];
		}
		else if (Modes.contains("BIKE_SHARING") ){
			this.setMode(RecommenderModes.BIKE_SHARING);
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
	
	public void calculateWalkingDistance(){
		int total_walk_distance = 0;
		for (RouteSegment segment :  this.route.getSegments()){
			if ("FOOT".equals(segment.getModeOfTransport().getGeneralizedType().toString()) ){
				total_walk_distance = total_walk_distance + segment.getDistanceMeters();
			}
		}
		this.walkingDistance = total_walk_distance;
	}
	
	public void calculateBikeDistance(){
		int total_bile_distance = 0;
		for (RouteSegment segment :  this.route.getSegments()){
			if ("BICYCLE".equals(segment.getModeOfTransport().getGeneralizedType().toString()) ){
				total_bile_distance = total_bile_distance + segment.getDistanceMeters();
			}
		}
		this.bikeDistance = total_bile_distance;
	}
	
	private double CalculateSegmentEmissions(Integer distance, String travel_mode, String detailed_mode) {
		double emissions=0.0;
		
		//logger.debug("distance: " + distance + " travel_mode: " + travel_mode + " detailed_mode: " + detailed_mode);
		if ("FOOT".equals(travel_mode) ){
			emissions = 0;
		}
		if ("BICYCLE".equals(travel_mode) ){
			emissions = 0;
		}
		if ("PUBLIC_TRANSPORT".equals(travel_mode) ) {

			if ("Optional[SUBWAY]".equals(detailed_mode)) {
				emissions = ( (double)(distance*20)/1000 );
			}
			if ("Optional[HEAVY_RAIL]".equals(detailed_mode)) {
				emissions = ( (double)(distance*50)/1000 );
			}
			if ("Optional[BUS]".equals(detailed_mode)) {
				emissions = ( (distance*25.5)/1000 );
			}
		}
		if ("CAR".equals(travel_mode) ){
			emissions = ( (double)(distance*110)/1000 );
		}
		if ("MOTORHOME".equals(travel_mode) ){
			emissions = ( (double)(distance*255)/1000 );
		}

		return emissions;

	}

	public Integer calculateWalkingDuration(){
		int total_walk_time = 0;
		for (RouteSegment segment :  this.route.getSegments()){
			if ("FOOT".equals(segment.getModeOfTransport().getGeneralizedType().toString()) ){
				total_walk_time = total_walk_time + segment.getDurationSeconds();
			}
		}
		return total_walk_time/60;
	}

	public Integer calculateBikeDuration(){
		int total_bike_time = 0;
		for (RouteSegment segment :  this.route.getSegments()){
			if ("BICYCLE".equals(segment.getModeOfTransport().getGeneralizedType().toString()) ){
				total_bike_time = total_bike_time + segment.getDurationSeconds();
			}
		}
		return total_bike_time/60;
	}

	public Integer calculatePtDuration(){
		int total_pt_time = 0;
		for (RouteSegment segment :  this.route.getSegments()){
			if ("PUBLIC_TRANSPORT".equals(segment.getModeOfTransport().getGeneralizedType().toString()) ){
				total_pt_time = total_pt_time + segment.getDurationSeconds();
			}
		}
		return total_pt_time/60;
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

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
		Map<String, Object> additionalInfo = this.route.getAdditionalInfo();
		additionalInfo.put("strategy", strategy);
		this.route.setAdditionalInfo(additionalInfo);
	}

	public double getUserPreferenceRank() {
		return UserPreferenceRank;
	}

	public void setUserPreferenceRank(double userPreferenceRank) {
		UserPreferenceRank = userPreferenceRank;
	}

	public double getBehaviouralModelRank() {
		return BehaviouralModelRank;
	}

	public void setBehaviouralModelRank(double behaviouralModelRank) {
		BehaviouralModelRank = behaviouralModelRank;
	}

	public double getCO2EmissionsRank() {
		return CO2EmissionsRank;
	}

	public void setCO2EmissionsRank(double cO2EmissionsRank) {
		CO2EmissionsRank = cO2EmissionsRank;
	}

	public void setPopup(Boolean popup){
		Map<String, Object> additionalInfoRouteRequest = route.getAdditionalInfo();
		additionalInfoRouteRequest.put("display_popup", popup);
		route.setAdditionalInfo(additionalInfoRouteRequest);
	}
	
	public boolean getPopup() { return this.popup;}

	public int getWalkingDistance() {
		return walkingDistance;
	}

	public void setWalkingDistance(int walkingDistance) {
		this.walkingDistance = walkingDistance;
	}

	public int getBikeDistance() {
		return bikeDistance;
	}

	public void setBikeDistance(int bikeDistance) {
		this.bikeDistance = bikeDistance;
	}

	public double getEmissions() {
		return emissions;
	}

	public void setEmissions(double emissions) {
		this.emissions = emissions;
	}


	public Object getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
		Map<String, Object> additionalInfo = this.route.getAdditionalInfo();
		additionalInfo.put("messageId", messageId);
		this.route.setAdditionalInfo(additionalInfo);
	}

	public List<String> getContext() {
		return context;
	}

	public void setContext(List<String> context) {
		this.context = context;
	}
}
