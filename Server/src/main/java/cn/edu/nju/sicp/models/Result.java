package cn.edu.nju.sicp.models;

import java.util.Date;
import java.util.List;

public class Result {

    private Integer score;
    private String message;
    private List<ScoreDetail> details;
    private String log;
    private String error;
    private Date retry;
    private Date date;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ScoreDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ScoreDetail> details) {
        this.details = details;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Date getRetry() {
        return retry;
    }

    public void setRetry(Date retry) {
        this.retry = retry;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public static class ScoreDetail {

        private String title;
        private int score;
        private String message;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

    @Override
    public String toString() {
        return "Result{" +
                "score=" + score +
                ", date=" + date +
                '}';
    }

}
