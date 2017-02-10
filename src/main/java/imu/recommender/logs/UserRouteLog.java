package imu.recommender.logs;

import java.util.Date;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.mongodb.DBObject;

@Entity("UserRouteLog")

public class UserRouteLog {
	@Id
    private ObjectId id;	
	private DBObject originalResults;
	private DBObject recommendedResults;
	private Date createdDate;
	
	public ObjectId getId() {
		return id;
	}
	public void setId(ObjectId id) {
		this.id = id;
	}	
	public DBObject getOriginalResults() {
		return originalResults;
	}
	public void setOriginalResults(DBObject originalResults) {
		this.originalResults = originalResults;
	}
	public DBObject getRecommendedResults() {
		return recommendedResults;
	}
	public void setRecommendedResults(DBObject recommendedResults) {
		this.recommendedResults = recommendedResults;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	
}
