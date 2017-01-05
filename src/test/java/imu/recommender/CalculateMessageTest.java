import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import imu.recommender.CalculateMessageUtilities;
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




public class CalculateMessageTest{
    @Test
    //Test case 1
    public void CalculateMessageTest1() throws Exception{
        String target= "walk";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
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
        personality.setTypeStr("Conscientiousness");
        user.setPersonality(personality);
        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        //Assert.assertThat(mes, CoreMatchers.is("Today it s sunny! Take the opportunity to walk to save CO2 emissions._suggestion "));
        Assert.assertThat(mes,CoreMatchers.anyOf(CoreMatchers.is("Today it s sunny! Take the opportunity to walk to save CO2 emissions._suggestion "),CoreMatchers.is("Nice weather for walking._suggestion ")));
    }

    /*@Test
    //Test case 2
    public void CalculateMessageTest2() throws Exception{
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setTypeStr("Extraversion");
        user.setPersonality(personality);
        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("80% of users biked for similar distances._comparison "));
    }*/

    @Test
    //Test case 3
    public void CalculateMessageTest3() throws Exception{
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setTypeStr("Extraversion");
        user.setPersonality(personality);
        user.setBikeUsageComparedToOthers(90.0);
        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("90.0% of users biked when the weather was as good as today!_comparison"));
    }

    @Test
    //Test case 4
    public void CalculateMessageTest4() throws Exception{
        String target= "bicycle";
        List<String> contextList = new ArrayList<String>();
        contextList.add("NiceWeather");
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setTypeStr("Extraversion");
        user.setPersonality(personality);

        user.setBikeUsageComparedToOthers(0);
        user.setMinBiked(5.0);
        user.setEmissionsLastWeek(0);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("Nice weather!When the weather was good you biked 5.0 minutes per day on average._self-monitoring"));
    }

    /*@Test
    //Test case 5
    public void CalculateMessageTest5() throws Exception{
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("TooManyTransportRoutes");
        contextList.add("BikeDistance");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy suggestion
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setTypeStr("Conscientiousness");
        user.setPersonality(personality);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("It s not too far. Take your bike instead of car and reach your weekly goal._suggestion "));
    }
    @Test
    //Test case 6
    public void CalculateMessageTest6() throws Exception{
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("TooManyCarRoutes");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy comparison
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setTypeStr("Extraversion");
        user.setPersonality(personality);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("It s not too far. Take your bike instead of car and reach your weekly goal._suggestion "));
    }

    @Test
    //Test case 7
    public void CalculateMessageTest7() throws Exception{
        String target= "pt";
        List<String> contextList = new ArrayList<String>();
        contextList.add("Duration");
        contextList.add("EmissionComparetoOthers");
        contextList.add("TooManyCarRoutes");

        String userID = "6EEGP034JBLydaotzqZrCs65jRdpfR4s";
        //Connect to our test mongo db
        String connectionStr = "mongodb://83.212.113.64:27017";
        MongoClient mongoSingleton = new MongoClient(new MongoClientURI(connectionStr) );
        Morphia morphia = new Morphia();
        Datastore mongoDatastore = morphia.createDatastore(mongoSingleton, "Optimum");
        User user = (User) mongoDatastore.find(User.class).field("id").equal(userID).get();
        //Set strategy self-monitoring
        user.setSugProb(0.0);
        user.setSelfProb(0.0);
        user.setCompProb(0.0);
        Personality personality = new Personality();
        personality.setTypeStr("Agreeableness");
        user.setPersonality(personality);

        user.setEmissionsLastWeek(0);

        //Find the message
        String mes = CalculateMessageUtilities.calculateForUser( contextList, user, target);
        System.out.println(mes);
        Assert.assertThat(mes, CoreMatchers.is("Last week your emissions are increasing. Try more!_self-monitoring"));
    }*/
}