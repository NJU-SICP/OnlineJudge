package cn.edu.nju.sicp.docker;

import java.util.Date;
import java.util.Set;

public class Grader {

    private String assignmentId;
    private String dockerfilePath;
    private Set<String> imageTags;
    private String imageId;
    private String imageBuildLog;
    private Date imageBuiltAt;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getDockerfilePath() {
        return dockerfilePath;
    }

    public void setDockerfilePath(String dockerfilePath) {
        this.dockerfilePath = dockerfilePath;
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

    public Date getImageBuiltAt() {
        return imageBuiltAt;
    }

    public void setImageBuiltAt(Date imageBuiltAt) {
        this.imageBuiltAt = imageBuiltAt;
    }

    @Override
    public String toString() {
        return "Grader{" +
                "dockerfilePath='" + dockerfilePath + '\'' +
                ", imageTags=" + imageTags +
                ", imageId='" + imageId + '\'' +
                ", imageBuiltAt=" + imageBuiltAt +
                '}';
    }
}
