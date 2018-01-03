package imu.recommender.helpers;

import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
/**
 * Created by evangelie on 21/10/2016.
 */
public class GetProperties implements ServletContextListener {

    String username = "";
    String password = "";
    InputStream inputStream;
    private Logger logger = Logger.getLogger(GetProperties.class);
    
    private static int maxWalkingDistance = 1000;
    private static int maxBikeDistance = 3000;
    private static String weatherId = "";
    private static int Duration = 5;
    private static int days = 7;
    private static double CompEx;
    private static double CompAg;
    private static double CompCons;
    private static double CompN;
    private static double CompOp;
    private static double SelfEx;
    private static double SelfAg;
    private static double SelfCons;
    private static double SelfN;
    private static double SelfOp;
    private static double SugEx;
    private static double SugAg;
    private static double SugCons;
    private static double SugN;
    private static double SugOp;
    private static double PCar;
    private static double PBikeGW;
    private static double PWalkGW;
    private static double PPtGW;
    private static double MinWalked;
    private static double MinBiked;
    private static double MinPT;
    private static double MinDrived;
    private static double MinBikeSharing;
    private static double MinBikeRide;
    private static double MinParkRide;
    private static Integer hours;
    private static Boolean TestGraphs;
    private static String client_id;
    private static String client_secret;

    public static double getPCar() {
        return PCar;
    }

    public static void setPCar(double PCar) {
        GetProperties.PCar = PCar;
    }

    public static double getPBikeGW() {
        return PBikeGW;
    }

    public static void setPBikeGW(double PBikeGW) {
        GetProperties.PBikeGW = PBikeGW;
    }

    public static double getPWalkGW() {
        return PWalkGW;
    }

    public static void setPWalkGW(double PWalkGW) {
        GetProperties.PWalkGW = PWalkGW;
    }

    public static double getPPtGW() {
        return PPtGW;
    }

    public static void setPPtGW(double PPtGW) {
        GetProperties.PPtGW = PPtGW;
    }

    public static double getMinWalked() {
        return MinWalked;
    }

    public static void setMinWalked(double minWalked) {
        MinWalked = minWalked;
    }

    public static double getMinBiked() {
        return MinBiked;
    }

    public static void setMinBiked(double minBiked) {
        MinBiked = minBiked;
    }

    public static double getMinPT() {
        return MinPT;
    }

    public static void setMinPT(double minPT) {
        MinPT = minPT;
    }

    public static double getMinDrived() {
        return MinDrived;
    }

    public static void setMinDrived(double minDrived) {
        MinDrived = minDrived;
    }

    public static double getMinBikeSharing() {
        return MinBikeSharing;
    }

    public static void setMinBikeSharing(double minBikeSharing) {
        MinBikeSharing = minBikeSharing;
    }

    public static double getMinBikeRide() {
        return MinBikeRide;
    }

    public static void setMinBikeRide(double minBikeRide) {
        MinBikeRide = minBikeRide;
    }

    public static double getMinParkRide() {
        return MinParkRide;
    }

    public static void setMinParkRide(double minParkRide) {
        MinParkRide = minParkRide;
    }

    public static Integer getHours() {
        return hours;
    }

    public static void setHours(Integer hours) {
        GetProperties.hours = hours;
    }

    public static Boolean getTestGraphs() {
        return TestGraphs;
    }

    public static void setTestGraphs(Boolean testGraphs) {
        TestGraphs = testGraphs;
    }

    public static String getClient_id() {
        return client_id;
    }

    public static void setClient_id(String client_id) {
        GetProperties.client_id = client_id;
    }

    public static String getClient_secret() {
        return client_secret;
    }

    public static void setClient_secret(String client_secret) {
        GetProperties.client_secret = client_secret;
    }

