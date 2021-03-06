package imu.recommender.models.strategy;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by evangelie on 12/12/2016.
 */

@Entity("OptimumPersuasiveStrategies")
public class Strategy {
    @Id
    private ObjectId id;

    private String persuasive_strategy;
    private Integer number_of_times_sent;
    private Integer number_of_successes;
    private double probability;
    private Integer total_sucess_feedback;
    private Integer total_feedback;

    public Strategy(){

        //initialize variables
        this.persuasive_strategy = "suggestion";
        this.number_of_times_sent = 0;
        this.number_of_successes = 0;
        this.total_sucess_feedback = 0;
        this.total_feedback = 0;
        this.probability = 0.0;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getPersuasive_strategy() { return persuasive_strategy; }

    public void setPersuasive_strategy(String strategy) {
        this.persuasive_strategy = strategy;
    }

    public void setNumber_of_successes(Integer number_of_successes) {
        this.number_of_successes = number_of_successes;
    }

    public Integer getNumber_of_successes () { return number_of_successes;}

    public Integer getNumber_of_times_sent () { return number_of_times_sent;}


    public void setNumber_of_times_sent(Integer number_of_times_sent) {
        this.number_of_times_sent = number_of_times_sent;
    }

    public Double getProbability() {
        return probability;
    }
    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public Integer getTotal_sucess_feedback() {
        return total_sucess_feedback;
    }

    public void setTotal_sucess_feedback(Integer total_sucess_feedback) {
        this.total_sucess_feedback = total_sucess_feedback;
    }

    public Integer getTotal_feedback() {
        return total_feedback;
    }

    public void setTotal_feedback(Integer total_feedback) {
        this.total_feedback = total_feedback;
    }
}
