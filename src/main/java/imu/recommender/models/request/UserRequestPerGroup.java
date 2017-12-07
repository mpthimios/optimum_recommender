package imu.recommender.models.request;

import org.mongodb.morphia.annotations.Entity;

import java.sql.Timestamp;

/**
 * Created by evangelia on 28/11/2017.
 */
@Entity("UserRequestPerGroup")

public class UserRequestPerGroup {

    private String RequestId;
    private String userId;
    private String Group;
    private Timestamp Timestamp;
    private String Strategy;

    public UserRequestPerGroup(){

        //initialize variables
        this.RequestId = "";
        this.userId = "";
        this.Group = "";
        this.Timestamp = new Timestamp(System.currentTimeMillis());;
        this.Strategy = "";
    }

    public String getRequestId() {
        return RequestId;
    }

    public void setRequestId(String requestId) {
        RequestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroup() {
        return Group;
    }

    public void setGroup(String group) {
        Group = group;
    }

    public java.sql.Timestamp getTimestamp() {
        return Timestamp;
    }

    public void setTimestamp(java.sql.Timestamp timestamp) {
        Timestamp = timestamp;
    }

    public String getStrategy() {
        return Strategy;
    }

    public void setStrategy(String strategy) {
        Strategy = strategy;
    }
}
