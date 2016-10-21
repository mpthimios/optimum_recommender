package imu.recommender;

import java.io.*;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.swing.text.Segment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import at.ac.ait.ariadne.routeformat.*;
import at.ac.ait.ariadne.routeformat.Sproute.Status;

import at.ac.ait.ariadne.routeformat.geojson.GeoJSONFeature;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONFeatureCollection;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONLineString;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONPolygon;
import at.ac.ait.ariadne.routeformat.location.Address;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;

import at.ac.ait.ariadne.routeformat.RouteFormatRoot;

import java.util.Optional;

import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.sun.xml.internal.ws.message.stream.StreamAttachment;
import javafx.geometry.BoundingBox;
import org.apache.http.impl.client.RoutedRequest;
import org.bitpipeline.lib.owm.WeatherForecastResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import sun.font.TrueTypeFont;

import static com.sun.xml.internal.ws.policy.sourcemodel.wspolicy.XmlToken.Optional;
import static imu.recommender.CalculateMessageUtilities.calculate;
import static java.lang.System.out;

public class Recommender extends HttpServlet{

	private static boolean PRINT_JSON = true;

	public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module());

		RouteFormatRoot routes = mapper.readValue(getBody(request), RouteFormatRoot.class);

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();


	    if (PRINT_JSON){
	    	try {

				calculatePercentages("luka");
				GetProperties properties = new GetProperties();
				System.out.println(properties.getPasswordValues());
				RouteFormatRoot response_route = filtering(routes);
				List<Route> Trips = new ArrayList<Route>();
				for (int i = 0; i < response_route.getRoutes().size(); i++) {
					Route route = response_route.getRoutes().get(i);
					String mes="";
					if (response_route.getRoutes().get(i).getAdditionalInfo().get("mode")=="car"){
						mes =  "";
					}
					else{
						try {
							mes = calculate(response_route, route);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
					additionalInfoRouteRequest.put("mode", response_route.getRoutes().get(i).getAdditionalInfo().get("mode"));
					additionalInfoRouteRequest.put("message", mes);
					double emissions = CalculateEmissions(route);
					additionalInfoRouteRequest.put("emissions", emissions);
					Route r = Route.builder().withFrom(route.getFrom()).withTo(route.getTo()).withOptimizedFor(route.getOptimizedFor().get()).withAdditionalInfo(additionalInfoRouteRequest).withDepartureTime(route.getDepartureTime()).withArrivalTime(route.getArrivalTime()).withDistanceMeters(route.getDistanceMeters()).withDurationSeconds(route.getDurationSeconds()).withSegments(route.getSegments()).build();
					addTrip(r, Trips);

				}
				Integer min=response_route.getRequest().get().getAcceptedDelayMinutes().get();

				RouteFormatRoot final_route = RouteFormatRoot.builder().withRequestId(response_route.getRequestId()).withRouteFormatVersion(response_route.getRouteFormatVersion()).withProcessedTime(response_route.getProcessedTime()).withStatus(response_route.getStatus()).withCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).withRequest(response_route.getRequest().get()).withRoutes(Trips).build();
				String routeResponseStr = final_route.toString();
				String geoJson = mapper.writeValueAsString(final_route);
				out.println(geoJson);
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
		String data = "{\n" +
				"  \"routeFormatVersion\" : \"0.13-SNAPSHOT\",\n" +
				"  \"requestId\" : \"999\",\n" +
				"  \"processedTime\" : \"2016-06-14T13:47:30.955+02:00[Europe/Vienna]\",\n" +
				"  \"status\" : \"OK\",\n" +
				"  \"debugMessage\" : \"Route calculated in 0.002 seconds\",\n" +
				"  \"coordinateReferenceSystem\" : \"EPSG:4326\",\n" +
				"  \"request\" : {\n" +
				"    \"serviceId\" : \"ariadne_webservice_vienna\",\n" +
				"    \"from\" : {\n" +
				"      \"type\" : \"PointOfInterest\",\n" +
				"      \"coordinate\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"Point\",\n" +
				"          \"coordinates\" : [ 16.4265263, 48.2686617 ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"address\" : {\n" +
				"        \"country\" : \"Austria\",\n" +
				"        \"city\" : \"Wien\",\n" +
				"        \"postCode\" : \"1210\",\n" +
				"        \"streetName\" : \"Giefinggasse\",\n" +
				"        \"houseNumber\" : \"2b\"\n" +
				"      },\n" +
				"      \"poiType\" : \"company\",\n" +
				"      \"name\" : \"AIT\"\n" +
				"    },\n" +
				"    \"to\" : {\n" +
				"      \"type\" : \"Location\",\n" +
				"      \"coordinate\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"Point\",\n" +
				"          \"coordinates\" : [ 16.3695, 48.2243 ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"address\" : {\n" +
				"        \"postCode\" : \"1020\",\n" +
				"        \"streetName\" : \"Scholzgasse\",\n" +
				"        \"houseNumber\" : \"1\"\n" +
				"      }\n" +
				"    },\n" +
				"    \"modesOfTransport\" : [ \"FOOT\", \"BICYCLE\", \"MOTORCYCLE\", \"CAR\", \"PUBLIC_TRANSPORT\" ],\n" +
				"    \"excludedPublicTransport\" : [ \"AERIALWAY\", \"AIRPLANE\", \"SHIP\" ],\n" +
				"    \"optimizedFor\" : \"traveltime\",\n" +
				"    \"maximumTransfers\" : 10,\n" +
				"    \"departureTime\" : \"2016-01-01T15:00+01:00\",\n" +
				"    \"acceptedDelayMinutes\" : 30,\n" +
				"    \"maximumPublicTransportRoutes\" : 20,\n" +
				"    \"accessibilityRestrictions\" : [ \"NO_ELEVATOR\" ],\n" +
				"    \"privateVehicleLocations\" : {\n" +
				"      \"CAR\" : [ {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.36329, 48.234077 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"postCode\" : \"1200\",\n" +
				"          \"streetName\" : \"Treustraße\",\n" +
				"          \"houseNumber\" : \"92\"\n" +
				"        }\n" +
				"      } ],\n" +
				"      \"BICYCLE\" : [ {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3655, 48.23752 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Adalbert-Stifter-Straße\",\n" +
				"          \"houseNumber\" : \"15\"\n" +
				"        }\n" +
				"      }, {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3715916, 48.246609 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"postCode\" : \"1200\",\n" +
				"          \"streetName\" : \"Hopsagasse\",\n" +
				"          \"houseNumber\" : \"5\"\n" +
				"        }\n" +
				"      } ]\n" +
				"    },\n" +
				"    \"language\" : \"DE\",\n" +
				"    \"additionalInfo\" : {\n" +
				"      \"ait:additionalTestString\" : \"hello this is a String\",\n" +
				"      \"ait:additionalTestBigDecimal\" : 12.34567,\n" +
				"      \"include_operators\" : \"flinc;car2go_vienna;citybike_vienna\",\n" +
				"      \"ait:additionalTestList\" : [ 1, 2, 3, 4, 5, 6, 7 ],\n" +
				"      \"ait:additionalTestObject\" : {\n" +
				"        \"name\" : \"Wiener Linien\",\n" +
				"        \"website\" : \"http://www.wienerlinien.at\",\n" +
				"        \"customerServiceEmail\" : \"post@wienerlinien.at\",\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"email_ticketshop\" : \"ticketshop@wienerlinien.at\"\n" +
				"        }\n" +
				"      }\n" +
				"    }\n" +
				"  },\n" +
				"  \"routes\" : [ {\n" +
				"    \"from\" : {\n" +
				"      \"type\" : \"PointOfInterest\",\n" +
				"      \"coordinate\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"Point\",\n" +
				"          \"coordinates\" : [ 16.4265263, 48.2686617 ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"address\" : {\n" +
				"        \"country\" : \"Austria\",\n" +
				"        \"city\" : \"Wien\",\n" +
				"        \"postCode\" : \"1210\",\n" +
				"        \"streetName\" : \"Giefinggasse\",\n" +
				"        \"houseNumber\" : \"2b\"\n" +
				"      },\n" +
				"      \"poiType\" : \"company\",\n" +
				"      \"name\" : \"AIT\"\n" +
				"    },\n" +
				"    \"to\" : {\n" +
				"      \"type\" : \"Location\",\n" +
				"      \"coordinate\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"Point\",\n" +
				"          \"coordinates\" : [ 16.3695, 48.2243 ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"address\" : {\n" +
				"        \"postCode\" : \"1020\",\n" +
				"        \"streetName\" : \"Scholzgasse\",\n" +
				"        \"houseNumber\" : \"1\"\n" +
				"      }\n" +
				"    },\n" +
				"    \"distanceMeters\" : 8208,\n" +
				"    \"durationSeconds\" : 3520,\n" +
				"    \"departureTime\" : \"2016-01-01T15:00+01:00\",\n" +
				"    \"arrivalTime\" : \"2016-01-01T15:58:40+01:00\",\n" +
				"    \"optimizedFor\" : \"travel time and low costs\",\n" +
				"    \"boundingBox\" : {\n" +
				"      \"type\" : \"Feature\",\n" +
				"      \"crs\" : {\n" +
				"        \"type\" : \"name\",\n" +
				"        \"properties\" : {\n" +
				"          \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"geometry\" : {\n" +
				"        \"type\" : \"Polygon\",\n" +
				"        \"coordinates\" : [ [ [ 16.36288, 48.2243 ], [ 16.36288, 48.2686617 ], [ 16.42824, 48.2686617 ], [ 16.42824, 48.2243 ], [ 16.36288, 48.2243 ] ] ]\n" +
				"      },\n" +
				"      \"properties\" : { }\n" +
				"    },\n" +
				"    \"segments\" : [ {\n" +
				"      \"nr\" : 1,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PointOfInterest\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.4265263, 48.2686617 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"country\" : \"Austria\",\n" +
				"          \"city\" : \"Wien\",\n" +
				"          \"postCode\" : \"1210\",\n" +
				"          \"streetName\" : \"Giefinggasse\",\n" +
				"          \"houseNumber\" : \"2b\"\n" +
				"        },\n" +
				"        \"poiType\" : \"company\",\n" +
				"        \"name\" : \"AIT\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.42791, 48.26680 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Heinrich-von-Buol-Gasse/Siemensstraße\"\n" +
				"      },\n" +
				"      \"distanceMeters\" : 200,\n" +
				"      \"durationSeconds\" : 60,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"FOOT\",\n" +
				"        \"detailedType\" : \"FOOT\"\n" +
				"      },\n" +
				"      \"departureTime\" : \"2016-01-01T15:00+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:01+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.4265263, 48.2686617 ], [ 16.4263, 48.2682 ], [ 16.42824, 48.26719 ], [ 16.42791, 48.26680 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"geometryGeoJsonEdges\" : {\n" +
				"        \"type\" : \"FeatureCollection\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"features\" : [ {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"LineString\",\n" +
				"            \"coordinates\" : [ [ 16.4265263, 48.2686617 ], [ 16.4263, 48.2682 ] ]\n" +
				"          },\n" +
				"          \"properties\" : {\n" +
				"            \"frc\" : \"6\",\n" +
				"            \"name\" : \"Giefinggasse\",\n" +
				"            \"edgeWeight\" : \"54.1\"\n" +
				"          }\n" +
				"        }, {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"LineString\",\n" +
				"            \"coordinates\" : [ [ 16.4263, 48.2682 ], [ 16.42824, 48.26719 ] ]\n" +
				"          },\n" +
				"          \"properties\" : {\n" +
				"            \"frc\" : \"2\",\n" +
				"            \"name\" : \"Siemensstraße\",\n" +
				"            \"edgeWeight\" : \"182.5\"\n" +
				"          }\n" +
				"        }, {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"LineString\",\n" +
				"            \"coordinates\" : [ [ 16.42824, 48.26719 ], [ 16.42791, 48.26680 ] ]\n" +
				"          },\n" +
				"          \"properties\" : {\n" +
				"            \"frc\" : \"3\",\n" +
				"            \"name\" : \"Heinrich-von-Buol-Gasse\",\n" +
				"            \"edgeWeight\" : \"49.8\"\n" +
				"          }\n" +
				"        } ]\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 2,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.42791, 48.26680 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Heinrich-von-Buol-Gasse/Siemensstraße\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.42791, 48.26680 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Heinrich-von-Buol-Gasse/Siemensstraße\"\n" +
				"      },\n" +
				"      \"distanceMeters\" : 0,\n" +
				"      \"durationSeconds\" : 300,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"FOOT\",\n" +
				"        \"detailedType\" : \"TRANSFER\"\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 300,\n" +
				"      \"departureTime\" : \"2016-01-01T15:01+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:06+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.42791, 48.26680 ], [ 16.42791, 48.26680 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 3,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.42791, 48.26680 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Heinrich-von-Buol-Gasse/Siemensstraße\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.40073, 48.25625 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Floridsdorf\",\n" +
				"        \"platform\" : \"C\"\n" +
				"      },\n" +
				"      \"distanceMeters\" : 2500,\n" +
				"      \"durationSeconds\" : 630,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"PUBLIC_TRANSPORT\",\n" +
				"        \"detailedType\" : \"BUS\",\n" +
				"        \"service\" : {\n" +
				"          \"name\" : \"28A\",\n" +
				"          \"towards\" : \"Floridsdorf\"\n" +
				"        },\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Wiener Linien\",\n" +
				"          \"website\" : \"http://www.wienerlinien.at\",\n" +
				"          \"customerServiceEmail\" : \"post@wienerlinien.at\",\n" +
				"          \"additionalInfo\" : {\n" +
				"            \"email_ticketshop\" : \"ticketshop@wienerlinien.at\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"accessibility\" : [ \"HIGH_FLOOR_VEHICLE\" ]\n" +
				"      },\n" +
				"      \"departureTime\" : \"2016-01-01T15:06+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:16:30+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.42791, 48.26680 ], [ 16.42354, 48.26306 ], [ 16.4236, 48.2621 ], [ 16.4044, 48.2576 ], [ 16.40305, 48.25621 ], [ 16.40127, 48.25698 ], [ 16.40073, 48.25625 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 4,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.40073, 48.25625 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Floridsdorf\",\n" +
				"        \"platform\" : \"C\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.40050, 48.25618 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Floridsdorf\",\n" +
				"        \"platform\" : \"2 (U-Bahn)\"\n" +
				"      },\n" +
				"      \"distanceMeters\" : 40,\n" +
				"      \"durationSeconds\" : 240,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"FOOT\",\n" +
				"        \"detailedType\" : \"TRANSFER\"\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 180,\n" +
				"      \"departureTime\" : \"2016-01-01T15:16:30+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:20:30+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.40073, 48.25625 ], [ 16.40050, 48.25618 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"accessibility\" : [ \"STAIRS_DOWN\" ]\n" +
				"    }, {\n" +
				"      \"nr\" : 5,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.40050, 48.25618 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Floridsdorf\",\n" +
				"        \"platform\" : \"2 (U-Bahn)\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.38541, 48.24173 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Handelskai\",\n" +
				"        \"platform\" : \"2\"\n" +
				"      },\n" +
				"      \"distanceMeters\" : 2000,\n" +
				"      \"durationSeconds\" : 240,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"PUBLIC_TRANSPORT\",\n" +
				"        \"detailedType\" : \"SUBWAY\",\n" +
				"        \"service\" : {\n" +
				"          \"name\" : \"U6\",\n" +
				"          \"towards\" : \"Siebenhirten\",\n" +
				"          \"color\" : \"#bf7700\"\n" +
				"        },\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Wiener Linien\",\n" +
				"          \"website\" : \"http://www.wienerlinien.at\",\n" +
				"          \"customerServiceEmail\" : \"post@wienerlinien.at\",\n" +
				"          \"additionalInfo\" : {\n" +
				"            \"email_ticketshop\" : \"ticketshop@wienerlinien.at\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"accessibility\" : [ \"LOW_FLOOR_VEHICLE\" ]\n" +
				"      },\n" +
				"      \"departureTime\" : \"2016-01-01T15:20:30+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:24:30+01:00\",\n" +
				"      \"intermediateStops\" : [ {\n" +
				"        \"stop\" : {\n" +
				"          \"type\" : \"PublicTransportStop\",\n" +
				"          \"coordinate\" : {\n" +
				"            \"type\" : \"Feature\",\n" +
				"            \"crs\" : {\n" +
				"              \"type\" : \"name\",\n" +
				"              \"properties\" : {\n" +
				"                \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"              }\n" +
				"            },\n" +
				"            \"geometry\" : {\n" +
				"              \"type\" : \"Point\",\n" +
				"              \"coordinates\" : [ 16.39468, 48.24630 ]\n" +
				"            },\n" +
				"            \"properties\" : { }\n" +
				"          },\n" +
				"          \"name\" : \"Neue Donau\",\n" +
				"          \"platform\" : \"2\",\n" +
				"          \"relatedLines\" : {\n" +
				"            \"20A\" : \"BUS\",\n" +
				"            \"20B\" : \"BUS\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"plannedArrivalTime\" : \"2016-01-01T15:22:30+01:00\",\n" +
				"        \"plannedDepartureTime\" : \"2016-01-01T15:23:30+01:00\",\n" +
				"        \"estimatedArrivalTime\" : \"2016-01-01T15:22:30+01:00\",\n" +
				"        \"estimatedDepartureTime\" : \"2016-01-01T15:23:30+01:00\"\n" +
				"      } ],\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.40050, 48.25618 ], [ 16.39468, 48.24630 ], [ 16.38541, 48.24173 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 6,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.38541, 48.24173 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Handelskai\",\n" +
				"        \"platform\" : \"2\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3848877, 48.2416471 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Handelskai (Stationseingang)\"\n" +
				"      },\n" +
				"      \"distanceMeters\" : 40,\n" +
				"      \"durationSeconds\" : 180,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"FOOT\",\n" +
				"        \"detailedType\" : \"TRANSFER\"\n" +
				"      },\n" +
				"      \"departureTime\" : \"2016-01-01T15:24:30+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:27:30+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.38541, 48.24173 ], [ 16.3848877, 48.2416471 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"accessibility\" : [ \"ESCALATOR_DOWN\", \"STAIRS_DOWN\" ]\n" +
				"    }, {\n" +
				"      \"nr\" : 7,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"PublicTransportStop\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3848877, 48.2416471 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"name\" : \"Handelskai (Stationseingang)\"\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"SharingStation\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3847976, 48.2420356 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"capacity\" : \"35\",\n" +
				"          \"bikes_available\" : \"10\",\n" +
				"          \"boxes_available\" : \"25\"\n" +
				"        },\n" +
				"        \"name\" : \"Millennium Tower\",\n" +
				"        \"id\" : \"2005\",\n" +
				"        \"modesOfTransport\" : [ \"BICYCLE\" ],\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Citybike Wien\",\n" +
				"          \"address\" : {\n" +
				"            \"city\" : \"Wien\",\n" +
				"            \"postCode\" : \"1030\",\n" +
				"            \"streetName\" : \"Litfaßstraße\",\n" +
				"            \"houseNumber\" : \"6\"\n" +
				"          },\n" +
				"          \"website\" : \"http://citybikewien.at\",\n" +
				"          \"customerServiceEmail\" : \"kontakt@citybikewien.at\",\n" +
				"          \"customerServicePhone\" : \"0810 500 500\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 57,\n" +
				"      \"durationSeconds\" : 40,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"FOOT\",\n" +
				"        \"detailedType\" : \"FOOT\"\n" +
				"      },\n" +
				"      \"departureTime\" : \"2016-01-01T15:27:30+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:28:10+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.3848877, 48.2416471 ], [ 16.3845846, 48.2418792 ], [ 16.3847976, 48.2420356 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 8,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"SharingStation\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3847976, 48.2420356 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"capacity\" : \"35\",\n" +
				"          \"bikes_available\" : \"10\",\n" +
				"          \"boxes_available\" : \"25\"\n" +
				"        },\n" +
				"        \"name\" : \"Millennium Tower\",\n" +
				"        \"id\" : \"2005\",\n" +
				"        \"modesOfTransport\" : [ \"BICYCLE\" ],\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Citybike Wien\",\n" +
				"          \"address\" : {\n" +
				"            \"city\" : \"Wien\",\n" +
				"            \"postCode\" : \"1030\",\n" +
				"            \"streetName\" : \"Litfaßstraße\",\n" +
				"            \"houseNumber\" : \"6\"\n" +
				"          },\n" +
				"          \"website\" : \"http://citybikewien.at\",\n" +
				"          \"customerServiceEmail\" : \"kontakt@citybikewien.at\",\n" +
				"          \"customerServicePhone\" : \"0810 500 500\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"SharingStation\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3792033, 48.2441354 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"capacity\" : \"27\",\n" +
				"          \"bikes_available\" : \"27\",\n" +
				"          \"boxes_available\" : \"0\"\n" +
				"        },\n" +
				"        \"name\" : \"Friedrich Engels Platz\",\n" +
				"        \"id\" : \"2006\",\n" +
				"        \"modesOfTransport\" : [ \"BICYCLE\" ],\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Citybike Wien\",\n" +
				"          \"address\" : {\n" +
				"            \"city\" : \"Wien\",\n" +
				"            \"postCode\" : \"1030\",\n" +
				"            \"streetName\" : \"Litfaßstraße\",\n" +
				"            \"houseNumber\" : \"6\"\n" +
				"          },\n" +
				"          \"website\" : \"http://citybikewien.at\",\n" +
				"          \"customerServiceEmail\" : \"kontakt@citybikewien.at\",\n" +
				"          \"customerServicePhone\" : \"0810 500 500\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 692,\n" +
				"      \"durationSeconds\" : 360,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"BICYCLE\",\n" +
				"        \"detailedType\" : \"BICYCLE\",\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Citybike Wien\",\n" +
				"          \"address\" : {\n" +
				"            \"city\" : \"Wien\",\n" +
				"            \"postCode\" : \"1030\",\n" +
				"            \"streetName\" : \"Litfaßstraße\",\n" +
				"            \"houseNumber\" : \"6\"\n" +
				"          },\n" +
				"          \"website\" : \"http://citybikewien.at\",\n" +
				"          \"customerServiceEmail\" : \"kontakt@citybikewien.at\",\n" +
				"          \"customerServicePhone\" : \"0810 500 500\"\n" +
				"        },\n" +
				"        \"sharingType\" : \"STATION_BOUND_VEHICLE_SHARING\"\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 120,\n" +
				"      \"alightingSeconds\" : 60,\n" +
				"      \"departureTime\" : \"2016-01-01T15:28:10+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:34:10+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.3847976, 48.2420356 ], [ 16.3838145, 48.2413853 ], [ 16.3807639, 48.2442201 ], [ 16.3793906, 48.2438237 ], [ 16.3792033, 48.2441354 ] ]\n" +
				"        },\n" +
				"        \"properties\" : {\n" +
				"          \"color\" : \"#FFBBCC\",\n" +
				"          \"weight\" : \"7\",\n" +
				"          \"opacity\" : \"0.9\"\n" +
				"        }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 9,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"SharingStation\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3792033, 48.2441354 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"capacity\" : \"27\",\n" +
				"          \"bikes_available\" : \"27\",\n" +
				"          \"boxes_available\" : \"0\"\n" +
				"        },\n" +
				"        \"name\" : \"Friedrich Engels Platz\",\n" +
				"        \"id\" : \"2006\",\n" +
				"        \"modesOfTransport\" : [ \"BICYCLE\" ],\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Citybike Wien\",\n" +
				"          \"address\" : {\n" +
				"            \"city\" : \"Wien\",\n" +
				"            \"postCode\" : \"1030\",\n" +
				"            \"streetName\" : \"Litfaßstraße\",\n" +
				"            \"houseNumber\" : \"6\"\n" +
				"          },\n" +
				"          \"website\" : \"http://citybikewien.at\",\n" +
				"          \"customerServiceEmail\" : \"kontakt@citybikewien.at\",\n" +
				"          \"customerServicePhone\" : \"0810 500 500\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.377454, 48.24386 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Adalbert-Stifter-Straße\",\n" +
				"          \"houseNumber\" : \"71\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 150,\n" +
				"      \"durationSeconds\" : 115,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"FOOT\",\n" +
				"        \"detailedType\" : \"FOOT\"\n" +
				"      },\n" +
				"      \"departureTime\" : \"2016-01-01T15:34:10+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:36:05+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.3792033, 48.2441354 ], [ 16.37763, 48.24369 ], [ 16.377454, 48.24386 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"accessibility\" : [ \"STAIRS_UP\" ]\n" +
				"    }, {\n" +
				"      \"nr\" : 10,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.377454, 48.24386 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Adalbert-Stifter-Straße\",\n" +
				"          \"houseNumber\" : \"71\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3655, 48.23752 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Adalbert-Stifter-Straße\",\n" +
				"          \"houseNumber\" : \"15\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 692,\n" +
				"      \"durationSeconds\" : 420,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"CAR\",\n" +
				"        \"detailedType\" : \"CAR\",\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Car2Go\",\n" +
				"          \"website\" : \"https://www.car2go.com/de/wien\",\n" +
				"          \"customerServiceEmail\" : \"wien@car2go.com\"\n" +
				"        },\n" +
				"        \"electric\" : true,\n" +
				"        \"sharingType\" : \"FREE_FLOATING_VEHICLE_SHARING\",\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"licensePlate\" : \"W-123456\",\n" +
				"          \"fuelPercentage\" : \"80\",\n" +
				"          \"interiorState\" : \"good\",\n" +
				"          \"exteriorState\" : \"unacceptable\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 120,\n" +
				"      \"alightingSeconds\" : 60,\n" +
				"      \"departureTime\" : \"2016-01-01T15:36:05+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:43:05+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.377454, 48.24386 ], [ 16.373601, 48.24218 ], [ 16.3655, 48.23752 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 11,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3655, 48.23752 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Adalbert-Stifter-Straße\",\n" +
				"          \"houseNumber\" : \"15\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.36329, 48.234077 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"postCode\" : \"1200\",\n" +
				"          \"streetName\" : \"Treustraße\",\n" +
				"          \"houseNumber\" : \"92\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 597,\n" +
				"      \"durationSeconds\" : 226,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"BICYCLE\",\n" +
				"        \"detailedType\" : \"BICYCLE\"\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 60,\n" +
				"      \"alightingSeconds\" : 60,\n" +
				"      \"departureTime\" : \"2016-01-01T15:43:05+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:46:51+01:00\",\n" +
				"      \"intermediateStops\" : [ {\n" +
				"        \"stop\" : {\n" +
				"          \"type\" : \"PointOfInterest\",\n" +
				"          \"coordinate\" : {\n" +
				"            \"type\" : \"Feature\",\n" +
				"            \"crs\" : {\n" +
				"              \"type\" : \"name\",\n" +
				"              \"properties\" : {\n" +
				"                \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"              }\n" +
				"            },\n" +
				"            \"geometry\" : {\n" +
				"              \"type\" : \"Point\",\n" +
				"              \"coordinates\" : [ 16.364074, 48.2350109 ]\n" +
				"            },\n" +
				"            \"properties\" : { }\n" +
				"          },\n" +
				"          \"poiType\" : \"park\",\n" +
				"          \"name\" : \"Anton-Kummerer-Park\"\n" +
				"        }\n" +
				"      } ],\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.3655, 48.23752 ], [ 16.36515, 48.23729 ], [ 16.3656, 48.23515 ], [ 16.36288, 48.23509 ], [ 16.36329, 48.234077 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      },\n" +
				"      \"additionalInfo\" : {\n" +
				"        \"name\" : \"Univega Mountainbike\"\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 12,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.36329, 48.234077 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"postCode\" : \"1200\",\n" +
				"          \"streetName\" : \"Treustraße\",\n" +
				"          \"houseNumber\" : \"92\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.369045, 48.2267 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Gaußplatz\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 941,\n" +
				"      \"durationSeconds\" : 292,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"CAR\",\n" +
				"        \"detailedType\" : \"CAR\",\n" +
				"        \"operator\" : {\n" +
				"          \"name\" : \"Flinc\",\n" +
				"          \"website\" : \"https://flinc.org\"\n" +
				"        },\n" +
				"        \"sharingType\" : \"RIDE_SHARING\",\n" +
				"        \"additionalInfo\" : {\n" +
				"          \"userName\" : \"herbertWien78\",\n" +
				"          \"phoneNumber\" : \"+43 650 7734343\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 180,\n" +
				"      \"departureTime\" : \"2016-01-01T15:46:51+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:51:43+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.36329, 48.234077 ], [ 16.3644, 48.2311 ], [ 16.36638, 48.22886 ], [ 16.369045, 48.2267 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    }, {\n" +
				"      \"nr\" : 13,\n" +
				"      \"from\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.369045, 48.2267 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"streetName\" : \"Gaußplatz\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"to\" : {\n" +
				"        \"type\" : \"Location\",\n" +
				"        \"coordinate\" : {\n" +
				"          \"type\" : \"Feature\",\n" +
				"          \"crs\" : {\n" +
				"            \"type\" : \"name\",\n" +
				"            \"properties\" : {\n" +
				"              \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"            }\n" +
				"          },\n" +
				"          \"geometry\" : {\n" +
				"            \"type\" : \"Point\",\n" +
				"            \"coordinates\" : [ 16.3695, 48.2243 ]\n" +
				"          },\n" +
				"          \"properties\" : { }\n" +
				"        },\n" +
				"        \"address\" : {\n" +
				"          \"postCode\" : \"1020\",\n" +
				"          \"streetName\" : \"Scholzgasse\",\n" +
				"          \"houseNumber\" : \"1\"\n" +
				"        }\n" +
				"      },\n" +
				"      \"distanceMeters\" : 299,\n" +
				"      \"durationSeconds\" : 417,\n" +
				"      \"modeOfTransport\" : {\n" +
				"        \"generalizedType\" : \"CAR\",\n" +
				"        \"detailedType\" : \"CAR\"\n" +
				"      },\n" +
				"      \"boardingSeconds\" : 60,\n" +
				"      \"alightingSeconds\" : 300,\n" +
				"      \"departureTime\" : \"2016-01-01T15:51:43+01:00\",\n" +
				"      \"arrivalTime\" : \"2016-01-01T15:58:40+01:00\",\n" +
				"      \"geometryGeoJson\" : {\n" +
				"        \"type\" : \"Feature\",\n" +
				"        \"crs\" : {\n" +
				"          \"type\" : \"name\",\n" +
				"          \"properties\" : {\n" +
				"            \"name\" : \"urn:ogc:def:crs:OGC:1.3:CRS84\"\n" +
				"          }\n" +
				"        },\n" +
				"        \"geometry\" : {\n" +
				"          \"type\" : \"LineString\",\n" +
				"          \"coordinates\" : [ [ 16.369045, 48.2267 ], [ 16.3688, 48.2263 ], [ 16.3693, 48.2257 ], [ 16.3697, 48.2256 ], [ 16.3695, 48.2243 ] ]\n" +
				"        },\n" +
				"        \"properties\" : { }\n" +
				"      }\n" +
				"    } ]\n" +
				"  } ]\n" +
				"}";
		//JsonResponseRoute route = RouteParser.routeFromJson(getBody(request));
		//JsonResponseRoute route = RouteParser.routeFromJson(data);
		ObjectMapper mapper = new ObjectMapper();
		//mapper.registerModule(new Jdk8Module());
		/*ObjectMapper mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);*/

		RouteFormatRoot routes = mapper.readValue(data, RouteFormatRoot.class);

		String mes = null;

		// Set the response message's MIME type
	    response.setContentType("text/html; charset=UTF-8");
	    // Allocate a output writer to write the response message into the network socket
	    PrintWriter out = response.getWriter();
		boolean PRINT_JSON = false;


	    // Write the response message, in an HTML page
	    if (PRINT_JSON){
	    	//String routeResponseStr = RouteParser.routeToJson(route);
	    	//out.println(routeResponseStr);
	    }
	    else{
	    	try {
		    	out.println("<!DOCTYPE html>");
		        out.println("<html><head>");
		        out.println("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
		        out.println("<title>Echo Servlet</title></head>");
				Route route1 = routes.getRoutes().get(0);
				Optional<Address> address =route1.getFrom().getAddress();
				if (address.isPresent()){
					java.util.Optional<String> city = address.get().getCity();
					address.get().getStreetName();
				}
				Optional<Address> address_dest=route1.getTo().getAddress();

				if (address.isPresent() && address_dest.isPresent()) {
					out.println("<h3>Alternatives Routes from " + address.get().getStreetName().get() + " to " + address_dest.get().getStreetName().get() + "					:</h3>");
				}
				try {
					//filter route
					calculatePercentages("luka");
					RouteFormatRoot response_route = filtering(routes);
					List<Route> Trips = new ArrayList<Route>();
					for (int i = 0; i < response_route.getRoutes().size(); i++) {
						Route route = response_route.getRoutes().get(i);
						if (response_route.getRoutes().get(i).getAdditionalInfo().get("mode")=="car"){
							mes =  "";
						}
						else{
							mes = calculate(response_route, route);
						}
						Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
						additionalInfoRouteRequest.put("mode", response_route.getRoutes().get(i).getAdditionalInfo().get("mode"));
						additionalInfoRouteRequest.put("message", mes);
						double emissions = CalculateEmissions(route);
						additionalInfoRouteRequest.put("emissions", emissions);
						Route r = Route.builder().withFrom(route.getFrom()).withTo(route.getTo()).withOptimizedFor(route.getOptimizedFor().get()).withAdditionalInfo(additionalInfoRouteRequest).withDepartureTime(route.getDepartureTime()).withArrivalTime(route.getArrivalTime()).withDistanceMeters(route.getDistanceMeters()).withDurationSeconds(route.getDurationSeconds()).withSegments(route.getSegments()).build();
						addTrip(r, Trips);
						out.println("<p>"+r.getAdditionalInfo().get("message")+"</p>");
						out.println("<p>"+r.getAdditionalInfo().get("emissions")+"</p>");
						out.println("<p>Choice " + (i + 1) + ":<span style='padding-left:68px;'>" +
								"</span>"+ r.getAdditionalInfo().get("mode")+"<span style='padding-left:68px;'></span>"
									+ r.getDurationSeconds() + "sec</p>");

					}
					Integer min=response_route.getRequest().get().getAcceptedDelayMinutes().get();
					//RoutingRequest request = RoutingRequest.builder().withAcceptedDelayMinutes(min).build();

					RouteFormatRoot final_route = RouteFormatRoot.builder().withRequestId(response_route.getRequestId()).withRouteFormatVersion(response_route.getRouteFormatVersion()).withProcessedTime(response_route.getProcessedTime()).withStatus(response_route.getStatus()).withCoordinateReferenceSystem(response_route.getCoordinateReferenceSystem()).withRequest(response_route.getRequest().get()).withRoutes(Trips).build();

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
	//Filtering function
	public RouteFormatRoot filtering(RouteFormatRoot routes){
		List<Route> Trips = new ArrayList<Route>();

		for (int i = 0; i < routes.getRoutes().size(); i++) {
			Route trip = routes.getRoutes().get(i);
			boolean car_owner = false;
			boolean bike_owner = true;
			System.out.println(trip.getFrom());
			trip.getFrom().getCoordinate().geometry.coordinates.get(0);
			//Find the mode of the route searching segments of the route
			List<String> Modes = new ArrayList<String>();
			for (int j=0; j< trip.getSegments().size(); j++) {
				RouteSegment segment = trip.getSegments().get(j);
				String mode = segment.getModeOfTransport().getGeneralizedType().toString();
				if (!Modes.contains(mode)) {
					Modes.add(mode);
				}
			}
			String mode="";
			System.out.println(Modes);
			if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.size()==2){
				mode="pt";
			}
			else if (Modes.contains("CAR") && Modes.contains("FOOT") && Modes.size()==2){
				mode="car";
			}
			else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") && Modes.size()==3){
				mode="park&ride";
			}
			else if (Modes.contains("BICYCLE") && Modes.contains("FOOT") && Modes.contains("PUBLIC_TRANSPORT") && Modes.size()==3){
				mode="bike&ride";
			}
			else if (Modes.contains("FOOT") && Modes.size()==1 ){
				mode="walk";
			}
			else if (Modes.contains("BICYCLE") && Modes.size()==1 ){
				mode="walk";
			}
			else if (Modes.contains("CAR") && Modes.size()==1 ){
				mode="car";
			}
			else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.size()==1 ){
				mode="pt";
			}
			else if (Modes.contains("PUBLIC_TRANSPORT") && Modes.contains("FOOT") && Modes.contains("CAR") && Modes.contains("BICYCLE") && Modes.size()==4 ){
					mode="park&ride_with_bike";
			}
			else {
				mode="unknown";
			}
			//System.out.println(Trips.get(0).getAdditionalInfo().get("mode"));
			//Filter out routes
			//Filter out car and park and ride modes for users that don’t own a car.
			if(!car_owner) {
				if (mode.equals("car") || mode.equals("park&ride")) {
					continue;
				} else {
					addTrip(trip, Trips, mode);
				}
			}
			//Filter out bike modes for users that don’t own a bike and for routes containing biking more than 3 Km
			else if (mode.equals("bike")){
				if((bike_owner) && (trip.getDistanceMeters()<3000)){
					addTrip(trip,Trips, mode);
				}
				else{
					continue;
				}
			}
			//Filter out walk modes for routes containing walking more than 1 Km
			else if(mode.equals("walk")){
				if(trip.getDistanceMeters()<1000){
					addTrip(trip,Trips,mode);
				}
				else{
					continue;
				}
			}
			else {
				addTrip(trip,Trips);
			}


		}

		Integer min=routes.getRequest().get().getAcceptedDelayMinutes().get();
		//RoutingRequest request = RoutingRequest.builder().withAcceptedDelayMinutes(min).build();

		RouteFormatRoot filtered_route = RouteFormatRoot.builder().withRequestId(routes.getRequestId()).withRouteFormatVersion(routes.getRouteFormatVersion()).withProcessedTime(routes.getProcessedTime()).withStatus(routes.getStatus()).withCoordinateReferenceSystem(routes.getCoordinateReferenceSystem()).withRequest(routes.getRequest().get()).withRoutes(Trips).build();
		//.withOptimizedFor(trip.getOptimizedFor().toString())
		return filtered_route;

	}

	public void addTrip(Route trip, List<Route> Trips, String mode) {
		Map<String, Object> additionalInfoRouteRequest = new HashMap<>();
		additionalInfoRouteRequest.put("mode", mode);
		Trips.add(Route.builder().withAdditionalInfo(additionalInfoRouteRequest).withFrom(trip.getFrom()).withOptimizedFor(trip.getOptimizedFor().get()).withTo(trip.getTo()).withDistanceMeters(trip.getDistanceMeters()).withDurationSeconds(trip.getDurationSeconds()).withDepartureTime(trip.getDepartureTime()).withArrivalTime(trip.getArrivalTime()).withSegments(trip.getSegments()).build());
	}

	public void addTrip(Route trip, List<Route> Trips) {
		Trips.add(Route.builder().withAdditionalInfo(trip.getAdditionalInfo()).withFrom(trip.getFrom()).withOptimizedFor(trip.getOptimizedFor().get()).withTo(trip.getTo()).withDistanceMeters(trip.getDistanceMeters()).withDurationSeconds(trip.getDurationSeconds()).withDepartureTime(trip.getDepartureTime()).withArrivalTime(trip.getArrivalTime()).withSegments(trip.getSegments()).build());
	}

	private static void calculatePercentages(String user) throws IOException {
		String url = "http://traffic.ijs.si/NextPinDev/getActivities";

		 URL obj = new URL("http://traffic.ijs.si/NextPinDev/getActivities");
		 HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		 con.setRequestMethod("GET");


		//add request header
		con.setRequestProperty("token",user);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		//print result
		System.out.println(response.toString());
		 //JSONObject jsonObj = new JSONObject(response);
		 //Iterator<String> keys = jsonObj.keys();
		 try {
			 JSONObject jsonObj  = new JSONObject(response.toString());

			 //JSONArray jsonarr= json.getJSONArray("address");
			 //String address = jsonarr.getJSONObject(0).getString("addressLine1");
			 System.out.println(jsonObj.getJSONArray("data"));
			 JSONArray arr = jsonObj.getJSONArray("data");
			 Integer n_car=0;
			 Integer n_pt=0;
			 Integer n_bike=0;
			 Integer n_walk=0;
			 System.out.println(arr);
			 for (int i = 0; i < arr.length(); i++) {
				 String [] all_modes = {"car", "pt", "bike", "walk"};
				 Random random = new Random();

				 // randomly selects an index from the arr
				 int select = random.nextInt(all_modes.length);
				 //Put the random selected mode to string
				 arr.getJSONObject(i).put("mode", all_modes[select]);

				 JSONObject object = arr.getJSONObject(i);
				 String mode = object.get("mode").toString();
				 //String a = objects.get("");
				 if (mode.equals("car") ){
					 n_car++;
				 }
				 if (mode.equals("pt") ){
					 n_pt++;
				 }
				 if (mode.equals("bike") ){
					 n_bike++;
				 }
				 if (mode.equals("walk") ){
					 n_walk++;
				 }
				 //System.out.println(arr.getJSONObject(i).get("mode"));
			 }
			 //Calculate percentages
			 double car_percent = ( (double)(n_car*100)/(double) arr.length());
			 double pt_percent = ( (double)(n_pt*100)/(double) arr.length());
			 double bike_percent = ( (double)(n_bike*100)/(double) arr.length());
			 double walk_percent = ( (double)(n_walk*100)/(double) arr.length());
			 //percentages should be saved to mongo
			 System.out.println(car_percent);
			 System.out.println(pt_percent);
			 System.out.println(bike_percent);
			 System.out.println(walk_percent);
		 } catch (JSONException e) {
			 e.printStackTrace();
		 }


	 }

	private void CalculateOtherUserPercentages(){
		//Connect to mongodb
		MongoClient mongo = null;
		try {
			mongo = new MongoClient("euprojects.net",3368);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		//Print all database names
		//System.out.println(mongo.getDatabaseNames());
		DB db = mongo.getDB("Optimum");
		DBCollection table = db.getCollection("OptimumUsers");
		//Select the messages where persuasive strategy is Reward
		BasicDBObject searchQuery = new BasicDBObject();

	}

	//Calculate emissions of route
	private double CalculateEmissions(Route trip) throws IOException {
		double emissions = 0.0;
		for (int j = 0; j < trip.getSegments().size(); j++) {
			RouteSegment segment = trip.getSegments().get(j);
			String mode = segment.getModeOfTransport().getGeneralizedType().toString();
			String detailed_mode = segment.getModeOfTransport().getDetailedType().toString();
			Integer distance = segment.getDistanceMeters();
			emissions = emissions + CalculateSegmentsEmissions(distance, mode, detailed_mode);
			}
		return emissions;
	}

	private double CalculateSegmentsEmissions(Integer distance, String travel_mode, String detailed_mode) throws IOException {
		double emissions=0.0;
		if (travel_mode.equals("FOOT") ){
			emissions = 0;
		}
		if (travel_mode.equals("BICYCLE") ){
			emissions = 0;
		}
		if (travel_mode.equals("PUBLIC_TRANSPORT") ) {

			if (detailed_mode.equals("SUBWAY")) {
				emissions = ( (double)(distance*20)/1000 );
			}
			if (detailed_mode.equals("HEAVY_RAIL")) {
				emissions = ( (double)(distance*50)/1000 );
			}
			if (detailed_mode.equals("BUS")) {
				emissions = ( (distance*25.5)/1000 );
			}
		}
		if (travel_mode.equals("CAR") ){
			emissions = ( (double)(distance*110)/1000 );
		}

		return emissions;

	}


}
