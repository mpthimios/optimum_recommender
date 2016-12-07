package imu.recommender.models.user;

public class RoutePreference {
	
	private String label = "";
	private double walkPref = 0.0;
	private double bikePref = 0.0;
	private double carPref = 0.0;
	private double ptPref = 0.0;
	private double bikeSharingPref = 0.0;
	private double carSharingPref = 0.0;
	
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public double getWalkPref() {
		return walkPref;
	}
	public void setWalkPref(double walkPref) {
		this.walkPref = walkPref;
	}
	public double getBikePref() {
		return bikePref;
	}
	public void setBikePref(double bikePref) {
		this.bikePref = bikePref;
	}
	public double getCarPref() {
		return carPref;
	}
	public void setCarPref(double carPref) {
		this.carPref = carPref;
	}
	public double getPtPref() {
		return ptPref;
	}
	public void setPtPref(double ptPref) {
		this.ptPref = ptPref;
	}
	public double getBikeSharingPref() {
		return bikeSharingPref;
	}
	public void setBikeSharingPref(double bikeSharingPref) {
		this.bikeSharingPref = bikeSharingPref;
	}
	public double getCarSharingPref() {
		return carSharingPref;
	}
	public void setCarSharingPref(double carSharingPref) {
		this.carSharingPref = carSharingPref;
	}
	
	
	
}
