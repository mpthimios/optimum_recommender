package imu.recommender.models.message;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by evangelia on 15/1/2018.
 */
@Entity("MessageThrottling")

public class MessageThrottling {

    private String messageId;
    private String userId;
    private Integer count;

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
}