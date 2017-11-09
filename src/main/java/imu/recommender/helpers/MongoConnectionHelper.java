package imu.recommender.helpers;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import imu.recommender.RequestHandler;
import org.apache.log4j.Logger;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.net.UnknownHostException;


public class MongoConnectionHelper implements ServletContextListener {
	 private static MongoClient mongoSingleton = null;
	 private static Datastore dsSingleton = null;
	 private Logger logger = Logger.getLogger(RequestHandler.class);
	 
	 private static String host = "83.212.113.64";
	 private static int port = 27017;
	 private static String user = "";
	 private static String pass = "";

	    public static synchronized Datastore getMongoDatastore() 
	    		throws UnknownHostException {

	        if (mongoSingleton == null) {

	            synchronized (MongoConnectionHelper.class) {
	                if (mongoSingleton == null) {
	                	
	                	if (user.matches("")){
	                		mongoSingleton = new MongoClient(host, port);	                		
	                	}else{
	                		String connectionStr = "mongodb://" + user + ":" + pass + "@" + host + ":" + port + "/admin";
	                		mongoSingleton = new MongoClient(	                				
	                  			new MongoClientURI(connectionStr)
	                  		);
	                	}
	                		
	                	
	                    
	                    Morphia morphia = new Morphia();
//	                    morphia.getMapper().getOptions().setObjectFactory(new DefaultCreator() {
//	                        @Override
//	                        protected ClassLoader getClassLoaderForClass() {
//	                        	return Thread.currentThread().getContextClassLoader();
//	                        }
//	                    });
	                    dsSingleton = morphia.createDatastore(mongoSingleton,"Optimum");
	                }
	            }
	        }

	        return dsSingleton;

	    }

		@Override
		public void contextDestroyed(ServletContextEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void contextInitialized(ServletContextEvent servletContextEvent) {
			//test
			try{
	    		ServletContext sc = servletContextEvent.getServletContext();
	    		String mongoURL = sc.getInitParameter("mongoURL");
	    		logger.debug("mongoURL: " + mongoURL);
	    		this.host = mongoURL;
	    		String mongoPort = sc.getInitParameter("mongoPort");
	    		logger.debug("mongoPort: " + mongoPort);
	    		this.port = Integer.parseInt(mongoPort);
	    		String mongoUser = sc.getInitParameter("mongoUser");
	    		logger.debug("mongoUser: " + mongoUser);
	    		this.user = mongoUser;
	    		String mongoPass = sc.getInitParameter("mongoPass");
	    		logger.debug("mongoPass: " + mongoPass);
	    		this.pass = mongoPass;
			}
			catch (Exception e){
				logger.debug(e);
			}
		}

}
