package imu.recommender;

import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class BehaviouralModel {
	
	private static Logger logger = Logger.getLogger(BehaviouralModel.class);
	
	public static double calculateBhaviouralModelUtility (RouteModel route, User user){
		double final_utility = 0.0;
		double utility = 0.0;
		String[] location = {
				route.getRoute().getFrom().getCoordinate().geometry.coordinates.get(0).toString(),
				route.getRoute().getFrom().getCoordinate().geometry.coordinates.get(1).toString()	
		};
		for (int j = 0; j < route.getRoute().getSegments().size(); j++) {
			RouteSegment segment = route.getRoute().getSegments().get(j);
			String mode = segment.getModeOfTransport().getGeneralizedType().toString();
			double time = (double) segment.getDurationSeconds();
			try {
				double cost = (double)segment.getAdditionalInfo().get("estimatedCost");
				logger.debug("Estimated Cost : "+cost);
				switch (mode){
					case "WALK":
						utility = U2(cost, time, user, location);
						break;
					case "BICYCLE":
						utility = U1(cost, time, user, location);
						break;
					case "BIKE_AND_RIDE":
						utility = U1(cost, time, user, location);
						break;
					case "PUBLIC_TRANSPORT":
						utility = U3(cost, time, user, location);
						break;
					case "PARK_AND_RIDE_WITH_BIKE":
						utility = U4(cost, time, user, location);
						break;
					case "PARK_AND_RIDE":
						utility = U4(cost, time, user, location);
						break;
					case "CAR":
						utility = U4(cost, time, user, location);
						break;
					default:
						break;
				}
			}
			catch (Exception e){
				utility = 0.0;
			}

			final_utility = final_utility + utility;
		}

		logger.debug("behavioural model utility: " + final_utility);
		return final_utility;
	}
	
	public static double U1(double cost, double time, User user, String[] location){
		double ASC1 = 0.0;
		double TravelCostgeneric = -0.0779;
		double TravelTime1 = -0.0606;
		double gAspEnv1 = 3.18;
		
		double U1 = ASC1 + TravelCostgeneric*cost + TravelTime1*time +
				gAspEnv1*AspEnv(user, location)*time;
		
		return U1;
	}
	
	public static double U2(double cost, double time, User user, String[] location){
		double ASC2 = 0.821;
		double TravelCostgeneric = -0.0779;
		double TravelTime2 = -0.0568;
		double gAspEnv2 = 5.34;
		
		double U2 = ASC2 + TravelCostgeneric*cost + TravelTime2*time +
				gAspEnv2*AspEnv(user, location)*time;
		
		return U2;
	}
	
	public static double U3(double cost, double time, User user, String[] location){
		double ASC3 = -1.31;
		double TravelCostgeneric = -0.0779;
		double TravelTime3 = -0.0568; //???
		double gAspEnv3 = 1.29;
		
		double U3 = ASC3 + TravelCostgeneric*cost + TravelTime3*time +
				gAspEnv3*AspEnv(user, location)*time;
		
		return U3;
	}
	
	public static double U4(double cost, double time, User user, String[] location){
		double ASC4 = 1.92;
		double TravelCostgeneric = -0.0779;
		double TravelTime4 = -0.0714;
		double gAspEnv4 = 2.13;
		
		double U4 = ASC4 + TravelCostgeneric*cost + TravelTime4*time +
				gAspEnv4*AspEnv(user, location)*time;
		
		return U4;
	}
	
	private static double AspEnv(User user, String[] location){
		double g0 = 4.08;
		double g1 = 0.0361;
		double g2 = -0.797;
		double g3 = 1.41;
		double g4 = -2.10;
		
		double result = g0 + 
				g1*(double)user.getDemographics().getAge() +
				g2*1 +educationValue(user.getDemographics().getEducation()) +
				g3*genderValue(user.getDemographics().getGender()) +
				g4*getLocationValue(location[0], location[1]);				
		return 0.0;
	}
	
	private static double educationValue(String education){
		if (education.matches("Primary education or less")){
			return 1.0;
		}
		else if (education.matches("Secondary education")){
			return 2.0;
		}
		else if (education.matches("Undergraduate degree")){
			return 3.0;
		}
		else if (education.matches("Graduate or post-graduate degree")){
			return 4.0;
		}
		else return 0.0;
	}
	
	private static double genderValue(String gender){
		if (gender.matches("male")){
			return 0.0;
		}
		else return 1.0;
	}
	
	private static  double getLocationValue(String lng, String lat){
		
		String urlString = "http://api.geonames.org/countryCode?lat="+lat+"&lng="+lng+"&username=demo";
		try{
			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();
			InputStream is = conn.getInputStream();
			String location = IOUtils.toString(is, "UTF-8").trim().replaceAll("\n ", "");			
			if (location.matches("SI")){
				logger.debug("request from Slovenia");
				return 1.0;
			}
			else if (location.matches("AT")){
				logger.debug("request from Austria");
				return 2.0;
			}
			else if (location.matches("GB")){
				logger.debug("request from UK");
				return 3.0;
			}
			else return 0.0;
		}
		catch (Exception e){
			e.printStackTrace();
			return 0.0;
		}		
	}
}
