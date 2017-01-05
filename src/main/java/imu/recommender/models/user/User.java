package imu.recommender.models.user;

import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.MongoConnectionHelper;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.*;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.*;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Map;

import java.net.UnknownHostException;

@Entity("OptimumUsers")

public class User {
	@Id
	private ObjectId _id;
	
	private String id;
	
	private String name;
	private String mobility_type;
	private int car_ownership;
	private String extra_data;
	private String access_token;
	
	private double carUsage;
	private double ptUsage;
	private double walkUsage;
	private double bikeUsage;
	private double carUsageComparedToOthers;
	private double ptUsageComparedToOthers;
	private double bikeUsageComparedToOthers;
	private double walkUsageComparedToOthers;
	private double emissionsLastWeek;
	private  double AverageEmissions;
	private  double sugProb;
	private  double compProb;
	private  double selfProb;
	private Integer sugSuccess;
	private Integer compSuccess;
	private Integer selfSuccess;
	private Integer sugAttempts;
	private Integer compAttempts;
	private Integer selfAttempts;
	private  double MinWalked;
	private double MinBiked;
	private double MinPT;
	private double MinDrived;
	private String PreferredMode;
	private Integer MaxPreferredBikeDistance;
	private Integer MaxPreferredWalkDistance;
	private double CarPercentagePreviousWeek;
	private double PercentageReduceDriving;


	private ArrayList<OwnedVehicle> owned_vehicles;
	
	@Embedded("demographics")
	private Demographics demographics;
	
	@Embedded("personality")
	private Personality personality;
	
	@Embedded("stated_preferences")
	private StatedPreferences stated_preferences;
	
	@Embedded("facebook_data")
	private FacebookData facebook_data;
	
	@Embedded("mode_usage")
	private ModeUsage mode_usage;
	
	@Embedded("route_preferences")
	private ArrayList<RoutePreference> routePreferences;
	
	public User(){
		//initialize variables
		this.id="6EEGP034JBLydaotzqZrCs65jRdpfR4s";
		this.name = "John";
		this.access_token = "lukaios";
		this.bikeUsage = 0.0;
		this.bikeUsageComparedToOthers = 0.0;
		this.walkUsageComparedToOthers = 0.0;
		this.carUsage = 0.0;
		this.carUsageComparedToOthers = 0.0;		
		this.extra_data = "";
		this.mobility_type = "car";
		this.car_ownership = 0;
		this.ptUsage = 0.0;
		this.ptUsage = 0.0;
		this.ptUsageComparedToOthers = 0.0;
		this.walkUsage = 0.0;
		this.demographics = new Demographics();
		this.personality = new Personality();
		this.stated_preferences = new StatedPreferences();
		this.facebook_data = new FacebookData();
		this.owned_vehicles = null;
		this.emissionsLastWeek = 0.0;
		this.routePreferences = new ArrayList<RoutePreference>();
		this.mode_usage = new ModeUsage();
		this.AverageEmissions =0.0;
		this.sugProb = 0.0;
		this.sugAttempts = 0;
		this.sugSuccess = 0;
		this.compProb = 0.0;
		this.compAttempts = 0;
		this.compSuccess= 0;
		this.selfProb = 0.0;
		this.selfAttempts = 0;
		this.selfSuccess = 0;
		this.MinWalked = 0.0;
		this.MinPT = 0.0;
		this.MinDrived = 0.0;
		this.MinBiked = 0.0;
		this.PreferredMode = "";
		this.MaxPreferredBikeDistance = 3;
		this.MaxPreferredWalkDistance = 3;
		this.CarPercentagePreviousWeek = 0;
		this.PercentageReduceDriving = 80;

	}
	
	public boolean emissionsIncreasing() {
        //
        return true;

    }
	
	public boolean tooManyPublicTransportRoutes() {
        //Get percentage_of_public_transport_use_last_period from mongodb
        Double percentage_of_public_transport_use_last_period = this.getPtUsage();
        return percentage_of_public_transport_use_last_period>0.6;

    }
	
