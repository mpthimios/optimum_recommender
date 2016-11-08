package imu.recommender.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
/**
 * Created by evangelie on 21/10/2016.
 */
public class GetProperties {
    String username = "";
    String password = "";
    InputStream inputStream;
    private Logger logger = Logger.getLogger(GetProperties.class);

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
}

