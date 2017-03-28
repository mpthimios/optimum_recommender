package imu.recommender;

import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.helpers.RecommenderModes;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import uk.recurse.geocoding.reverse.Country;
import uk.recurse.geocoding.reverse.ReverseGeocoder;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

public class BehaviouralModel {
	
	private static Logger logger = Logger.getLogger(BehaviouralModel.class);
	
	public static double calculateBhaviouralModelUtility (RouteModel route, User user, double locationValue){
		double final_utility = 0.0;
		double utility = 0.0;
				double cost = 0.0;
		double segment_cost = 0.0;
		double time = (double) route.getRoute().getDurationSeconds()/60;
		logger.debug("Mode : "+route.getRoute().getAdditionalInfo().get("mode"));
		logger.debug("Time: "+time);
		for (int j = 0; j < route.getRoute().getSegments().size(); j++) {
			RouteSegment segment = route.getRoute().getSegments().get(j);
			try {
				segment_cost = (double)segment.getAdditionalInfo().get("estimatedCost");

			}
			catch (Exception e){
				segment_cost = 0.0;
			}
			cost = segment_cost + cost;
		}
		logger.debug("Estimated Cost : "+cost);
		switch (route.getMode()){
			case RecommenderModes.WALK:
				utility = U2(cost, time, user, locationValue);
				break;
			case RecommenderModes.BICYCLE:
				utility = U1(cost, time, user, locationValue);
				break;
			case RecommenderModes.BIKE_AND_RIDE:
				utility = U1(cost, time, user, locationValue);
				break;
			case RecommenderModes.PUBLIC_TRANSPORT:
				utility = U3(cost, time, user, locationValue);
				break;
			case RecommenderModes.PARK_AND_RIDE_WITH_BIKE:
				utility = U4(cost, time, user, locationValue);
				break;
			case RecommenderModes.PARK_AND_RIDE:
				utility = U4(cost, time, user, locationValue);
				break;
			case RecommenderModes.CAR:
				utility = U4(cost, time, user, locationValue);
				break;
			default:
				break;
		}

		logger.debug("behavioural model utility: " + utility);
		return utility;
	}
	
	public static double U1(double cost, double time, User user, double locationValue){
		double ASC1 = 0.0;
		double TravelCostgeneric = -0.0779;
		double TravelTime1 = -0.0606;
		double gAspEnv1 = 3.18;
		
		double U1 = ASC1 + TravelCostgeneric*cost + TravelTime1*time +
				gAspEnv1*AspEnv(user, locationValue)*time;
		
		return U1;
	}
	
	public static double U2(double cost, double time, User user, double locationValue){
		double ASC2 = 0.821;
		double TravelCostgeneric = -0.0779;
		double TravelTime2 = -0.0568;
		double gAspEnv2 = 5.34;
		
		double U2 = ASC2 + TravelCostgeneric*cost + TravelTime2*time +
				gAspEnv2*AspEnv(user, locationValue)*time;
		
		return U2;
	}
	
	public static double U3(double cost, double time, User user, double locationValue){
		double ASC3 = -1.31;
		double TravelCostgeneric = -0.0779;
		double TravelTime3 = -0.0568; //???
		double gAspEnv3 = 1.29;
		
		double U3 = ASC3 + TravelCostgeneric*cost + TravelTime3*time +
				gAspEnv3*AspEnv(user, locationValue)*time;
		
		return U3;
	}
	
	public static double U4(double cost, double time, User user, double locationValue){
		double ASC4 = 1.92;
		double TravelCostgeneric = -0.0779;
		double TravelTime4 = -0.0714;
		double gAspEnv4 = 2.13;
		
		double U4 = ASC4 + TravelCostgeneric*cost + TravelTime4*time +
				gAspEnv4*AspEnv(user, locationValue)*time;
		
		return U4;
	}
	
	private static double AspEnv(User user, double locationValue){
		double g0 = 4.08;
		double g1 = 0.0361;
		double g2 = -0.797;
		double g3 = 1.41;
		double g4 = -2.10;
		
		double result = g0 + 
				g1*(double)user.getDemographics().getAge() +
				g2*educationValue(user.getDemographics().getEducation()) +
				g3*genderValue(user.getDemographics().getGender()) +
				g4*locationValue;				
		return 0.0;
	}
	
	private static double educationValue(String education){
		if (education.matches("Graduate or post-graduate degree")){
			return 1.0;
		}
		else return 0.0;
	}
	
	private static double genderValue(String gender){
		if (gender.matches("male")){
			return 0.0;
		}
		else return 1.0;
	}
	
	public static double getLocationValue(String lng, String lat){
		try{
			ReverseGeocoder geocoder = new ReverseGeocoder();
			Optional country = geocoder.getCountry(Double.parseDouble(lat), Double.parseDouble(lng));
			if (country.isPresent()){
				String location = ((Country)country.get()).iso();
				logger.debug("Request from: " + location);
				if (location.matches("GB")){
					logger.debug("request from UK");
					return 1.0;
				}
				else return 0.0;
			}
			else return 0.0;
		}
		catch (Exception e){
			e.printStackTrace();
			return 0.0;
		}		
	}
}
