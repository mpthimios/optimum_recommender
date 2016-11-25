package imu.recommender.models.user;

import java.util.Arrays;
import java.util.List;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.*;

import imu.recommender.helpers.MongoConnectionHelper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

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
	private double emissionsLastWeek;

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
	
	public User(){
		//initialize variables
		this.name = "John";
		this.access_token = "lukaios";
		this.bikeUsage = 0.0;
		this.bikeUsageComparedToOthers = 0.0;
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

	public String getBestPersuasiveStrategy(String personality){
		String strategy = "";
		if (personality.equals("Extraversion") ){
			strategy="comparison";
		}
		else if (personality.equals("Agreeableness") ){
			strategy="self-monitoring";
		}
		else if (personality.equals("Openness") ){
			strategy="self-monitoring";
		}
		else if (personality.equals("Conscientiousness") ){
			strategy="suggestion ";
		}
		else if (personality.equals("Neuroticism") ){
			strategy="suggestion ";
		}

		return strategy;

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
	
}
