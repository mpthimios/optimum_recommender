package imu.recommender.helpers;

import java.net.UnknownHostException;
import java.util.ResourceBundle;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.mapping.DefaultCreator;


public class MongoConnectionHelper {
	 private static MongoClient mongoSingleton = null;
	 private static Datastore dsSingleton = null;

	    public static synchronized Datastore getMongoDatastore() throws UnknownHostException {

	        if (mongoSingleton == null) {

	            synchronized (MongoConnectionHelper.class) {
	                if (mongoSingleton == null) {

	                    //ResourceBundle bundle = ResourceBundle.getBundle("mongodb");
	                    //String host = bundle.getString("host");
	                	String host = "83.212.113.64";	                	
	                    //int port = Integer.parseInt(bundle.getString("port"));
	                	int port = 27017;
	                	
	                    mongoSingleton = new MongoClient(host, port);
	                    Morphia morphia = new Morphia();
//	                    morphia.getMapper().getOptions().setObjectFactory(new DefaultCreator() {
//	                        @Override
//	                        protected ClassLoader getClassLoaderForClass() {
//	                        	return Thread.currentThread().getContextClassLoader();
//	                        }
//	                    });
	                    dsSingleton = morphia.createDatastore(mongoSingleton, "Optimum");	                    
	                }
	            }
	        }

	        return dsSingleton;

	    }

}
