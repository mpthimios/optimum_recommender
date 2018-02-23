package imu.recommender;

import at.ac.ait.ariadne.routeformat.RouteSegment;
import imu.recommender.helpers.RecommenderModes;
import imu.recommender.models.route.RouteModel;
import imu.recommender.models.user.User;
import org.apache.log4j.Logger;
import uk.recurse.geocoding.reverse.Country;
import uk.recurse.geocoding.reverse.ReverseGeocoder;

import java.util.Optional;

public class BehaviouralModel {
	
	private static Logger logger = Logger.getLogger(BehaviouralModel.class);
	
	public static double calculateBhaviouralModelUtility (RouteModel route, User user, double locationValue){
		double utility = 0.0;
				double cost = 0.0;
		double segment_cost;
		double time = (double) route.getRoute().getDurationSeconds()/60;
		logger.debug("Mode : "+route.getRoute().getAdditionalInfo().get("mode"));
		logger.debug("Time: "+time);
		for (int j = 0; j < route.getRoute().getSegments().size(); j++) {
			RouteSegment segment = route.getRoute().getSegments().get(j);
			try {
				//segment_cost = (double)segment.getAdditionalInfo().get("estimatedCost");
				segment_cost =  Double.parseDouble(segment.getAdditionalInfo().get("additionalProperties").toString().split("estimatedCost=")[1].split("}")[0]);

			}
			catch (Exception e){
				logger.debug(e);
				segment_cost = 0.0;
			}
			cost = segment_cost + cost;
		}
		logger.debug("Estimated Cost : "+cost);
		switch (route.getMode()){
			case RecommenderModes.WALK:
				utility = U10(cost, time, user, locationValue);
				break;
			case RecommenderModes.BICYCLE:
				utility = U11(cost, time, user, locationValue);
				break;
			case RecommenderModes.BIKE_AND_RIDE:
				utility = U2(cost, time, user, locationValue);
				break;
			case RecommenderModes.PUBLIC_TRANSPORT:
				utility = U4(cost, time, user, locationValue);
				break;
			case RecommenderModes.PARK_AND_RIDE_WITH_BIKE:
				utility = U3(cost, time, user, locationValue);
				break;
			case RecommenderModes.PARK_AND_RIDE:
				utility = U1(cost, time, user, locationValue);
				break;
			case RecommenderModes.CAR_SHARING:
				utility = U6(cost, time, user, locationValue);
				break;
			case RecommenderModes.CAR:
				utility = U7(cost, time, user, locationValue);
				break;
			/*case RecommenderModes.CARPOOLING:
				utility = U8(cost, time, user, locationValue);
				break;
			case RecommenderModes.UBER:
				utility = U9(cost, time, user, locationValue);
				break;*/
			default:
				break;
		}

		logger.debug("behavioural model utility: " + utility);
		return utility;
	}

	public static double U1(double cost, double time, User user, double locationValue){
		double ASC1 = 1.540;
		double TravelCost1 = -0.012;
		double TravelTime1 = -0.047;
		double gAspEnv1 = -0.515;

		double U1 = ASC1 + TravelCost1*cost + TravelTime1*time +
				gAspEnv1*DM(user, locationValue);

		return U1;
	}
	public static double U2(double cost, double time, User user, double locationValue){
		double ASC2 = 0.264;
		double TravelCost2 = -0.007;
		double TravelTime2 = -0.027;
		double gAspEnv2 = 0.455;
		double gEI = -0.140;
		double error = 0.929;

		double U2 = ASC2 + TravelCost2*cost + TravelTime2*time +
				gAspEnv2*AspEnv(user, locationValue)+ gEI*EI(user, locationValue)+ error;

		return U2;
	}
	public static double U3(double cost, double time, User user, double locationValue){
		double ASC3 = 0.264;
		double TravelCost3 = -0.007;
		double TravelTime3 = -0.027;
		double gAspEnv3 = 0.455;
		double gEI = -0.140;
		double error = 0.929;

		double U3 = ASC3 + TravelCost3*cost + TravelTime3*time +
				gAspEnv3*AspEnv(user, locationValue)+ gEI*EI(user, locationValue)+ error;

		return U3;
	}
	public static double U4(double cost, double time, User user, double locationValue){
		double ASC4 = 2.820;
		double TravelCost4 = -0.015;
		double TravelTime4 = -0.090;
		double gAspEnv4 = 0.455;
		double gEI = -0.140;
		double error = 0.929;

		double U4 = ASC4 + TravelCost4*cost + TravelTime4*time +
				gAspEnv4*AspEnv(user, locationValue)+ gEI*EI(user, locationValue)+ error;

		return U4;
	}
	public static double U5(double cost, double time, User user, double locationValue){
		double ASC5 = 1.130;
		double TravelCost5 = -0.020;
		double TravelTime5 = -0.049;


		double U5 = ASC5 + TravelCost5*cost + TravelTime5*time;

		return U5;
	}
	public static double U6(double cost, double time, User user, double locationValue){
		double ASC6 = 1.160;
		double TravelCost6 = -0.020;
		double TravelTime6 = -0.057;
		double gLA= 0.346;


		double U6 = ASC6 + TravelCost6*cost + TravelTime6*time+ gLA*LateArrival(user, locationValue);

		return U6;
	}
	public static double U7(double cost, double time, User user, double locationValue){
		double ASC7 = 3.420;
		double TravelCost7 = -0.012;
		double TravelTime7 = -0.082;
		double gDIE= 0.614;
		double gDM = -0.550;
		double bflex = -1.710;


		double U7 = ASC7 + TravelCost7*cost + TravelTime7*time+ gDIE*DIE(user, locationValue)+
				bflex*educationValue(user.getDemographics().getEducation())+ gDM*DM(user, locationValue);

		return U7;
	}
	public static double U8(double cost, double time, User user, double locationValue){
		double ASC8 = 1.240;
		double TravelCost8 = -0.022;
		double TravelTime8 = -0.057;
		double gLA = 0.346;


		double U8 = ASC8 + TravelCost8*cost + TravelTime8*time+ gLA*LateArrival(user, locationValue);

		return U8;
	}

