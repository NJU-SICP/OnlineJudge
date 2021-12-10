package cn.edu.nju.sicp.models;

import java.util.*;

public class Statistics {

    private final Long count;
    private final Integer min;
    private final Integer max;
    private final Double average;

    public Statistics(IntSummaryStatistics stats) {
        this.count = stats.getCount();
        this.min = stats.getMin();
        this.max = stats.getMax();
        this.average = stats.getAverage();
    }

    public Statistics(Long count, Integer score) {
        this.count = count;
        this.min = this.max = score;
        this.average = Double.valueOf(score);
    }

    public Long getCount() {
        return count;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }

    public Double getAverage() {
        return average;
    }

}
