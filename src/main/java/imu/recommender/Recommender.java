package imu.recommender;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.fluidtime.library.model.json.JsonSegment;
import com.fluidtime.library.model.json.JsonTrip;
import com.fluidtime.library.model.json.FeatureTypes.JsonFeature;
import com.fluidtime.library.model.json.request.RequestGetRoute;
import com.fluidtime.library.model.json.response.route.JsonResponseRoute;
import com.fluidtime.brivel.route.json.AttributeListKeys;
import com.fluidtime.brivel.route.json.RouteParser;

public class Recommender extends HttpServlet{
	public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		
		JsonResponseRoute route = RouteParser.routeFromJson(getBody(request));
		
		
		
		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();
	 
	    // Write the response message, in an HTML page
	    try {
	    	out.println(route.toString());	    
	    }
	    finally {
	    	
	    }
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		
		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();
	 
	    // Write the response message, in an HTML page
	    try {
	    	out.println("<!DOCTYPE html>");
	        out.println("<html><head>");
	        out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
	        out.println("<title>Echo Servlet</title></head>");
	        out.println("<body><h2>You have enter</h2>");
	    }
	    finally {
	    	
	    }
	}
	
	public static String getBody(HttpServletRequest request) throws IOException {

	    String body = null;
	    StringBuilder stringBuilder = new StringBuilder();
	    BufferedReader bufferedReader = null;

	    try {
	        InputStream inputStream = request.getInputStream();
	        if (inputStream != null) {
	            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	            char[] charBuffer = new char[128];
	            int bytesRead = -1;
	            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
	                stringBuilder.append(charBuffer, 0, bytesRead);
	            }
	        } else {
	            stringBuilder.append("");
	        }
	    } catch (IOException ex) {
	        throw ex;
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException ex) {
	                throw ex;
	            }
	        }
	    }

	    body = stringBuilder.toString();
	    return body;
	}
}
