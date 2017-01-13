import at.ac.ait.ariadne.routeformat.RouteFormatRoot;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import imu.recommender.CalculateMessageUtilities;
import imu.recommender.Recommender;
import imu.recommender.helpers.*;
import imu.recommender.helpers.GetProperties;
import imu.recommender.jobs.UpdateStrategiesProbabilities;
import imu.recommender.models.user.ModeUsage;
import imu.recommender.models.user.Personality;
import imu.recommender.models.user.User;
import org.hamcrest.CoreMatchers;
import org.hamcrest.CoreMatchers.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers.*;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import java.util.*;
import imu.recommender.Recommender;
import java.io.*;
import at.ac.ait.ariadne.routeformat.RouteFormatRoot;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import imu.recommender.helpers.GetProperties;
import imu.recommender.models.user.ModeUsage;

public class CalculateMessageTest{
    @Test
    //Test case 1
    public void CalculateMessageTest1() throws Exception{
        String target= "walk";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Change user persuadability
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setQ1(5.0);
        personality.setQ2(5.0);
        personality.setQ3(1.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(1.0);
        personality.setQ7(1.0);
        personality.setQ8(5.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target, mongoDatastore);
        System.out.println(mes);
        Assert.assertThat(mes,CoreMatchers.anyOf(CoreMatchers.is("Today it s sunny! Take the opportunity to walk to save CO2 emissions._suggestion"),CoreMatchers.is("Nice weather for walking._suggestion")));
    }

    @Test
    //Test case 2
    public void CalculateMessageTest2() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);

