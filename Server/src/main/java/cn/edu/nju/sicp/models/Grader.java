package cn.edu.nju.sicp.models;

import java.util.Date;

public class Grader {

    private String assignmentId;
    private String key;
    private String imageId;
    private String imageRepository;
    private String imageTag;
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

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageRepository() {
      return imageRepository;
    }

    public void setImageRepository(String imageRepository) {
      this.imageRepository = imageRepository;
    }
    
    public String getImageTag() {
      return imageTag;
    }

    public void setImageTag(String imageTag) {
      this.imageTag = imageTag;
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
                ", imageId='" + imageId + '\'' +
                ", imageRepository='" + imageRepository + '\'' +
                ", imageTag='" + imageTag + '\'' +
                ", imageBuildError='" + imageBuildError + '\'' +
                ", imageBuiltAt=" + imageBuiltAt +
                '}';
    }

}
