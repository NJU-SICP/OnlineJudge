package cn.edu.nju.sicp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Backup {

    @Id
    private String id;

    private String userId;
    private String assignmentId;

    private String filePath;
    private Analytics analytics;
    private Date createdAt;

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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Backup{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", assignmentId='" + assignmentId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Analytics {

        private String time;
        @JsonProperty("time-utc") private String timeUTC;
        private boolean unlock;
        private List<String> question;
        private List<String> requestedQuestions;
        private List<String> requestedSuite;
        private List<String> requestedCase;
        private History history;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getTimeUTC() {
            return timeUTC;
        }

        public void setTimeUTC(String timeUTC) {
            this.timeUTC = timeUTC;
        }

        public boolean isUnlock() {
            return unlock;
        }

        public void setUnlock(boolean unlock) {
            this.unlock = unlock;
        }

        public List<String> getQuestion() {
            return question;
        }

        public void setQuestion(List<String> question) {
            this.question = question;
        }

        public List<String> getRequestedQuestions() {
            return requestedQuestions;
        }

        public void setRequestedQuestions(List<String> requestedQuestions) {
            this.requestedQuestions = requestedQuestions;
        }

        public List<String> getRequestedSuite() {
            return requestedSuite;
        }

        public void setRequestedSuite(List<String> requestedSuite) {
            this.requestedSuite = requestedSuite;
        }

        public List<String> getRequestedCase() {
            return requestedCase;
        }

        public void setRequestedCase(List<String> requestedCase) {
            this.requestedCase = requestedCase;
        }

        public History getHistory() {
            return history;
        }

        public void setHistory(History history) {
            this.history = history;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class History {

        private HashMap<String, Question> questions;
        @JsonProperty("all_attempts") private int allAttempts;
        private List<String> question;

        public HashMap<String, Question> getQuestions() {
            return questions;
        }

        public void setQuestions(HashMap<String, Question> questions) {
            this.questions = questions;
        }

        public int getAllAttempts() {
            return allAttempts;
        }

        public void setAllAttempts(int allAttempts) {
            this.allAttempts = allAttempts;
        }

        public List<String> getQuestion() {
            return question;
        }

        public void setQuestion(List<String> question) {
            this.question = question;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Question {

        private int attempts;
        private boolean solved;

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public boolean isSolved() {
            return solved;
        }

        public void setSolved(boolean solved) {
            this.solved = solved;
        }

    }

}
