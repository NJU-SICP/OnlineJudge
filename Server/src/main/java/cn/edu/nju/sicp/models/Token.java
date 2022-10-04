package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;

import java.util.Date;

@Deprecated
public class Token {

    @Id
    private String id;

    private String token;
    private String userId;
    private String assignmentId;
    private String issuedBy; // userId
    private Date issuedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public void setValues(Token token) {
        setUserId(token.getUserId());
        setAssignmentId(token.getAssignmentId());
    }

    @Override
    public String toString() {
        return "Token{" +
                "id='" + id + '\'' +
                ", token='" + token + '\'' +
                ", userId='" + userId + '\'' +
                ", assignmentId='" + assignmentId + '\'' +
                ", issuedBy='" + issuedBy + '\'' +
                ", issuedAt='" + issuedAt + '\'' +
                '}';
    }

}
