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
}

