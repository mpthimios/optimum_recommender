package imu.recommender.models.message;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by evangelie on 4/11/2016.
 */

@Entity("OptimumMessages")

public class Message {

    @Id
    private ObjectId id;

    private String persuasive_strategy;
    private String context;
    private Integer number_of_times_sent;
    private String message_text;
    private String target;
    private Integer number_of_successes;
    private Double utility;
    private String parameters;
    private String message_text_german;
    private String message_text_slo;

    public Message(){

        //initialize variables
        this.persuasive_strategy = "suggestion";
        this.context = "NiceWeather";
        this.number_of_times_sent = 0;
        this.message_text = "Today it’s sunny! Take the opportunity to use your bicycle to save CO2 emissions.";
        this.target = "bike";
        this.number_of_successes = 0;
        this.parameters = "no";
        this.message_text_german = "";
        this.message_text_slo = "";
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getPersuasive_strategy() { return persuasive_strategy; }

    public void setPersuasive_strategy(String persuasive_strategy) {
        this.persuasive_strategy =persuasive_strategy;
    }

    public String getContext() { return context; }

    public void setContext(String context) {
        this.context =context;
    }

    public void setNumber_of_successes(Integer number_of_successes) {
        this.number_of_successes = number_of_successes;
    }

    public Integer getNumber_of_successes () { return number_of_successes;}

    public String getMessage_text() { return message_text; }

    public void setMessage_text(String message_text) {
        this.message_text =message_text;
    }

    public void setNumber_of_times_sent(Integer number_of_times_sent) {
        this.number_of_times_sent = number_of_times_sent;
    }

    public Integer getNumber_of_times_sent () { return number_of_times_sent;}

    public String getTarget() { return target; }

    public void setTarget(String target) {
        this.target = target;
    }

    public Double getUtility() {
        return utility;
    }
    public void setUtility(Double utility) {
        this.utility = utility;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getMessage_text_german() {
        return message_text_german;
    }

    public String getMessage_text_slo() {
        return message_text_slo;
    }

    public void setMessage_text_german(String message_text_german) {
        this.message_text_german = message_text_german;
    }

    public void setMessage_text_slo(String message_text_slo) {
        this.message_text_slo = message_text_slo;
    }
}
