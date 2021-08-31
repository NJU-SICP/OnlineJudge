package cn.edu.nju.sicp.models;

import java.util.Date;
import java.util.Set;

public class Grader {

    private String assignmentId;
    private String key;
    private Set<String> imageTags;
    private String imageId;
    private String imageBuildLog;
    private String imageBuildError;
    private Date imageBuiltAt;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Set<String> getImageTags() {
        return imageTags;
    }

    public void setImageTags(Set<String> imageTags) {
        this.imageTags = imageTags;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageBuildLog() {
        return imageBuildLog;
    }

    public void setImageBuildLog(String imageBuildLog) {
        this.imageBuildLog = imageBuildLog;
    }

    public String getImageBuildError() {
        return imageBuildError;
    }

    public void setImageBuildError(String imageBuildError) {
        this.imageBuildError = imageBuildError;
    }

    public Date getImageBuiltAt() {
        return imageBuiltAt;
    }

    public void setImageBuiltAt(Date imageBuiltAt) {
        this.imageBuiltAt = imageBuiltAt;
    }

    @Override
    public String toString() {
        return "Grader{" +
                "key='" + key + '\'' +
                ", imageTags=" + imageTags +
                ", imageId='" + imageId + '\'' +
                ", imageBuildError='" + imageBuildError + '\'' +
                ", imageBuiltAt=" + imageBuiltAt +
                '}';
    }

}
