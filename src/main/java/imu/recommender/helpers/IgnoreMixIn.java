package imu.recommender.helpers;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class IgnoreMixIn {
    @JsonIgnore public List<?> costs;
}
