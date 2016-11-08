package imu.recommender.models.user;

public class Demographics {

	private String name;
	private String country_of_residence;
	private String email;
	private String facebook_account;
	private String twitter_account;
	private String gender;
	private int age;
	private String education;
	private String occupation;
	private String physical_status;
	private int children;
	
	public Demographics (){
		//initialize the variables
		this.name = "John Doe";
		this.country_of_residence = "Greece";
		this.email = "john@email.com";
		this.facebook_account = "john_doe";
		this.twitter_account = "@john_doe";
		this.gender = "male";
		this.age = 29;
		this.education = "higher degree";
		this.occupation = "worker";
		this.physical_status = "good";
		this.children = 3;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCountry_of_residence() {
		return country_of_residence;
	}

	public void setCountry_of_residence(String country_of_residence) {
		this.country_of_residence = country_of_residence;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFacebook_account() {
		return facebook_account;
	}

	public void setFacebook_account(String facebook_account) {
		this.facebook_account = facebook_account;
	}

	public String getTwitter_account() {
		return twitter_account;
	}

	public void setTwitter_account(String twitter_account) {
		this.twitter_account = twitter_account;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getEducation() {
		return education;
	}

	public void setEducation(String education) {
		this.education = education;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getPhysical_status() {
		return physical_status;
	}

	public void setPhysical_status(String physical_status) {
		this.physical_status = physical_status;
	}

	public int getChildren() {
		return children;
	}

	public void setChildren(int children) {
		this.children = children;
	}
	
}