	public boolean tooManyCarRoutes() {
        //Get percentage_of_car_use_last_period from mongodb
        Double percentage_of_car_use_last_period = this.getCarUsage();
        return percentage_of_car_use_last_period>0.6;
    }
	
	public static User findByAccessToken(String accessToken){
		
		Datastore mongoDatastore;
		try {
			mongoDatastore = MongoConnectionHelper.getMongoDatastore();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		User m = (User) mongoDatastore.find(User.class).field("access_token").equal(accessToken).get();
	    if (m == null) {
	    	return null;
	    }
	    return m;		
	}
	
	public static User findById(String id){
		
		Datastore mongoDatastore;
		try {
			mongoDatastore = MongoConnectionHelper.getMongoDatastore();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		User m = (User) mongoDatastore.find(User.class).field("id").equal(id).get();
	    if (m == null) {
	    	return null;
	    }
	    return m;		
	}
	
	public String getUserPersonalityType(String id) throws UnknownHostException {

		Datastore mongoDatastore;
		mongoDatastore = MongoConnectionHelper.getMongoDatastore();

		if (!this.personality.isScores_calculated()){
			//calculate scores
			double extraversion_score = ( reverse(this.personality.getQ1()) + this.personality.getQ6() )/2;
			double agreeableness_score = ( this.personality.getQ2() + reverse(this.personality.getQ7() ) )/2;
			double conscientiousness_score = ( reverse(this.personality.getQ3() ) + this.personality.getQ8() )/2;
			double neuroticism_score = ( reverse(this.personality.getQ4()) + this.personality.getQ9() )/2;
			double openness_score = (reverse(this.personality.getQ5()) + this.personality.getQ10() )/2;
			this.personality.setExtraversion(extraversion_score);
			this.personality.setAgreeableness(agreeableness_score);
			this.personality.setConsientiousness(conscientiousness_score);
			this.personality.setNeuroticism(neuroticism_score);
			this.personality.setOpenness(openness_score);
			this.personality.setScores_calculated(true);
			//Find max score
			List<String> personalities =  Arrays.asList("Extraversion", "Agreeableness", "Conscientiousness", "Neuroticism", "Openness");
			List<Double> scores = Arrays.asList( this.personality.getExtraversion(), this.personality.getAgreeableness(), this.personality.getConsientiousness(), this.personality.getNeuroticism(), this.personality.getOpenness() );
			double max = 0.0;
			for(int i=0; i<personalities.size(); i++){
				if (scores.get(i) > max) {
					max= scores.get(i);
					this.personality.setTypeStr(personalities.get(i));
					this.personality.setType(max);
				}
			}

			Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal(id);
			//Update the personality of user.
			Personality personality = new Personality();
			personality.setTypeStr(this.personality.getTypeStr());
			personality.setType(this.personality.getType());
			personality.setQ1(this.personality.getQ1());
			personality.setQ2(this.personality.getQ2());
			personality.setQ3(this.personality.getQ3());
			personality.setQ4(this.personality.getQ4());
			personality.setQ5(this.personality.getQ5());
			personality.setQ6(this.personality.getQ6());
			personality.setQ7(this.personality.getQ7());
			personality.setQ8(this.personality.getQ8());
			personality.setQ9(this.personality.getQ9());
			personality.setQ10(this.personality.getQ10());
			personality.setConsientiousness(this.personality.getConsientiousness());
			personality.setAgreeableness(this.personality.getAgreeableness());
			personality.setOpenness(this.personality.getOpenness());
			personality.setNeuroticism(this.personality.getNeuroticism());
			personality.setExtraversion(this.personality.getExtraversion());
			personality.setScores_calculated(true);

			UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("personality",personality);
			mongoDatastore.update(query, ops);
		}

		return this.personality.getTypeStr();
	}

	public List<String> getTargetList() {
		List<String> target = new ArrayList<String>();
		try {			
			Double max = this.getMode_usage().getPt_percent();
			String user_mode="pt";
			if (this.getMode_usage().getCar_percent() > max ){
				max = this.getMode_usage().getCar_percent();
				user_mode="car";
			}
			if (this.getMode_usage().getWalk_percent() > max ){
				max = this.getMode_usage().getWalk_percent();
				user_mode = "walk";
			}
			if (this.getMode_usage().getBike_percent() > max ){
				max = this.getMode_usage().getBike_percent();
				user_mode = "bicycle";
			}
			if (user_mode.equals("pt")){
				target.add("bike&ride");
				target.add("bicycle");
				target.add("walk");
				//target.add("bikeSharing");
			}
			if (user_mode.equals("bicycle")){
			    target.add("bicycle");
				target.add("walk");
				//target.add("bikeSharing");
			}
			if (user_mode.equals("walk")){
	            target.add("bicycle");
				target.add("walk");
			}
			if (user_mode.equals("car")){
				//target.add("carSharing");
				target.add("park&ride");
				target.add("pt");
				target.add("bike&ride");
				//target.add("bikeSharing");
				target.add("bike");
				target.add("walk");
			}
		}
		catch (Exception e){
			target.add("pt");
			target.add("bike&ride");
			target.add("bicycle");
			target.add("walk");
		}

		return target;

	}

	public ObjectId get_Id() {
		return _id;
	}

	public void set_Id(ObjectId id) {
		this._id = _id;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Demographics getDemographics() {
		return demographics;
	}

	public void setDemographics(Demographics demographics) {
		this.demographics = demographics;
	}

	public Personality getPersonality() {
		return personality;
	}

	public void setPersonality(Personality personality) {
		this.personality = personality;
	}

	public String getMobility_type() {
		return mobility_type;
	}

	public void setMobility_type(String mobility_type) {
		this.mobility_type = mobility_type;
	}

	public String getExtra_data() {
		return extra_data;
	}

	public void setExtra_data(String extra_data) {
		this.extra_data = extra_data;
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public double getCarUsage() {
		return carUsage;
	}

	public void setCarUsage(double carUsage) {
		this.carUsage = carUsage;
	}

	public double getPtUsage() {
		return ptUsage;
	}

	public void setPtUsage(double ptUsage) {
		this.ptUsage = ptUsage;
	}

	public double getWalkUsage() {
		return walkUsage;
	}

	public void setWalkUsage(double walkUsage) {
		this.walkUsage = walkUsage;
	}

	public double getBikeUsage() {
		return bikeUsage;
	}

	public void setBikeUsage(double bikeUsage) {
		this.bikeUsage = bikeUsage;
	}

	public double getEmissionsLastWeek() {
		return emissionsLastWeek;
	}

	public void setEmissionsLastWeek(double emissionsLastWeek) {
		this.emissionsLastWeek = emissionsLastWeek;
	}

	public double getCarUsageComparedToOthers() {
		return carUsageComparedToOthers;
	}

	public void setCarUsageComparedToOthers(double carUsageComparedToOthers) {
		this.carUsageComparedToOthers = carUsageComparedToOthers;
	}

	public double getPtUsageComparedToOthers() {
		return ptUsageComparedToOthers;
	}

	public void setPtUsageComparedToOthers(double ptUsageComparedToOthers) {
		this.ptUsageComparedToOthers = ptUsageComparedToOthers;
	}

	public double getBikeUsageComparedToOthers() {
		return bikeUsageComparedToOthers;
	}

	public void setBikeUsageComparedToOthers(double bikeUsageComparedToOthers) {
		this.bikeUsageComparedToOthers = bikeUsageComparedToOthers;
	}
	public double getWalkUsageComparedToOthers() {
		return walkUsageComparedToOthers;
	}

	public void setWalkUsageComparedToOthers(double walkUsageComparedToOthers) {
		this.walkUsageComparedToOthers = walkUsageComparedToOthers;
	}

	public ArrayList<OwnedVehicle> getOwned_vehicles() {
		return owned_vehicles;
	}

	public void setOwned_vehicles(ArrayList<OwnedVehicle> owned_vehicles) {
		this.owned_vehicles = owned_vehicles;
	}

	public StatedPreferences getStated_preferences() {
		return stated_preferences;
	}

	public void setStated_preferences(StatedPreferences stated_preferences) {
		this.stated_preferences = stated_preferences;
	}

	public FacebookData getFacebook_data() {
		return facebook_data;
	}

	public void setFacebook_data(FacebookData facebook_data) {
		this.facebook_data = facebook_data;
	}

	public int getCar_ownership() {
		return car_ownership;
	}

	public void setCar_ownership(int car_ownership) {
		this.car_ownership = car_ownership;
	}

	public ModeUsage getMode_usage() {
		return mode_usage;
	}

	public void setMode_usage(ModeUsage mode_usage) {
		this.mode_usage = mode_usage;
	}

	public ArrayList<RoutePreference> getRoutePreferences() {
		return routePreferences;
	}

	public void setRoutePreferences(ArrayList<RoutePreference> routePreferences) {
		this.routePreferences = routePreferences;
	}

	public double getAverageEmissions() {
		return AverageEmissions;
	}

	public void setAverageEmissions(double AverageEmissions) {
		this.AverageEmissions = AverageEmissions;
	}

	public double getSugProb() {
		return sugProb;
	}

	public void setSugProb(double sugProb) {
		this.sugProb = sugProb;
	}

	public double getCompProb() {
		return compProb;
	}

	public void setCompProb(double compProb) {
		this.compProb = compProb;
	}

	public double getSelfProb() {
		return selfProb;
	}

	public void setSelfProb(double selfProb) {
		this.selfProb = selfProb;
	}

	public Integer getSugSuccess() {
		return sugSuccess;
	}

	public void setSugSuccess(Integer sugSuccess) {
		this.sugSuccess = sugSuccess;
	}

	public Integer getCompSuccess() {
		return compSuccess;
	}

	public void setCompSuccess(Integer compSuccess) {
		this.compSuccess = compSuccess;
	}

	public Integer getSelfSuccess() {
		return selfSuccess;
	}

	public void setSelfSuccess(Integer selfSuccess) {
		this.selfSuccess = selfSuccess;
	}

	public Integer getSugAttempts() {
		return sugAttempts;
	}

	public void setSugAttempts(Integer sugAttempts) {
		this.sugAttempts = sugAttempts;
	}

	public Integer getCompAttempts() {
		return compAttempts;
	}

	public void setCompAttempts(Integer compAttempts) {
		this.compAttempts = compAttempts;
	}

	public Integer getSelfAttempts() {
		return selfAttempts;
	}

	public void setSelfAttempts(Integer selfAttempts) {
		this.selfAttempts = selfAttempts;
	}

	public  List<String> getBestPersuasiveStrategy(String personality) {

		Double Suggestion = 0.0;
		Double Comparison = 0.0;
		Double SelfMonitoring = 0.0;

		Double UserSug;
		Double UserComp;
		Double UserSelf;

		try {
			UserSug = this.sugProb;
			UserComp = this.compProb;
			UserSelf = this.selfProb;

		} catch (Exception e) {
			UserSug = 0.0;
			UserComp = 0.0;
			UserSelf = 0.0;
		}

		String strategy = "";
		switch (personality) {
			case "Extraversion":
				if (UserSug == 0.0 & UserComp == 0.0 & UserSelf == 0.0) {
					Suggestion = GetProperties.getSugEx();
					Comparison = GetProperties.getCompEx();
					SelfMonitoring = GetProperties.getSelfEx();

				} else {
					Suggestion = (UserSug + GetProperties.getSugEx()) / 2.0;
					Comparison = (UserComp + GetProperties.getCompEx()) / 2.0;
					SelfMonitoring = (UserSelf + GetProperties.getSelfEx()) / 2.0;
				}
				break;
			case "Agreeableness":

				if (UserSug == 0.0 & UserComp == 0.0 & UserSelf == 0.0) {
					Suggestion = GetProperties.getSugAg();
					Comparison = GetProperties.getCompAg();
					SelfMonitoring = GetProperties.getSelfAg();

				} else {
					Suggestion = (UserSug + GetProperties.getSugAg()) / 2.0;
					Comparison = (UserComp + GetProperties.getCompAg()) / 2.0;
					SelfMonitoring = (UserSelf + GetProperties.getSelfAg()) / 2.0;
				}
				break;
			case "Openness":

				if (UserSug == 0.0 & UserComp == 0.0 & UserSelf == 0.0) {
					Suggestion = GetProperties.getSugOp();
					Comparison = GetProperties.getCompOp();
					SelfMonitoring = GetProperties.getSelfOp();

				} else {
					Suggestion = (UserSug + GetProperties.getSugOp()) / 2.0;
					Comparison = (UserComp + GetProperties.getCompOp()) / 2.0;
					SelfMonitoring = (UserSelf + GetProperties.getSelfOp()) / 2.0;
				}
				break;
			case "Conscientiousness":

				if (UserSug == 0.0 & UserComp == 0.0 & UserSelf == 0.0) {
					Suggestion = GetProperties.getSugCons();
					Comparison = GetProperties.getCompCons();
					SelfMonitoring = GetProperties.getSelfCons();

				} else {
					Suggestion = (UserSug + GetProperties.getSugCons()) / 2.0;
					Comparison = (UserComp + GetProperties.getCompCons()) / 2.0;
					SelfMonitoring = (UserSelf + GetProperties.getSelfCons()) / 2.0;
				}
				break;
			case "Neuroticism":

				if (UserSug == 0.0 & UserComp == 0.0 & UserSelf == 0.0) {
					Suggestion = GetProperties.getSugN();
					Comparison = GetProperties.getCompN();
					SelfMonitoring = GetProperties.getSelfN();

				} else {
					Suggestion = (UserSug + GetProperties.getSugN()) / 2.0;
					Comparison = (UserComp + GetProperties.getCompN()) / 2.0;
					SelfMonitoring = (UserSelf + GetProperties.getSelfN()) / 2.0;
				}
				break;
		}

        List<String> rankedStrategies = new ArrayList<String>();
        TreeMap<String, Double> rankedStrategiesMap = new TreeMap<String, Double>();

        rankedStrategiesMap.put("suggestion", Suggestion);
        rankedStrategiesMap.put("comparison", Comparison);
        rankedStrategiesMap.put("self-monitoring", SelfMonitoring);

        for (Map.Entry<String, Double> entry : rankedStrategiesMap.entrySet()){
            rankedStrategies.add(entry.getKey());
        }

		return rankedStrategies;

	}

	private Double reverse(Double score){
		Double reversed = 0.0;
		if (score == 1.0){
			reversed = 5.0;
		}
		else if (score == 2.0){
			reversed = 4.0;
		}
		else if (score == 4.0){
			reversed = 2.0;
		}
		if (score == 5.0){
			reversed = 1.0;
		}
		else{
			reversed = 3.0;
		}
		return reversed;
	}

	public double getMinWalked() {
		return MinWalked;
	}

	public void setMinWalked(double minWalked) {
		MinWalked = minWalked;
	}

	public double getMinBiked() {
		return MinBiked;
	}

	public void setMinBiked(double minBiked) {
		MinBiked = minBiked;
	}

	public double getMinPT() {
		return MinPT;
	}

	public void setMinPT(double minPT) {
		MinPT = minPT;
	}

	public double getMinDrived() {
		return MinDrived;
	}

	public void setMinDrived(double minDrived) {
		MinDrived = minDrived;
	}

	public String getPreferredMode() {
		return PreferredMode;
	}

	public void setPreferredMode(String preferredMode) {
		PreferredMode = preferredMode;
	}

	public Integer getMaxPreferredBikeDistance() {
		return MaxPreferredBikeDistance;
	}

	public void setMaxPreferredBikeDistance(Integer maxPreferredBikeDistance) {
		MaxPreferredBikeDistance = maxPreferredBikeDistance;
	}

	public Integer getMaxPreferredWalkDistance() {
		return MaxPreferredWalkDistance;
	}

	public void setMaxPreferredWalkDistance(Integer maxPreferredWalkDistance) {
		MaxPreferredWalkDistance = maxPreferredWalkDistance;
	}
	public double getCarPercentagePreviousWeek() {
		return CarPercentagePreviousWeek;
	}

	public void setCarPercentagePreviousWeek(double CarPercentagePreviousWeek) {
		CarPercentagePreviousWeek = CarPercentagePreviousWeek;
	}
}
