package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;

public class Assignment {

    @Id
    private String id;

    private String title;
    private Date beginTime;
    private Date endTime;
    private List<String> submitFileTypes;
    private int submitCountLimit;

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

    public List<String> getSubmitFileTypes() {
        return submitFileTypes;
    }

    public void setSubmitFileTypes(List<String> submitFileTypes) {
        this.submitFileTypes = submitFileTypes;
    }

    public int getSubmitCountLimit() {
        return submitCountLimit;
    }

    public void setSubmitCountLimit(int submitCountLimit) {
        this.submitCountLimit = submitCountLimit;
    }

}
