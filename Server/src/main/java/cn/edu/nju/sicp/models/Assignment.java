package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class Assignment {

    private final static String GRADER_ROOT = "D:\\Temp\\Graders";

    @Id
    private String id;

    private String title;
    private Date beginTime;
    private Date endTime;
    private String submitFileType;
    private int submitCountLimit;
    private double totalScore;
    private double percentage;

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

    public String getSubmitFileType() {
        return submitFileType;
    }

    public void setSubmitFileType(String submitFileType) {
        this.submitFileType = submitFileType;
    }

    public int getSubmitCountLimit() {
        return submitCountLimit;
    }

    public void setSubmitCountLimit(int submitCountLimit) {
        this.submitCountLimit = submitCountLimit;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public Path getGraderPath() {
        return Paths.get(GRADER_ROOT, id, "grader.zip");
    }

    public File getGraderFile() {
        Path path = getGraderPath();
        if (Files.exists(path)) {
            return path.toFile();
        } else {
            return null;
        }
    }

    public GraderInfo getGraderInfo() throws IOException {
        Path path = getGraderPath();
        if (Files.exists(path)) {
            return new GraderInfo(path);
        } else {
            return new GraderInfo();
        }
    }

    public GraderInfo putGraderFile(MultipartFile file) throws IOException {
        Path path = getGraderPath();
        Path parent = path.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        file.transferTo(path);
        return new GraderInfo(path);
    }

    public void deleteGraderFile() throws IOException {
        Path path = getGraderPath();
        Files.delete(path);
    }

    public static class GraderInfo {

        private final long size;
        private final long modifiedAt;

        public GraderInfo() {
            size = modifiedAt = 0;
        }

        public GraderInfo(Path path) throws IOException {
            size = Files.size(path);
            modifiedAt = Files.getLastModifiedTime(path).toMillis();
        }

        public long getSize() {
            return size;
        }

        public long getModifiedAt() {
            return modifiedAt;
        }

    }

}