	public static double U9(double cost, double time, User user, double locationValue){
		double ASC9 = 0.681;
		double TravelCost9 = -0.015;
		double TravelTime9 = -0.050;
		double gLA = 0.346;


		double U9 = ASC9 + TravelCost9*cost + TravelTime9*time+ gLA*LateArrival(user, locationValue);

		return U9;
	}

	public static double U10(double cost, double time, User user, double locationValue){
		double ASC10 = 0.264;
		double TravelCost10 = -0.007;
		double TravelTime10 = -0.027;
		double gAspEnv10 = 0.455;
		double gEI = -0.140;
		double error = 0.929;

		double U10 = ASC10 + TravelCost10*cost + TravelTime10*time +
				gAspEnv10*AspEnv(user, locationValue)+ gEI*EI(user, locationValue)+ error;

		return U10;
	}
	public static double U11(double cost, double time, User user, double locationValue){
		double ASC9 = 0.681;
		double TravelCost9 = -0.015;
		double TravelTime9 = -0.050;
		double gLA = 0.346;


		double U9 = ASC9 + TravelCost9*cost + TravelTime9*time+ gLA*LateArrival(user, locationValue);

		return U9;
	}

	/*public static double U1(double cost, double time, User user, double locationValue){
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
	}*/
	private static double LateArrival(User user, double locationValue){

		double g1 = 0.0361;
		double g2 = -0.797;
		double sigmaLA = 1.41;

		double result = g1*(double)user.getDemographics().getAge() +
				g2*locationValue + sigmaLA ;
		return result;
	}

	private static double AspEnv(User user, double locationValue){
		double g0 = -1.710;
		double g1 = 0.079;
		double g2 = 1.470;
		double g3 = 0.733;
		double g4 = 1.090;
		
		double result = g0 + 
				g1*(double)user.getDemographics().getAge() +
				g2*genderValue(user.getDemographics().getGender()) +
				g3*locationValue +
				g4*educationValue(user.getDemographics().getEducation());
		return result;
	}

	private static double DIE(User user, double locationValue){
		double g0 = 1.300;
		double g1 = 0.111;
		double g2 = -0.397;

		double result = g0 +
				g1*(double)user.getDemographics().getAge() +
				g2*locationValue;
		return result;
	}

	private static double DM(User user, double locationValue){
		double g0 = 1.660;
		double g1 = 0.074;
		double g2 = 1.200;
		double g3 = 0.911;
		double g4 = 0.688;

		double result = g0 +
				g1*(double)user.getDemographics().getAge() +
				g2*genderValue(user.getDemographics().getGender()) +
				g3*educationValue(user.getDemographics().getEducation()) +
				g4*locationValue;
		return result;
	}

	private static double EI(User user, double locationValue){
		double g0 = 1.480;
		double g1 = 0.077;
		double g2 = 1.780;
		double g3 = 1.320;
		double g4 = 0.584;

		double result = g0 +
				g1*(double)user.getDemographics().getAge() +
				g2*genderValue(user.getDemographics().getGender()) +
				g3*educationValue(user.getDemographics().getEducation()) +
				g4*locationValue;
		return result;
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
			logger.debug(e);
			return 0.0;
		}		
	}
}
