package imu.recommender.helpers;

public class RecommenderModes {
	public static final int WALK = 0;
	public static final int BICYCLE = 1;
	public static final int BIKE_AND_RIDE = 2;
	public static final int PUBLIC_TRANSPORT = 3;
	public static final int PARK_AND_RIDE_WITH_BIKE = 4;
	public static final int PARK_AND_RIDE = 5;
	public static final int CAR = 6;
	public static final int UNKNOWN = 7;
	
	public static String[] recommenderModesStr = {
		"walk",
		"bicycle",
		"bike&ride",		
		"pt",
		"park&ride_with_bike",
		"park&ride",
		"car",
		"unknown"
	};
}
