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

    public Strategy(){

        //initialize variables
        this.persuasive_strategy = "suggestion";
        this.number_of_times_sent = 0;
        this.number_of_successes = 0;
        this.probability = 0.0;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getPersuasive_strategy() { return persuasive_strategy; }

    public void setPersuasive_strategy(ObjectId id) {
        this.persuasive_strategy =persuasive_strategy;
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
}
