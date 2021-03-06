package imu.recommender.models.request;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by evangelia on 24/11/2017.
 */

@Entity("Request")

public class Request {
    private Integer numberOfUsersGroupA;
    private Integer numberOfUsersGroupB;
    private Integer numberOfUsersGroupC;
    private Integer numberOfMessages;
    private Integer numberOfGraphs;
    private Integer numberOfCombination;

    public Request(){

        //initialize variables
        this.numberOfUsersGroupA = 0;
        this.numberOfUsersGroupB = 0;
        this.numberOfUsersGroupC = 0;
    }


    public Integer getNumberOfUsersGroupA() {
        return numberOfUsersGroupA;
    }

    public void setNumberOfUsersGroupA(Integer numberOfUsersGroupA) {
        this.numberOfUsersGroupA = numberOfUsersGroupA;
    }

    public Integer getNumberOfUsersGroupB() {
        return numberOfUsersGroupB;
    }

    public void setNumberOfUsersGroupB(Integer numberOfUsersGroupB) {
        this.numberOfUsersGroupB = numberOfUsersGroupB;
    }

    public Integer getNumberOfUsersGroupC() {
        return numberOfUsersGroupC;
    }

    public void setNumberOfUsersGroupC(Integer numberOfUsersGroupC) {
        this.numberOfUsersGroupC = numberOfUsersGroupC;
    }

    public Integer getNumberOfMessages() {
        return numberOfMessages;
    }

    public void setNumberOfMessages(Integer numberOfMessages) {
        this.numberOfMessages = numberOfMessages;
    }

    public Integer getNumberOfGraphs() {
        return numberOfGraphs;
    }

    public void setNumberOfGraphs(Integer numberOfGraphs) {
        this.numberOfGraphs = numberOfGraphs;
    }

    public Integer getNumberOfCombination() {
        return numberOfCombination;
    }

    public void setNumberOfCombination(Integer numberOfCombination) {
        this.numberOfCombination = numberOfCombination;
    }
}
