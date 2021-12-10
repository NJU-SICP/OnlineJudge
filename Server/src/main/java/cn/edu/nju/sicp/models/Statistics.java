package cn.edu.nju.sicp.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

@JsonIgnoreProperties
public final class Statistics {

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

    @JsonCreator
    public Statistics(@JsonProperty("count") Long count,
                      @JsonProperty("min") Integer min,
                      @JsonProperty("max") Integer max,
                      @JsonProperty("average") Double average) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.average = average;
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
