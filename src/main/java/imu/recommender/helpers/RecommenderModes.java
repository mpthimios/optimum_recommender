package imu.recommender.helpers;

public class RecommenderModes {
	public static final int WALK = 0;
	public static final int BICYCLE = 1;
	public static final int BIKE_SHARING = 2;
	public static final int BIKE_AND_RIDE = 3;
	public static final int PUBLIC_TRANSPORT = 4;
	public static final int PARK_AND_RIDE_WITH_BIKE = 5;
	public static final int PARK_AND_RIDE = 6;
	public static final int CAR = 7;
	public static final int UNKNOWN = 8;
	
	public static int[] recommenderModesOrder = {
			WALK, BICYCLE, BIKE_SHARING, BIKE_AND_RIDE, PUBLIC_TRANSPORT, PARK_AND_RIDE_WITH_BIKE, PARK_AND_RIDE, CAR, UNKNOWN
		};
	
	public static String[] recommenderModesStr = {
		"walk",
		"bicycle",
		"bikeSharing",
		"bike&ride",		
		"pt",
		"park&ride_with_bike",
		"park&ride",
		"car",
		"unknown"
	};
}
