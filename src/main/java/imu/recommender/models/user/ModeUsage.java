package imu.recommender.models.user;

public class ModeUsage {
	
	private double car_percent;
	private double pt_percent;
	private double bike_percent;
	private double walk_percent;
	private double bike_percentGW;
	private double walk_percentGW;
	
	public ModeUsage(){
		car_percent = 0.0;
		pt_percent = 0.0;
		bike_percent = 0.0;
		walk_percent = 0.0;
		bike_percentGW = 0.0;
		walk_percentGW = 0.0;
	}

	public double getCar_percent() {
		return car_percent;
	}

	public void setCar_percent(double car_percent) {
		this.car_percent = car_percent;
	}

	public double getPt_percent() {
		return pt_percent;
	}

	public void setPt_percent(double pt_percent) {
		this.pt_percent = pt_percent;
	}

	public double getBike_percent() {
		return bike_percent;
	}

	public void setBike_percent(double bike_percent) {
		this.bike_percent = bike_percent;
	}

	public double getWalk_percent() {
		return walk_percent;
	}

	public void setWalk_percent(double walk_percent) {
		this.walk_percent = walk_percent;
	}

	public double getBike_percentGW() {
		return bike_percentGW;
	}

	public void setBike_percentGW(double bike_percentGW) {
		this.bike_percent = bike_percentGW;
	}

	public double getWalk_percentGW() {
		return walk_percentGW;
	}

	public void setWalk_percentGW(double walk_percentGW) {
		this.walk_percentGW = walk_percentGW;
	}
	
}
