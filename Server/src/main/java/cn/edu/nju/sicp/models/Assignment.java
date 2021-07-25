package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;

import java.util.Date;

public class Assignment {

    @Id
    private String id;

    private String title;
    private Date beginTime;
    private Date endTime;
    private String submitFileType;
    private int submitCountLimit;
    private double totalScore;
    private double percentage;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getSubmitFileType() {
        return submitFileType;
    }

    public void setSubmitFileType(String submitFileType) {
        this.submitFileType = submitFileType;
    }

    public int getSubmitCountLimit() {
        return submitCountLimit;
    }

    public void setSubmitCountLimit(int submitCountLimit) {
        this.submitCountLimit = submitCountLimit;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
