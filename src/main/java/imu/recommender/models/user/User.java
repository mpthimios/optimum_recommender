package imu.recommender.models.user;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import imu.recommender.helpers.GetProperties;
import imu.recommender.helpers.MongoConnectionHelper;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


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
	private double bikeUsageComparedToOthersGW;
	private double walkUsageComparedToOthersGW;
	private double parkrideUsageComparedToOthers;
	private double bikerideUsageComparedToOthers;
	private double ptUsageComparedToOthersGW;
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
	private double CarPercentagePreviousWeek;
	private double PercentageReduceDriving;
	private String language;
	private Date fistLogin;
	private Integer total_activities;
	private String persuasion;
	private String group;
	private Integer count;


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

	@Embedded("mode_usage_last_week")
	private ModeUsageLastWeek mode_usage_last_week;

	@Embedded("mode_usage_previous_week")
	private ModeUsagePreviousWeek mode_usage_previous_week;
	
	@Embedded("route_preferences")
	private ArrayList<RoutePreference> routePreferences;

	private static Logger logger = Logger.getLogger(User.class);

	public User(){
		//initialize variables
		this.id="6EEGP034JBLydaotzqZrCs65jRdpfR4s";
		this.name = "John";
		this.access_token = "lukaios";
		this.bikeUsageComparedToOthers = 0.0;
		this.walkUsageComparedToOthers = 0.0;
		this.bikeUsageComparedToOthersGW = 0.0;
		this.walkUsageComparedToOthersGW = 0.0;
		this.ptUsageComparedToOthersGW = 0.0;
		this.carUsageComparedToOthers = 0.0;
		this.parkrideUsageComparedToOthers = 0.0;
		this.bikerideUsageComparedToOthers = 0.0;
		this.extra_data = "";
		this.mobility_type = "car";
		this.car_ownership = 0;
		this.ptUsageComparedToOthers = 0.0;
		this.demographics = new Demographics();
		this.personality = new Personality();
		this.stated_preferences = new StatedPreferences();
		this.facebook_data = new FacebookData();
		this.owned_vehicles = null;
		this.emissionsLastWeek = 0.0;
		this.routePreferences = new ArrayList<RoutePreference>();
		this.mode_usage = new ModeUsage();
		this.mode_usage_last_week = new ModeUsageLastWeek();
		this.mode_usage_previous_week = new ModeUsagePreviousWeek();
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
		//this.PreferredMode = "";
		//this.MaxPreferredBikeDistance = 3;
		//this.MaxPreferredWalkDistance = 3;
		this.CarPercentagePreviousWeek = 0;
		this.PercentageReduceDriving = 80.0;
		this.language = "en";
		this.total_activities = 0;
		this.persuasion = "";
		this.count = 0;

	}
	
	public boolean emissionsIncreasing() {
        //
		Double emissions_last_period = this.getEmissionsLastWeek();
		return emissions_last_period>300.0;

    }
	
	public boolean tooManyPublicTransportRoutes() {
        //Get percentage_of_public_transport_use_last_period from mongodb
        Double percentage_of_public_transport_use_last_period = this.getMode_usage().getPt_percent();
        return percentage_of_public_transport_use_last_period>51.0;

    }
	
	public boolean tooManyCarRoutes() {
        //Get percentage_of_car_use_last_period from mongodb
        Double percentage_of_car_use_last_period = this.getMode_usage().getCar_percent();
        return percentage_of_car_use_last_period>51.0;
    }
	
	public static User findByAccessToken(String accessToken){
		
		Datastore mongoDatastore;
		try {
			mongoDatastore = MongoConnectionHelper.getMongoDatastore();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
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
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
			return null;
		}
		
		User m = (User) mongoDatastore.find(User.class).field("id").equal(id).get();
	    if (m == null) {
	    	return null;
	    }
	    return m;		
	}
	
	public String getUserPersonalityType(String id, Datastore mongoDatastore) throws UnknownHostException {

		//Datastore mongoDatastore;
		//mongoDatastore = MongoConnectionHelper.getMongoDatastore();
		String pref_mode= this.personality.getPreferredMode();

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
			personality.setPreferredMode(pref_mode);
			personality.setMaxPreferredBikeDistance(this.personality.getMaxPreferredBikeDistance());
			personality.setMaxPreferredWalkDistance(this.personality.getMaxPreferredWalkDistance());

			UpdateOperations<User> ops = mongoDatastore.createUpdateOperations(User.class).set("personality",personality);
			mongoDatastore.update(query, ops);
		}

		return this.personality.getTypeStr();
	}

	public List<String> getTargetList() {
		List<String> target = new ArrayList<String>();
		try {
			String user_mode="car";
			if(this.getTotal_activities()<=10 || 
					(valueEquals(this.getMode_usage().getPt_percent(), 0) && 
							valueEquals(this.getMode_usage().getCar_percent(), 0) && 
							valueEquals(this.getMode_usage().getWalk_percent(), 0) && 
							valueEquals(this.getMode_usage().getBike_percent(), 0) )){
				user_mode = this.personality.convertPreferredMode();
			}
			else {
				Double max = this.getMode_usage().getCar_percent();
				if (this.getMode_usage().getPt_percent() > max) {
					max = this.getMode_usage().getPt_percent();
					user_mode = "pt";
				}
				if (this.getMode_usage().getWalk_percent() > max) {
					max = this.getMode_usage().getWalk_percent();
					user_mode = "walk";
				}
				if (this.getMode_usage().getBike_percent() > max) {
					max = this.getMode_usage().getBike_percent();
					user_mode = "bicycle";
				}
			}
			if ("pt".equals(user_mode)){
				target.add("bike&ride");
				target.add("BikeSharing");
				target.add("bicycle");
				target.add("walk");

			}
			if ("bicycle".equals(user_mode)){
			    target.add("bicycle");
				target.add("walk");
				target.add("BikeSharing");
			}
			if ("walk".equals(user_mode)){
	            target.add("bicycle");
				target.add("walk");
			}
			if ("car".equals(user_mode)){
				target.add("CarSharing");
				target.add("park&ride");
				target.add("pt");
				target.add("bike&ride");
				target.add("BikeSharing");
				target.add("bicycle");
				target.add("walk");
			}
		}
		catch (Exception e){
			target.add("pt");
			target.add("bike&ride");
			target.add("BikeSharing");
			target.add("bicycle");
			target.add("walk");
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
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

	public double getBikeUsageComparedToOthersGW() {
		return bikeUsageComparedToOthersGW;
	}

	public void setBikeUsageComparedToOthersGW(double bikeUsageComparedToOthersGW) {
		this.bikeUsageComparedToOthersGW = bikeUsageComparedToOthersGW;
	}
	public double getWalkUsageComparedToOthersGW() {
		return walkUsageComparedToOthersGW;
	}

	public void setWalkUsageComparedToOthersGW(double walkUsageComparedToOthersGW) {
		this.walkUsageComparedToOthersGW = walkUsageComparedToOthersGW;
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
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
		}

		String strategy = "";
		switch (personality) {
			case "Extraversion":
				if (valueEquals(UserSug, 0.0) & valueEquals(UserComp, 0.0) & valueEquals(UserSelf, 0.0)) {
					Suggestion = GetProperties.getSugEx();
					Comparison = GetProperties.getCompEx();
					SelfMonitoring = GetProperties.getSelfEx();

				} else {
					Suggestion = UserSug;
					Comparison = UserComp;
					SelfMonitoring = UserSelf;
				}
				break;
			case "Agreeableness":

				if (valueEquals(UserSug, 0.0) & valueEquals(UserComp, 0.0) & valueEquals(UserSelf, 0.0)) {
					Suggestion = GetProperties.getSugAg();
					Comparison = GetProperties.getCompAg();
					SelfMonitoring = GetProperties.getSelfAg();

				} else {
					Suggestion = UserSug;
					Comparison = UserComp;
					SelfMonitoring = UserSelf;
				}
				break;
			case "Openness":

				if (valueEquals(UserSug, 0.0) & valueEquals(UserComp, 0.0) & valueEquals(UserSelf, 0.0)) {
					Suggestion = GetProperties.getSugOp();
					Comparison = GetProperties.getCompOp();
					SelfMonitoring = GetProperties.getSelfOp();

				} else {
					Suggestion = UserSug;
					Comparison = UserComp;
					SelfMonitoring = UserSelf;
				}
				break;
			case "Conscientiousness":

				if (valueEquals(UserSug, 0.0) & valueEquals(UserComp, 0.0) & valueEquals(UserSelf, 0.0)) {
					Suggestion = GetProperties.getSugCons();
					Comparison = GetProperties.getCompCons();
					SelfMonitoring = GetProperties.getSelfCons();

				} else {
					Suggestion = UserSug;
					Comparison = UserComp;
					SelfMonitoring = UserSelf;
				}
				break;
			case "Neuroticism":

				if (valueEquals(UserSug, 0.0) & valueEquals(UserComp, 0.0) & valueEquals(UserSelf, 0.0)) {
					Suggestion = GetProperties.getSugN();
					Comparison = GetProperties.getCompN();
					SelfMonitoring = GetProperties.getSelfN();

				} else {
					Suggestion = UserSug;
					Comparison = UserComp;
					SelfMonitoring = UserSelf;
				}
				break;
		}

        List<String> rankedStrategies = new ArrayList<String>();

		LinkedHashMap<String, Double> rankedStrategiesMap = new LinkedHashMap<String, Double>();

        rankedStrategiesMap.put("suggestion", Suggestion);
        rankedStrategiesMap.put("comparison", Comparison);
        rankedStrategiesMap.put("self-monitoring",SelfMonitoring);

		rankedStrategiesMap = entriesSortedByValues(rankedStrategiesMap);

        for (Map.Entry<String, Double> entry : rankedStrategiesMap.entrySet()){
            rankedStrategies.add(entry.getKey());
        }
		return rankedStrategies;

	}

	private Double reverse(Double score){
		Double reversed;
		if (valueEquals(score, 1.0)){
			reversed = 5.0;
		}
		else if (valueEquals(score, 2.0)){
			reversed = 4.0;
		}
		else if (valueEquals(score, 4.0)){
			reversed = 2.0;
		}
		else if (valueEquals(score, 5.0)){
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

	/*public String getPreferredMode() {
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
	}*/
	public double getCarPercentagePreviousWeek() {
		return CarPercentagePreviousWeek;
	}

	public void setCarPercentagePreviousWeek(double CarPercentagePreviousWeek) {
		this.CarPercentagePreviousWeek = CarPercentagePreviousWeek;
	}
	public double getPercentageReduceDriving() {
		return PercentageReduceDriving;
	}

	public void setPercentageReduceDriving(double percentageReduceDriving) {
		this.PercentageReduceDriving = percentageReduceDriving;
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

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Date getFistLogin() {
		return fistLogin;
	}

	public void setFistLogin(Date fistLogin) {
		this.fistLogin = fistLogin;
	}

	public Integer getTotal_activities() {
		return total_activities;
	}

	public void setTotal_activities(Integer total_activities) {
		this.total_activities = total_activities;
	}

	public String getPersuasion() {
		return persuasion;
	}

	public void setPersuasion(String persuasion) {
		this.persuasion = persuasion;
	}
	public void classify (User user, Datastore mongoDatastore){
		Query<User> query = mongoDatastore.createQuery(User.class).field("id").equal( user.getId());
		if (query.field("persuasion").exists().asList().isEmpty()) {  // .asList().isEmpty()
			String personality = this.getPersonality().getTypeStr();
			query = mongoDatastore.createQuery(User.class).field("id").equal( user.getId());
			if ("Openess".equals(personality) || "Extraversion".equals(personality)) {
				this.setPersuasion("A");
				mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("persuasion", "A"),true);

			} else {
				Random rn = new Random();
				int number = rn.nextInt(100);
				System.out.println(number);
				if (number>50) {
					this.setPersuasion("B");
					mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("persuasion", "B"),true);
				}
				else {
					this.setPersuasion("A");
					mongoDatastore.update(query, mongoDatastore.createUpdateOperations(User.class).set("persuasion", "A"),true);
				}
			}
		}
		else {
			System.out.print("Classified");
		}
	}

	public Boolean getFeedback(String userId , Datastore mongoDatastore) {
		DBCollection trips = mongoDatastore.getDB().getCollection("UserTrip");
		DBCollection routes = mongoDatastore.getDB().getCollection("UserRoute");
		Integer days;

		try {
			BasicDBObject RouteQuery1 = new BasicDBObject();
			RouteQuery1.put("route_feedback.helpful", false);
			RouteQuery1.put("route_feedback.helpful", true);
			RouteQuery1.put("userId",userId);
			DBCursor RequsetIds = routes.find(RouteQuery1);
			if (RequsetIds.size() > 0) {
				List<Integer> requestIds = new ArrayList<Integer>();
				while (RequsetIds.hasNext()) {
					requestIds.add(Integer.parseInt(RequsetIds.next().get("_id").toString()));
				}
				//sort request Ids and get the last one
				Collections.sort(requestIds, Collections.reverseOrder());

				BasicDBObject TripQuery = new BasicDBObject();
				TripQuery.put("requestId", requestIds.get(0).toString());
				DBCursor tripsIds = trips.find(TripQuery);
				String feedback_date = "";
				if (tripsIds.hasNext()) {
					feedback_date = tripsIds.next().get("createdat").toString();
				}
				//Calculate the number of days since last feedback
				Timestamp endDate = new Timestamp(System.currentTimeMillis());
				DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
				Date startDate = null;
				try {
					startDate = df.parse(feedback_date);
				} catch (ParseException e) {
					logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
				}
				//Timestamp startDate =  Timestamp.valueOf(feedback_date);
				long days_long = Math.abs((endDate.getTime() - startDate.getTime()) / 86400000);
				days = Math.round(days_long);
			}
			else{
				//Define days 4 in order to enable feedback
				days = 4;
			}
		}
		catch (Exception e){
			days = 4;
			logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
		}


		if (days>=3){
			return Boolean.TRUE;
		}
		else {
			return Boolean.FALSE;
		}

	}

	public double getParkrideUsageComparedToOthers() {
		return parkrideUsageComparedToOthers;
	}

	public void setParkrideUsageComparedToOthers(double parkrideUsageComparedToOthers) {
		this.parkrideUsageComparedToOthers = parkrideUsageComparedToOthers;
	}

	public double getBikerideUsageComparedToOthers() {
		return bikerideUsageComparedToOthers;
	}

	public void setBikerideUsageComparedToOthers(double bikerideUsageComparedToOthers) {
		this.bikerideUsageComparedToOthers = bikerideUsageComparedToOthers;
	}
	
	private boolean valueEquals(double a, double b) {
        return (Math.abs(a - b) < 0.0000001);
    }

	public double getPtUsageComparedToOthersGW() {
		return ptUsageComparedToOthersGW;
	}

	public void setPtUsageComparedToOthersGW(double ptUsageComparedToOthersGW) {
		this.ptUsageComparedToOthersGW = ptUsageComparedToOthersGW;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public ModeUsageLastWeek getMode_usage_last_week() {
		return mode_usage_last_week;
	}

	public void setMode_usage_last_week(ModeUsageLastWeek mode_usage_last_week) {
		this.mode_usage_last_week = mode_usage_last_week;
	}

	public ModeUsagePreviousWeek getMode_usage_previous_week() {
		return mode_usage_previous_week;
	}

	public void setMode_usage_previous_week(ModeUsagePreviousWeek mode_usage_previous_week) {
		this.mode_usage_previous_week = mode_usage_previous_week;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}
}
