package cn.edu.nju.sicp.contests.hog;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

@Document(collection = "contests_hog")
public class HogEntry {

    private String id;
    private String userId;
    private String submissionId;

    private String key;
    private Date date;
    private Boolean valid;
    private String message;

    private String name;
    private Integer size;
    private Map<String, Integer> wins;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Map<String, Integer> getWins() {
        return wins;
    }

    public void setWins(Map<String, Integer> wins) {
        this.wins = wins;
    }

    @Override
    public String toString() {
        return "HogEntry{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", key='" + key + '\'' +
                ", date=" + date +
                ", valid=" + valid +
                ", message='" + message + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                '}';
    }

}
