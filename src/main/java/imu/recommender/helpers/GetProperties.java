package imu.recommender.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
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

            Date time = new Date(System.currentTimeMillis());

            // get the username
            username = prop.getProperty("username");
        } catch (Exception e) {
        	logger.debug("Exception: " + e);
        } finally {
            inputStream.close();
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

            Date time = new Date(System.currentTimeMillis());

            // get the password
            password = prop.getProperty("password");

        } catch (Exception e) {
        	logger.debug("Exception: " + e);
        } finally {
            inputStream.close();
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
		}
		catch (Exception e){
			e.printStackTrace();
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

    public static void setSugCons(double Sugons) {
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

