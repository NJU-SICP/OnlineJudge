package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;

public class Submission {

    @Id
    private String id;

    private String userId;
    private String assignmentId;

    private Integer score;
    private String message;
    private List<Result> results;
    private Date gradedAt;
    private String gradedBy;

    private Date createdAt;
    private String createdBy;

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

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

    public List<Result> getResults() {
        return results;
    }

    public void setResults(List<Result> results) {
        this.results = results;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    static class Result {

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
        return "Submission{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", assignmentId='" + assignmentId + '\'' +
                ", score=" + score +
                ", gradedAt=" + gradedAt +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }

}
