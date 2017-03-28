package imu.recommender.logs;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import java.util.Date;

/**
 * Created by evangelia on 3/27/2017.
 */
public class UserRouteCriteriaViolation {

    @Id
    private ObjectId id;
    private String userId;
    private DBObject originalResults;
    private DBObject personalizededResults;
    private Date createdDate;

    public ObjectId getId() {
        return id;
    }
    public void setId(ObjectId id) {
        this.id = id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public Date getCreatedDate() {
        return createdDate;
    }
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public DBObject getPersonalizededResults() {
        return personalizededResults;
    }

    public void setPersonalizededResults(DBObject personalizededResults) {
        this.personalizededResults = personalizededResults;
    }

    public DBObject getOriginalResults() {
        return originalResults;
    }

    public void setOriginalResults(DBObject originalResults) {
        this.originalResults = originalResults;
    }
}
