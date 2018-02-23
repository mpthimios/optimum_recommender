package imu.recommender.models.message;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by evangelia on 15/1/2018.
 */
@Entity("MessageThrottling")

public class MessageThrottling {

    private String messageId;
    private String userId;
    private Integer count;
    private ObjectId _id;

    public MessageThrottling(){

        //initialize variables
        this.messageId = "";
        this.userId = "";
        this.count = 1;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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

}