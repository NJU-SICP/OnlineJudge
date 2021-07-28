package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Assignment;
import cn.edu.nju.sicp.repositories.AssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentRepository repository;

    private final Logger logger;

    public AssignmentController() {
        logger = LoggerFactory.getLogger(AssignmentController.class);
    }

    @GetMapping("/{id}/grader")
    public ResponseEntity<Assignment.GraderInfo> getGrader(@PathVariable String id) {
        Assignment assignment = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            return new ResponseEntity<>(assignment.getGraderInfo(), HttpStatus.OK);
        } catch (IOException e) {
            logger.error(String.format("GetGrader failed: %s %s", e.getMessage(), assignment));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{id}/grader")
    public ResponseEntity<Assignment.GraderInfo> setGrader(@PathVariable String id, @RequestBody MultipartFile file) {
        Assignment assignment = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            assignment.putGraderFile(file);
            return new ResponseEntity<>(assignment.getGraderInfo(), HttpStatus.OK);
        } catch (IOException e) {
            logger.error(String.format("SetGrader failed: %s %s", e.getMessage(), assignment));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}/grader")
    public ResponseEntity<String> deleteGrader(@PathVariable String id) {
        Assignment assignment = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            assignment.deleteGraderFile();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IOException e) {
            logger.error(String.format("DeleteGrader failed: %s %s", e.getMessage(), assignment));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