    public String getUsernameValues() throws IOException {

        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
           
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            // get the username
            username = prop.getProperty("username");
            inputStream.close();
        } catch (Exception e) {
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
        }
        return username;
    }

    public String getPasswordValues() throws IOException {

        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            // get the password
            password = prop.getProperty("password");
            inputStream.close();

        } catch (Exception e) {
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
        }
        return password;
    }

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		try{
    		ServletContext sc = servletContextEvent.getServletContext();
    		String walkingDistance = sc.getInitParameter("maxWalkingDistance");
    		logger.debug("maxWalkingDistance: " + walkingDistance);    		
    		GetProperties.maxWalkingDistance = Integer.parseInt(walkingDistance);
    		
    		String bikeDistance = sc.getInitParameter("maxBikeDistance");
    		logger.debug("maxBikeDistance: " + bikeDistance);
    		GetProperties.maxBikeDistance = Integer.parseInt(bikeDistance);

            String weatherId = sc.getInitParameter("weatherId");
            logger.debug("weatherId: " + weatherId);
            GetProperties.weatherId = String.valueOf (weatherId);

            String duration = sc.getInitParameter("Duration");
            logger.debug("duration: " + duration);
            GetProperties.Duration = Integer.parseInt(duration);

            String days = sc.getInitParameter("days");
            logger.debug("days: " + days);
            GetProperties.days = Integer.parseInt(days);

            String CompEx = sc.getInitParameter("CompEx");
            logger.debug("CompEx: " + CompEx);
            GetProperties.CompEx = Double.parseDouble(CompEx);

            String CompAg = sc.getInitParameter("CompAg");
            logger.debug("CompAg: " + CompAg);
            GetProperties.CompAg = Double.parseDouble(CompAg);

            String CompCons = sc.getInitParameter("CompCons");
            logger.debug("CompCons: " + CompCons);
            GetProperties.CompCons = Double.parseDouble(CompCons);

            String CompN = sc.getInitParameter("CompN");
            logger.debug("CompN: " + CompN);
            GetProperties.CompN = Double.parseDouble(CompN);

            String CompOp = sc.getInitParameter("CompOp");
            logger.debug("CompOp: " + CompOp);
            GetProperties.CompOp = Double.parseDouble(CompOp);

            String SelfEx = sc.getInitParameter("SelfEx");
            logger.debug("SelfEx: " + SelfEx);
            GetProperties.SelfEx = Double.parseDouble(SelfEx);

            String SelfAg = sc.getInitParameter("SelfAg");
            logger.debug("SelfAg: " + SelfAg);
            GetProperties.SelfAg = Double.parseDouble(SelfAg);

            String SelfCons = sc.getInitParameter("SelfCons");
            logger.debug("SelfCons: " + SelfCons);
            GetProperties.SelfCons = Double.parseDouble(SelfCons);

            String SelfN = sc.getInitParameter("SelfN");
            logger.debug("SelfN: " + SelfN);
            GetProperties.SelfN = Double.parseDouble(SelfN);

            String SelfOp = sc.getInitParameter("SelfOp");
            logger.debug("SelfOp: " + SelfOp);
            GetProperties.SelfOp = Double.parseDouble(SelfOp);

            String SugEx = sc.getInitParameter("SugEx");
            logger.debug("SugEx: " + SugEx);
            GetProperties.SelfEx = Double.parseDouble(SelfEx);

            String SugAg = sc.getInitParameter("SugAg");
            logger.debug("SugAg: " + SugAg);
            GetProperties.SugAg = Double.parseDouble(SugAg);

            String SugCons = sc.getInitParameter("SugCons");
            logger.debug("SugCons: " + SugCons);
            GetProperties.SugCons = Double.parseDouble(SugCons);

            String SugN = sc.getInitParameter("SugN");
            logger.debug("SugN: " + SugN);
            GetProperties.SugN = Double.parseDouble(SugN);

            String SugOp = sc.getInitParameter("SugOp");
            logger.debug("SugOp: " + SugOp);
            GetProperties.SugOp = Double.parseDouble(SugOp);

            String PCar = sc.getInitParameter("PCar");
            logger.debug("PCar: " + PCar);
            GetProperties.PCar = Double.parseDouble(PCar);

            String PWalkGW = sc.getInitParameter("PWalkGW");
            logger.debug("PWalkGW: " + PWalkGW);
            GetProperties.PWalkGW = Double.parseDouble(PWalkGW);

            String PBikeGW = sc.getInitParameter("PBikeGW");
            logger.debug("PBikeGW: " + PBikeGW);
            GetProperties.PBikeGW = Double.parseDouble(PBikeGW);

            String PPtGW = sc.getInitParameter("PPtGW");
            logger.debug("PPtGW: " + PPtGW);
            GetProperties.PPtGW = Double.parseDouble(PPtGW);

            String MinWalked = sc.getInitParameter("MinWalked");
            logger.debug("MinWalked: " + MinWalked);
            GetProperties.MinWalked = Double.parseDouble(MinWalked);

            String MinBiked = sc.getInitParameter("MinBiked");
            logger.debug("MinBiked: " + MinBiked);
            GetProperties.MinBiked = Double.parseDouble(MinBiked);

            String MinPT = sc.getInitParameter("MinPT");
            logger.debug("MinPT: " + MinPT);
            GetProperties.MinPT = Double.parseDouble(MinPT);

            String MinDrived = sc.getInitParameter("MinDrived");
            logger.debug("MinDrived: " + MinDrived);
            GetProperties.MinDrived = Double.parseDouble(MinDrived);

            String MinBikeSharing = sc.getInitParameter("MinBikeSharing");
            logger.debug("MinBikeSharing: " + MinBikeSharing);
            GetProperties.MinBikeSharing = Double.parseDouble(MinBikeSharing);

            String MinBikeRide = sc.getInitParameter("MinBikeRide");
            logger.debug("MinBikeRide: " + MinBikeRide);
            GetProperties.MinBikeRide = Double.parseDouble(MinBikeRide);

            String MinParkRide = sc.getInitParameter("MinParkRide");
            logger.debug("MinParkRide: " + MinParkRide);
            GetProperties.MinParkRide = Double.parseDouble(MinParkRide);

            String hours = sc.getInitParameter("hours");
            logger.debug("hours: " + hours);
            GetProperties.hours = Integer.parseInt(hours);

            String TestGraphs = sc.getInitParameter("TestGraphs");
            logger.debug("TestGraphs: " + TestGraphs);
            GetProperties.TestGraphs = Boolean.parseBoolean(hours);

            String client_id = sc.getInitParameter("client_id");
            logger.debug("client_id: " + client_id);
            GetProperties.client_id = String.valueOf (client_id);

            String client_secret = sc.getInitParameter("client_secret");
            logger.debug("client_secret: " + client_secret);
            GetProperties.client_secret = String.valueOf (client_secret);

        }
		catch (Exception e){
            logger.error("Exception while filtering duplicate routes: " + e.getMessage(), e);
		}
		
	}
	
	 public static int getMaxWalkingDistance() {
		return maxWalkingDistance;
	}

	public static void setMaxWalkingDistance(int maxWalkingDistance) {
		GetProperties.maxWalkingDistance = maxWalkingDistance;
	}
	public static int getMaxBikeDistance() {
		return maxBikeDistance;
	}

	public static void setMaxBikeDistance(int maxBikeDistance) {
		GetProperties.maxBikeDistance = maxBikeDistance;
	}

    public static String getweatherId() {
        return weatherId;
    }

    public static void setweatherId(String weatherId) {
        GetProperties.weatherId = weatherId;
    }

    public static int getDuration() {
        return Duration;
    }

    public static void setDuration(int Duration) {
        GetProperties.Duration = Duration;
    }
    public static int getdays() {
        return days;
    }

    public static void setdays(int Days) {
        GetProperties.days = Days;
    }

    public static double getCompEx() {
        return CompEx;
    }

    public static void setCompEx(double CompEx) {
        GetProperties.CompEx = CompEx;
    }

    public static double getCompAg() {
        return CompAg;
    }

    public static void setCompAg(double CompAg) {
        GetProperties.CompAg = CompAg;
    }

    public static double getCompCons() {
        return CompCons;
    }

    public static void setCompCons(double CompCons) {
        GetProperties.CompCons = CompCons;
    }

    public static double getCompN() {
        return CompN;
    }

    public static void setCompN(double CompN) {
        GetProperties.CompN = CompN;
    }

    public static double getCompOp() { return CompOp; }

    public static void setCompOp(double CompOp) {
        GetProperties.CompOp = CompOp;
    }

    public static double getSelfEx() {
        return SelfEx;
    }

    public static void setSelfEx(double SelfEx) {
        GetProperties.SelfEx = SelfEx;
    }

    public static double getSelfAg() {
        return SelfAg;
    }

    public static void setSelfAg(double SelfAg) {
        GetProperties.SelfAg = SelfAg;
    }

    public static double getSelfCons() {
        return SelfCons;
    }

    public static void setSelfCons(double SelfCons) {
        GetProperties.SelfCons = SelfCons;
    }

    public static double getSelfN() {
        return SelfN;
    }

    public static void setSelfN(double SelfN) {
        GetProperties.SelfN = SelfN;
    }

    public static double getSelfOp() { return SelfOp; }

    public static void setSelfOp(double SelfOp) {
        GetProperties.SelfOp = SelfOp;
    }

    public static double getSugEx() {
        return SugEx;
    }

    public static void setSugEx(double SugEx) {
        GetProperties.SugEx = SugEx;
    }

    public static double getSugAg() {
        return SugAg;
    }

    public static void setSugAg(double SugAg) {
        GetProperties.SugAg = SugAg;
    }

    public static double getSugCons() {
        return SugCons;
    }

    public static void setSugCons(double SugCons) {
        GetProperties.SugCons = SugCons;
    }

    public static double getSugN() {
        return SugN;
    }

    public static void setSugN(double SugN) {
        GetProperties.SugN = SugN;
    }

    public static double getSugOp() { return SugOp; }

    public static void setSugOp(double SugOp) {
        GetProperties.SugOp = SugOp;
    }


}

