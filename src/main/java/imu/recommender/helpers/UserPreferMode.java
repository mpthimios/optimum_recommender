package imu.recommender.helpers;

/**
 * Created by evangelie on 21/11/2016.
 */
public class UserPreferMode implements Comparable<UserPreferMode> {

    private String mode;
    private int percentage;

    public UserPreferMode(String mode, int percentage) {
        super();
        this.mode = mode;
        this.percentage = percentage;
    }

    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getPercentage() {
        return percentage;
    }
    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public int compareTo( UserPreferMode comparePercentages) {

        int compareQuantity = ((UserPreferMode) comparePercentages).getPercentage();

        //ascending order
        //return this.percentage - compareQuantity;

        //descending order
        return compareQuantity - this.percentage;

    }
}
