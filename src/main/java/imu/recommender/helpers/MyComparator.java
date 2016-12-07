package imu.recommender.helpers;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by evangelie on 6/12/2016.
 */

public  class MyComparator implements Comparator {

    Map map;

    public MyComparator(Map map) {
        this.map = map;
    }

    public int compare(Object o1, Object o2) {

        return ((Double) map.get(o2)).compareTo((Double) map.get(o1));

    }
}