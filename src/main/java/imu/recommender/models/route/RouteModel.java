package imu.recommender.models.route;

import java.io.IOException;
import java.util.Map;

import at.ac.ait.ariadne.routeformat.Route;
import at.ac.ait.ariadne.routeformat.RouteSegment;

public class RouteModel {

	private Route route;
	private double emissions = 0.0;
	
	public RouteModel (Route route){
		this.route = route;
	}
	
	public void addMode(){
		//todo
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
		route.setAdditionalInfo(additionalInfo);
		
	}
	
	private double CalculateSegmentEmissions(Integer distance, String travel_mode, String detailed_mode) {
		double emissions=0.0;
		if (travel_mode.equals("FOOT") ){
			emissions = 0;
		}
		if (travel_mode.equals("BICYCLE") ){
			emissions = 0;
		}
		if (travel_mode.equals("PUBLIC_TRANSPORT") ) {

			if (detailed_mode.equals("SUBWAY")) {
				emissions = ( (double)(distance*20)/1000 );
			}
			if (detailed_mode.equals("HEAVY_RAIL")) {
				emissions = ( (double)(distance*50)/1000 );
			}
			if (detailed_mode.equals("BUS")) {
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
	
}