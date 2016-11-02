package imu.recommender.models;

public class OwnedVehicle {

	private String type;
	private String fuel_type;
	private double engine_size;
	private double year;
	
	public OwnedVehicle(){
		this.type = "?";
		this.fuel_type = "?";
		this.engine_size = 1600.0;
		this.year = 1999.0;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFuel_type() {
		return fuel_type;
	}

	public void setFuel_type(String fuel_type) {
		this.fuel_type = fuel_type;
	}

	public double getEngine_size() {
		return engine_size;
	}

	public void setEngine_size(double engine_size) {
		this.engine_size = engine_size;
	}

	public double getYear() {
		return year;
	}

	public void setYear(double year) {
		this.year = year;
	}
	
}
