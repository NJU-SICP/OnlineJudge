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
    private Grader grader;

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

    public Grader getGrader() {
        return grader;
    }

    public void setGrader(Grader grader) {
        this.grader = grader;
    }

    public void setValues(Assignment o) {
        this.title = o.getTitle();
        this.beginTime = o.getBeginTime();
        this.endTime = o.getEndTime();
        this.submitFileType = o.getSubmitFileType();
        this.submitCountLimit = o.getSubmitCountLimit();
        this.totalScore = o.getTotalScore();
        this.percentage = o.getPercentage();
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", beginTime=" + beginTime +
                ", endTime=" + endTime +
                ", submitFileType='" + submitFileType + '\'' +
                ", submitCountLimit=" + submitCountLimit +
                ", totalScore=" + totalScore +
                ", percentage=" + percentage +
                ", grader=" + grader +
                '}';
    }

}