        user.setBikeUsageComparedToOthers(80.0);
        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target, mongoDatastore);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("80.0% of users biked for similar distances_comparison"));
    }

    @Test
    //Test case 3
    public void CalculateMessageTest3() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        user.setBikeUsageComparedToOthers(0.0);
        user.setBikeUsageComparedToOthersGW(90.0);
        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target, mongoDatastore);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("90.0% of users biked when the weather was as good as today!_comparison"));
    }

    @Test
    //Test case 4
    public void CalculateMessageTest4() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Conscientiousness
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        user.setBikeUsageComparedToOthers(0);
        user.setMinBiked(2.0);
        user.setEmissionsLastWeek(0);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target, mongoDatastore);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("Nice weather!When the weather was good you biked 2.0 minutes per day on average._self-monitoring"));
    }

    @Test
    //Test case 5
    public void CalculateMessageTest5() throws Exception{

        List<String> targetList = new ArrayList<String>();
        targetList.add("pt");
        targetList.add("bicycle");
        String message="";
        List<String> contextList = new ArrayList<String>();
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Change user persuadability
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(5.0);
        personality.setQ2(5.0);
        personality.setQ3(1.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(1.0);
        personality.setQ7(1.0);
        personality.setQ8(5.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        //Find the message
        int j=0;
        while (message != "_suggestion" && message != "_comparison" && message != "_self-monitoring" && j<targetList.size() ) {
            String target = targetList.get(j);
            message = CalculateMessageUtilities.calculateForUser(contextList, user, target, mongoDatastore);
            j++;
        }
        System.out.println(message);
        Assert.assertThat(message, CoreMatchers.anyOf( CoreMatchers.is("It s not too far. Take your bike instead of car to  reduce your weekly emissions._suggestion"),CoreMatchers.is("You are near to your destination. It s an opportunity to bike._suggestion") ) );
    }
    @Test
    //Test case 6
    public void CalculateMessageTest6() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("TooManyCarRoutes");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        user.setPercentageReduceDriving(60.0);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target, mongoDatastore);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("Take public transport. 60.0 % of users have already reduced driving._comparison"));
    }

    @Test
    //Test case 7
    public void CalculateMessageTest7() throws Exception{

        GetProperties.setCompAg(0.196);
        GetProperties.setSugAg(0.17);
        GetProperties.setSelfAg(0.2);
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("Duration");
        contextList.add("emissionsIncreasing");
        contextList.add("TooManyCarRoutes");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy self-monitoring
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Agreeableness
        Personality personality = new Personality();
        personality.setQ1(5.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(1.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());
        //UpdateOperations<User> updateQuery = mongoDatastore.createUpdateOperations(User.class).disableValidation().set("SelfProb", 90.0);
        //mongoDatastore.update(user, updateQuery);
        user.setEmissionsLastWeek(0.0);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target, mongoDatastore);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("Last week your emissions are increasing. Try more!_self-monitoring"));
    }
    @Test
    //Test case 8
    public void CalculateMessageTest8() throws Exception{
        Double userProb = 0.0;
        userProb = UpdateStrategiesProbabilities.calculateUserProbability(20, 10, 4, 6);
        Assert.assertThat(userProb, CoreMatchers.is(0.6071428571428571));

    }
    @Test
    //Test case 9
    public void CalculateMessageTest9() throws Exception{
        GetProperties.setweatherId("e0f5c1a1d86a8bd69e497197804d411c");
        String target= "walk";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Change user persuadability
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setQ1(5.0);
        personality.setQ2(1.0);
        personality.setQ3(1.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(1.0);
        personality.setQ7(5.0);
        personality.setQ8(5.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);

        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(51.0);
        mode.setPt_percent(30.0);
        mode.setCar_percent(10.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);


        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //GetProperties getProperties = new GetProperties();

        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes2.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];
        System.out.println(res2);
        System.out.println(res1);

        Assert.assertThat(res3,CoreMatchers.anyOf(CoreMatchers.is(" \"Today it s sunny! Take the opportunity to walk to save CO2 emissions.\""),CoreMatchers.is(" \"Nice weather for walking.\"")));
    }

    @Test
    //Test case 10
    public void CalculateMessageTest10() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        //GetProperties.setMaxBikeDistance(15000);
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);

        user.setBikeUsageComparedToOthers(80.0);

        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(51.0);
        mode.setPt_percent(30.0);
        mode.setCar_percent(10.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);


        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //GetProperties getProperties = new GetProperties();

        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes3.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];

        Assert.assertThat(res3, CoreMatchers.is(" \"80.0% of users biked for similar distances\""));
    }

    @Test
    //Test case 11
    public void CalculateMessageTest11() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        user.setBikeUsageComparedToOthers(0.0);
        user.setBikeUsageComparedToOthersGW(90.0);

        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(51.0);
        mode.setPt_percent(30.0);
        mode.setCar_percent(10.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);


        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //GetProperties getProperties = new GetProperties();

        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes3.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];

        Assert.assertThat(res3, CoreMatchers.is(" \"90.0% of users biked when the weather was as good as today!\""));
    }

    @Test
    //Test case 12
    public void CalculateMessageTest12() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Conscientiousness
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        user.setBikeUsageComparedToOthers(0);
        user.setMinBiked(2.0);
        user.setEmissionsLastWeek(0);

        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(51.0);
        mode.setPt_percent(30.0);
        mode.setCar_percent(10.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //GetProperties getProperties = new GetProperties();

        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes3.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];

        Assert.assertThat(res3, CoreMatchers.is(" \"Nice weather!When the weather was good you biked 2.0 minutes per day on average.\""));

    }

    @Test
    //Test case 13
    public void CalculateMessageTest13() throws Exception{

        List<String> targetList = new ArrayList<String>();
        targetList.add("pt");
        targetList.add("bicycle");
        String message="";
        List<String> contextList = new ArrayList<String>();
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Change user persuadability
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(5.0);
        personality.setQ2(5.0);
        personality.setQ3(1.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(1.0);
        personality.setQ7(1.0);
        personality.setQ8(5.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(10.0);
        mode.setPt_percent(30.0);
        mode.setCar_percent(51.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);



        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //GetProperties getProperties = new GetProperties();

        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes4.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];

        //Assert.assertThat(res3, CoreMatchers.anyOf( CoreMatchers.is(" \"It s not too far. Take your bike instead of car to  reduce your weekly emissions.\""),CoreMatchers.is(" \"You are near to your destination. It s an opportunity to bike.\"") ) );
        Assert.assertThat(res3, CoreMatchers.anyOf( CoreMatchers.is(" \"Nice weather to use public transport.\""),CoreMatchers.is(" \"Today it s sunny! Take the opportunity to use public transport to save CO2 emissions.\"") ) );

    }
    @Test
    //Test case 14
    public void CalculateMessageTest14() throws Exception{
        GetProperties.setCompEx(0.212);
        GetProperties.setSugEx(0.118);
        GetProperties.setSelfEx(0.145);
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("TooManyCarRoutes");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Extraversion
        Personality personality = new Personality();
        personality.setQ1(1.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(5.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());

        user.setPercentageReduceDriving(60.0);


        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(10.0);
        mode.setPt_percent(10.0);
        mode.setCar_percent(65.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);



        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //GetProperties getProperties = new GetProperties();

        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes4.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];

        Assert.assertThat(res3, CoreMatchers.is(" \"Take public transport. 60.0 % of users have already reduced driving.\"") );

    }

    @Test
    //Test case 15
    public void CalculateMessageTest15() throws Exception{

        GetProperties.setCompAg(0.196);
        GetProperties.setSugAg(0.17);
        GetProperties.setSelfAg(0.2);
        GetProperties.setDuration(18000);
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("Duration");
        contextList.add("emissionsIncreasing");
        contextList.add("TooManyCarRoutes");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://OptimumUser1:Optimum123!@83.212.113.64:32085";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy self-monitoring
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        //Set personality Agreeableness
        Personality personality = new Personality();
        personality.setQ1(5.0);
        personality.setQ2(5.0);
        personality.setQ3(5.0);
        personality.setQ4(5.0);
        personality.setQ5(5.0);
        personality.setQ6(1.0);
        personality.setQ7(1.0);
        personality.setQ8(1.0);
        personality.setQ9(1.0);
        personality.setQ10(1.0);
        user.setPersonality(personality);
        System.out.println(user.getPersonality().getTypeStr());
        //UpdateOperations<User> updateQuery = mongoDatastore.createUpdateOperations(User.class).disableValidation().set("SelfProb", 90.0);
        //mongoDatastore.update(user, updateQuery);
        user.setEmissionsLastWeek(10.0);
        user.setAverageEmissions(8.0);


        ModeUsage mode = new ModeUsage();
        mode.setWalk_percent(10.0);
        mode.setPt_percent(10.0);
        mode.setCar_percent(65.0);
        mode.setBike_percent(9.0);
        user.setMode_usage(mode);


        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        Recommender recommenderRoutes= new Recommender(mapper.readValue(new File("src/main/resources/multiple_routes5.txt"), RouteFormatRoot.class), user);
        recommenderRoutes.filterDuplicates();
        recommenderRoutes.filterRoutesForUser(user);
        recommenderRoutes.rankRoutesForUser(user, mongoDatastore);
        String jsonResult = mapper.writeValueAsString(recommenderRoutes.getRankedRoutesResponse());
        System.out.println(jsonResult);
        String res = jsonResult.split("message")[1];
        String res1 = res.split("mode")[0];
        String res2 = res1.split(",")[0];
        String res3 = res2.split(":")[1];

        Assert.assertThat(res3, CoreMatchers.is(" \"Last week your emissions are increasing. Try more!\"") );
    }



}