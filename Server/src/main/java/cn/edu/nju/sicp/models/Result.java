package cn.edu.nju.sicp.models;

import java.util.Date;
import java.util.List;

public class Result {

    private Integer score;
    private String message;
    private List<ScoreDetail> details;
    private String log;
    private String error;
    private Date retryAt;
    private Date gradedAt;
    private String gradedBy;

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

    public Date getRetryAt() {
        return retryAt;
    }

    public void setRetryAt(Date retryAt) {
        this.retryAt = retryAt;
    }

    public Date getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(Date gradedAt) {
        this.gradedAt = gradedAt;
    }

    public String getGradedBy() {
        return gradedBy;
    }

    public void setGradedBy(String gradedBy) {
        this.gradedBy = gradedBy;
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
                ", error='" + error + '\'' +
                ", retryAt=" + retryAt +
                ", gradedAt=" + gradedAt +
                ", gradedBy='" + gradedBy + '\'' +
                '}';
    }

}
