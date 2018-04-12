package imu.recommender.models.message;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by evangelia on 3/4/2018.
 */
@Entity("MessageThrottlingContext")

public class MessageThrottlingContext {

    private String strategy;
    private String context;
    private String userId;
    private Integer count;
    private ObjectId _id;

    public MessageThrottlingContext(){

        //initialize variables
        this.strategy = "";
        this.context="";
        this.userId = "";
        this.count = 1;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public ObjectId get_id() {
        return _id;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
