package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.configs.DockerConfig;
import cn.edu.nju.sicp.configs.RolesConfig;
import cn.edu.nju.sicp.dtos.AssignmentInfo;
import cn.edu.nju.sicp.models.Grader;
import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.models.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentRepository repository;

    @Autowired
    private ProjectionFactory projectionFactory;

    @Autowired
    private SimpleAsyncTaskExecutor buildImageExecutor;

    @Autowired
    private SyncTaskExecutor removeImageExecutor;

    @Autowired
    private DockerConfig dockerConfig;

    private final String dataPath;
    private final Logger logger;

    public AssignmentController(@Value("${spring.application.data-path}") String dataPath) {
        this.dataPath = dataPath;
        this.logger = LoggerFactory.getLogger(AssignmentController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_READ_BEGUN)")
    public ResponseEntity<Page<AssignmentInfo>> listBegunAssignments(@RequestParam(required = false) Integer page,
                                                                     @RequestParam(required = false) Integer size) {
        Page<AssignmentInfo> infos = repository
                .findAllByBeginTimeBefore(new Date(), PageRequest.of(page == null || page < 0 ? 0 : page,
                        size == null || size < 0 ? 20 : size,
                        Sort.by(Sort.Direction.DESC, "endTime")))
                .map(a -> projectionFactory.createProjection(AssignmentInfo.class, a));
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_READ_BEGUN)")
    public ResponseEntity<List<AssignmentInfo>> listBegunAssignmentsByCalendar
            (@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date1 = calendar.getTime();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 2);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date date2 = calendar.getTime();
        List<AssignmentInfo> infos = repository
                .findAllByBeginTimeBeforeAndEndTimeBetween(new Date(), date1, date2).stream()
                .map(a -> projectionFactory.createProjection(AssignmentInfo.class, a))
                .collect(Collectors.toList());
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_READ_ALL)")
    public ResponseEntity<Page<AssignmentInfo>> listAllAssignments(@RequestParam(required = false) Integer page,
                                                                   @RequestParam(required = false) Integer size) {
        Page<AssignmentInfo> infos = repository
                .findAll(PageRequest.of(page == null || page < 0 ? 0 : page,
                        size == null || size < 0 ? 20 : size,
                        Sort.by(Sort.Direction.DESC, "endTime")))
                .map(a -> projectionFactory.createProjection(AssignmentInfo.class, a));
        return new ResponseEntity<>(infos, HttpStatus.OK);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_READ_ALL)")
    public ResponseEntity<List<AssignmentInfo>> searchAssignments(@RequestParam String prefix) {
        List<Assignment> assignments = repository.findFirst5ByTitleStartingWith(prefix);
        return new ResponseEntity<>(assignments.stream()
                .map(assignment -> projectionFactory.createProjection(AssignmentInfo.class, assignment))
                .collect(Collectors.toList()), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_READ_BEGUN) or hasAuthority(@Roles.OP_ASSIGNMENT_READ_ALL)")
    public ResponseEntity<Assignment> viewAssignment(@PathVariable String id) {
        Assignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (assignment.getBeginTime() != null && (new Date()).before(assignment.getBeginTime())) {
            if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals(RolesConfig.OP_ASSIGNMENT_READ_ALL))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }
        return new ResponseEntity<>(assignment, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_CREATE)")
    public ResponseEntity<Assignment> createAssignment(@RequestBody Assignment createdAssignment) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = new Assignment();
        assignment.setValues(createdAssignment);
        repository.save(assignment);
        logger.info(String.format("CreateAssignment %s by %s", assignment, user));
        return new ResponseEntity<>(assignment, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_UPDATE)")
    public ResponseEntity<Assignment> updateAssignment(@PathVariable String id,
                                                       @RequestBody Assignment updatedAssignment) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assignment.setValues(updatedAssignment);
        repository.save(assignment);
        logger.info(String.format("UpdateAssignment %s by %s", assignment, user));
        return new ResponseEntity<>(assignment, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_DELETE)")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(assignment);
        logger.info(String.format("DeleteAssignment %s by %s", assignment, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}/grader")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_UPDATE)")
    public ResponseEntity<Grader> getAssignmentGrader(@PathVariable String id) {
        Assignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new ResponseEntity<>(assignment.getGrader(), HttpStatus.OK);
    }

    @PostMapping("/{id}/grader")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_UPDATE)")
    public ResponseEntity<Grader> setAssignmentGrader(@PathVariable String id, @RequestBody MultipartFile file) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (assignment.getGrader() != null) {
            removeImageExecutor.execute(new RemoveImageTask(assignment.getGrader(), dockerConfig.getInstance()));
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

            BuildImageTask task = new BuildImageTask(grader, repository, dockerConfig.getInstance());
            buildImageExecutor.execute(task, AsyncTaskExecutor.TIMEOUT_IMMEDIATE);
            logger.info(String.format("SetAssignmentGrader %s by %s", assignment, user));
            return new ResponseEntity<>(grader, HttpStatus.CREATED);
        } catch (IOException e) {
            logger.error(String.format("SetAssignmentGrader failed: %s %s by %s", e.getMessage(), assignment, user));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}/grader")
    @PreAuthorize("hasAuthority(@Roles.OP_ASSIGNMENT_UPDATE)")
    public ResponseEntity<String> deleteAssignmentGrader(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Assignment assignment = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Grader grader = assignment.getGrader();
        removeImageExecutor.execute(new RemoveImageTask(grader, dockerConfig.getInstance()));

        try {
            Files.deleteIfExists(Paths.get(grader.getFilePath()));
        } catch (IOException e) {
            logger.error(String.format("DeleteAssignmentGrader failed: %s %s by %s", e.getMessage(), assignment, user));
        }

        assignment.setGrader(null);
        repository.save(assignment);
        logger.info(String.format("DeleteAssignmentGrader %s by %s", assignment, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
