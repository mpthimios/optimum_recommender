package imu.recommender.models.user;

public class FacebookData {
	
	private String likes;
	private String photos;
	private String posts;
	
	public FacebookData(){
		this.likes = "";
		this.photos = "";
		this.posts = "";
	}

	public String getLikes() {
		return likes;
	}

	public void setLikes(String likes) {
		this.likes = likes;
	}

	public String getPhotos() {
		return photos;
	}

	public void setPhotos(String photos) {
		this.photos = photos;
	}

	public String getPosts() {
		return posts;
	}

	public void setPosts(String posts) {
		this.posts = posts;
	}
	
	
}
