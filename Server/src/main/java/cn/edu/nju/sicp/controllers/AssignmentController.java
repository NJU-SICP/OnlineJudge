package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import cn.edu.nju.sicp.tasks.BuildImageTask;
import cn.edu.nju.sicp.tasks.RemoveImageTask;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentRepository repository;

    @Autowired
    private SimpleAsyncTaskExecutor buildImageExecutor;

    @Autowired
    private SyncTaskExecutor removeImageExecutor;

    private final String dataPath;
    private final Logger logger;

    public AssignmentController(@Value("${spring.application.data-path}") String dataPath) {
        this.dataPath = dataPath;
        this.logger = LoggerFactory.getLogger(AssignmentController.class);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Assignment>> searchAssignments(String prefix) {
        List<Assignment> assignments = repository.findFirst5ByTitleStartingWith(prefix);
        return new ResponseEntity<>(assignments, HttpStatus.OK);
    }

    @GetMapping("/{id}/grader")
    public ResponseEntity<Grader> getGrader(@PathVariable String id) {
        Assignment assignment = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ResponseEntity<>(assignment.getGrader(), HttpStatus.OK);
    }

    @PostMapping("/{id}/grader")
    public ResponseEntity<Grader> setGrader(@PathVariable String id, @RequestBody MultipartFile file) {
        Assignment assignment = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (assignment.getGrader() != null) {
            removeImageExecutor.execute(new RemoveImageTask(assignment.getGrader()));
            assignment.setGrader(null);
        }
        repository.save(assignment);

        try {
            Grader grader = new Grader();
            grader.setAssignmentId(id);

            Path path = Paths.get(dataPath, "assignments", id, "grader.zip");
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            file.transferTo(path);

            grader.setFilePath(path.toString());
            grader.setImageTags(Set.of(String.format("grader-%s:%s", id,
                    DateFormatUtils.format(new Date(), "yyyyMMdd-HHmmss"))));
            assignment.setGrader(grader);
            repository.save(assignment);

            BuildImageTask task = new BuildImageTask(grader, repository);
            buildImageExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
            return new ResponseEntity<>(grader, HttpStatus.CREATED);
        } catch (IOException e) {
            logger.error(String.format("SetGrader failed: %s %s", e.getMessage(), assignment));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}/grader")
    public ResponseEntity<String> deleteGrader(@PathVariable String id) {
        Assignment assignment = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Grader grader = assignment.getGrader();
        removeImageExecutor.execute(new RemoveImageTask(grader));

        try {
            Files.deleteIfExists(Paths.get(grader.getFilePath()));
        } catch (IOException e) {
            logger.warn(String.format("DeleteGrader failed: %s %s", e.getMessage(), assignment));
        }

        assignment.setGrader(null);
        repository.save(assignment);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
