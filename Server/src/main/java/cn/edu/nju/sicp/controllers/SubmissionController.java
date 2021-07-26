package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.Submission;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.repositories.SubmissionRepository;
import cn.edu.nju.sicp.repositories.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.stream.Stream;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    private Logger logger;

    public SubmissionController() {
        logger = LoggerFactory.getLogger(SubmissionController.class);
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<String> createSubmission(@RequestPart("assignmentId") String assignmentId,
                                                   @RequestPart(value = "token", required = false) String token,
                                                   @RequestPart("file") MultipartFile file) throws OperationNotSupportedException, JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (String) authentication.getPrincipal();
        User user = userRepository.findByUsername(username);

        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            return new ResponseEntity<>("作业不存在。", HttpStatus.BAD_REQUEST);
        }

        // TODO: support submit by token
        if (token != null) throw new OperationNotSupportedException("token not supported");

        int limit = assignment.getSubmitCountLimit();
        if (limit > 0) {
            int count = submissionRepository.countByUserIdAndAssignmentId(user.getId(), assignment.getId());
            if (count >= limit) {
                return new ResponseEntity<>("提交次数已达上限。", HttpStatus.BAD_REQUEST);
            }
        } else if (limit == 0) {
            return new ResponseEntity<>("此作业不允许自行提交。", HttpStatus.BAD_REQUEST);
        }

        Submission submission = new Submission();
        submission.setUserId(user.getId());
        submission.setAssignmentId(assignment.getId());
        submission.setResults(null);
        submission.setCreatedAt(new Date());
        submission.setCreatedBy(null);
        submission = submissionRepository.save(submission);
        logger.info(String.format("CreateSubmission %s", submission));

        try {
            String path = String.format("D:\\Temp\\%s", submission.getId());
            String name = String.format("submit%s", assignment.getSubmitFileType());
            if (Files.notExists(Paths.get(path))) {
                Files.createDirectories(Paths.get(path));
            }
            file.transferTo(Paths.get(path, name));
        } catch (IOException e) {
            logger.error("Cannot save submission file: " + e.getMessage());
            submissionRepository.delete(submission);
            return new ResponseEntity<>("无法存储提交文件。", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String response = (new ObjectMapper()).writeValueAsString(submission);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
