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
    private Long submitFileSize;
    private Long submitCountLimit;
    private Double totalScore;
    private Double percentage;
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

    public Long getSubmitFileSize() {
        return submitFileSize;
    }

    public void setSubmitFileSize(Long submitFileSize) {
        this.submitFileSize = submitFileSize;
    }

    public Long getSubmitCountLimit() {
        return submitCountLimit;
    }

    public void setSubmitCountLimit(Long submitCountLimit) {
        this.submitCountLimit = submitCountLimit;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
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
        this.submitFileSize = o.getSubmitFileSize();
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
                ", submitFileSize=" + submitFileSize +
                ", submitCountLimit=" + submitCountLimit +
                ", totalScore=" + totalScore +
                ", percentage=" + percentage +
                ", grader=" + grader +
                '}';
    }

}
