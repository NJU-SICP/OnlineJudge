package cn.edu.nju.sicp.contests.hog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties
public class HogTriggerResult {

    private String name;
    private Boolean valid;
    private Integer size;
    private Map<Integer, Map<Integer, Integer>> strategy;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Map<Integer, Map<Integer, Integer>> getStrategy() {
        return strategy;
    }

    public void setStrategy(Map<Integer, Map<Integer, Integer>> strategy) {
        this.strategy = strategy;
    }

}

