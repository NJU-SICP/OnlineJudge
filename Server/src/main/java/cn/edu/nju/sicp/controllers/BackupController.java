package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.S3Config;
import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Backup;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.BackupRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Objects;

@RestController
@RequestMapping("/backups")
public class BackupController {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private BackupRepository backupRepository;

    private final String s3Bucket;
    private final S3Client s3Client;
    private final Logger logger;

    public BackupController(S3Config s3Config) {
        this.s3Bucket = s3Config.getBucket();
        this.s3Client = s3Config.getInstance();
        this.logger = LoggerFactory.getLogger(BackupController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_BACKUP_READ_SELF) or hasAuthority(@Roles.OP_BACKUP_READ_ALL)")
    public ResponseEntity<Page<Backup>> listBackups(@RequestParam(required = false) String userId,
                                                    @RequestParam(required = false) String assignmentId,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.getId().equals(userId) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_BACKUP_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId).orElse(null);
        Backup backup = new Backup();
        backup.setUserId(userId);
        backup.setAssignmentId(assignment == null ? null : assignment.getId());
        Page<Backup> backups = backupRepository
                .findAll(Example.of(backup),
                        PageRequest.of(page == null || page < 0 ? 0 : page,
                                size == null || size < 0 ? 20 : size,
                                Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ResponseEntity<>(backups, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_BACKUP_READ_SELF) or hasAuthority(@Roles.OP_BACKUP_READ_ALL)")
    public ResponseEntity<Backup> getBackup(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!backup.getUserId().equals(user.getId()) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_BACKUP_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(backup, HttpStatus.OK);
    }


    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority(@Roles.OP_BACKUP_CREATE)")
    public ResponseEntity<Backup> createBackup(@RequestPart("assignmentId") String assignmentId,
                                               @RequestPart("file") MultipartFile file,
                                               @RequestPart(value = "analytics") String analytics) {
        if (!Objects.equals(FilenameUtils.getExtension(file.getOriginalFilename()), "zip")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "备份提交的文件必须是zip格式。");
        }

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = assignmentRepository.findOneByIdOrSlug(assignmentId, assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Backup example = new Backup();
        example.setUserId(user.getId());
        example.setAssignmentId(assignment.getId());
        long count = backupRepository.count(Example.of(example));

        Backup backup;
        try {
            backup = new Backup();
            backup.setUserId(user.getId());
            backup.setAssignmentId(assignment.getId());
            backup.setKey(String.format("backups/%s/%s/%s.zip", assignment.getSlug(), user.getUsername(), count + 1));
            backup.setAnalytics(new ObjectMapper().readValue(analytics, Backup.Analytics.class));
            backup.setCreatedAt(new Date());
            backupRepository.save(backup);
        } catch (JsonProcessingException e) {
            logger.error(String.format("CreateBackup failed: %s by %s", e.getMessage(), user), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法处理提交数据。");
        }

        try {
            String key = backup.getKey();
            s3Client.putObject(builder -> builder.bucket(s3Bucket).key(key).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (S3Exception | IOException e) {
            logger.error(String.format("CreateBackup failed: %s %s by %s", e.getMessage(), backup, user), e);
            backupRepository.delete(backup);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取或存储备份文件。");
        }

        return new ResponseEntity<>(backup, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_BACKUP_DELETE)")
    public ResponseEntity<Void> deleteBackup(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        backupRepository.delete(backup);

        try {
            String key = backup.getKey();
            s3Client.deleteObject(builder -> builder.bucket(s3Bucket).key(key).build());
        } catch (S3Exception e) {
            logger.error(String.format("DeleteBackup failed: %s %s by %s", e.getMessage(), backup, user), e);
        }

        logger.info(String.format("DeleteBackup %s by %s", backup, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority(@Roles.OP_BACKUP_READ_SELF) or hasAuthority(@Roles.OP_BACKUP_READ_ALL)")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Backup backup = backupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!backup.getUserId().equals(user.getId()) && user.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_BACKUP_READ_ALL))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        try {
            String key = backup.getKey();
            InputStream stream = s3Client.getObject(builder -> builder.bucket(s3Bucket).key(key).build());
            InputStreamResource resource = new InputStreamResource(stream);
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (S3Exception e) {
            logger.error(String.format("DownloadSubmission failed: %s %s by %s", e.getMessage(), backup, user), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取备份文件。");
        }
    }

}
