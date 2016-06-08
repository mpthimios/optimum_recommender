package imu.recommender;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.*;

import com.fluidtime.brivel.route.json.response.JsonResponseData;
import com.fluidtime.library.model.json.JsonTrip;
import com.fluidtime.library.model.json.request.RequestTimeRoute;
import com.fluidtime.library.model.json.response.route.JsonResponseRoute;
import com.fluidtime.brivel.route.json.RouteParser;
import com.google.gson.Gson;
import sun.font.TrueTypeFont;

import static imu.recommender.CalculateMessageUtilities.calculate;


public class Recommender extends HttpServlet{
	
	private static boolean PRINT_JSON = true;
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
		
		JsonResponseRoute route = RouteParser.routeFromJson(getBody(request));

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();
	 
	    if (PRINT_JSON){
	    	try {
	    		String routeResponseStr = RouteParser.routeToJson(route);
		    	out.println("thimios");
		    	out.println(routeResponseStr);
		    }
		    finally {

		    }	    	
	    }
	    else{
	    	try {	    		
		    	out.println("error");		    	
		    }
		    finally {

		    }
	    }
	    
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

		JsonResponseRoute route = RouteParser.routeFromJson(getBody(request));
		//JsonResponseRoute route = RouteParser.routeFromJson(data);

		String mes = null;

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();

	 
	    // Write the response message, in an HTML page
	    if (PRINT_JSON){
	    	String routeResponseStr = RouteParser.routeToJson(route);	    	
	    	out.println(routeResponseStr);
	    }
	    else{
	    	try {
		    	out.println("<!DOCTYPE html>");
		        out.println("<html><head>");
		        out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
		        out.println("<title>Echo Servlet</title></head>");
				out.println("<h3>Alternatives Routes from "+route.getLocationFrom().getProperties().getTitle()
						+" to "+route.getLocationTo().getProperties().getTitle()+":</h3>");

				try {
					//filter route
					JsonResponseRoute response_route = filtering(route);
					for (int i = 0; i < response_route.getTrips().size(); i++) {
						JsonTrip trip = response_route.getTrips().get(i);

						if (trip.getModality().equals("car") ){
							mes = "No message";
						}
						else {
							mes = calculate(route, trip);
							response_route.getTrips().get(i).addAttribute("message", mes);

						}

						if (mes != "No message"){
							out.println("<p>"+mes+"</p>");
						}
						if (trip.getModality().equals("pt")) {
							out.println("<p>Choice " + (i + 1) + ":<span style='padding-left:68px;'>" +
									"</span> Transit <span style='padding-left:68px;'></span>"
									+ trip.getDurationMinutes() + "min</p>");
						}
						if (trip.getModality().equals("car")) {
							out.println("<p>Choice " + (i + 1) + ": <span style='padding-left:68px;'>" +
									"</span> Car <span style='padding-left:68px;'></span>"
									+ trip.getDurationMinutes() + "min</p>");
						}
						if (trip.getModality().equals("walk")) {
							out.println("<p>Choice " + (i + 1) + ": <span style='padding-left:68px;'>" +
									"</span> Walk <span style='padding-left:68px;'></span>"
									+ trip.getDurationMinutes() + "min</p>");
						}
						if (trip.getModality().equals("bike")) {
							out.println("<p>Choice " + (i + 1) + ": <span style='padding-left:68px;'>" +
									"</span> Bike <span style='padding-left:68px;'></span>"
									+ trip.getDurationMinutes() + "min</p>");
						}
						if (trip.getModality().equals("par")) {
							out.println("<p>Choice " + (i + 1) + ": <span style='padding-left:68px;'>" +
									"</span>  Par  <span style='padding-left:68px;'></span> "
									+ trip.getDurationMinutes() + "min</p>");
						}
						//out.println("<p>Choice "+(i+1)+": "+mes+"</p>");

					}

				} catch (Exception e) {
					e.printStackTrace();
				}

		    }
		    finally {
		    	
		    }
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

	public JsonResponseRoute filtering(JsonResponseRoute route){
		JsonResponseRoute filtered_route = new JsonResponseRoute();
		filtered_route.setId(route.getId());
		filtered_route.setLocationFrom(route.getLocationFrom());
		filtered_route.setLocationTo(route.getLocationTo());
		filtered_route.addAttribute("city",route.getAttribute("city"));
		filtered_route.addAttribute("userid",route.getAttribute("userid"));
		filtered_route.addAttribute("clientid",route.getAttribute("clientid"));
		List<JsonTrip> Trips = new ArrayList<JsonTrip>();
		for (int i = 0; i < route.getTrips().size(); i++) {
			JsonTrip trip = route.getTrips().get(i);
			Boolean car_owner = Boolean.TRUE;
			Boolean bike_owner = Boolean.FALSE;
			//Filter out routes
			if ( trip.getDistanceMeter()<1000 && trip.getModality().equals("walk")  ||
					trip.getDistanceMeter()<3000 && trip.getModality().equals("bike") ||
					( trip.getModality().equals("car")) || !trip.getModality().equals("bike") )
			{
				JsonTrip new_trip = new JsonTrip();
				new_trip.setSegments(trip.getSegments());
				new_trip.setDescription(trip.getDescription());
				new_trip.setDistanceMeter(trip.getDistanceMeter());
				new_trip.setDurationMinutes(trip.getDurationMinutes());
				new_trip.setModality(trip.getModality());
				new_trip.setTimePlanned(trip.getTimePlanned());
				//new_trip.setAttributes(trip.getAttributes());

				Trips.add(new_trip);
			}

		}
		filtered_route.setTrips(Trips);
		return filtered_route;
	}
}
