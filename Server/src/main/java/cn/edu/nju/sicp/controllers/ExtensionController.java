package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Extension;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.ExtensionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;

@RestController
@RequestMapping("/extensions")
public class ExtensionController {

    private final ExtensionRepository extensionRepository;
    private final Logger logger;

    public ExtensionController(ExtensionRepository extensionRepository) {
        this.extensionRepository = extensionRepository;
        this.logger = LoggerFactory.getLogger(ExtensionController.class);
    }


    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_EXTENSION_READ)")
    public ResponseEntity<Page<Extension>> listExtensions(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        Page<Extension> extensions = extensionRepository
                .findAll(PageRequest.of(page == null || page < 0 ? 0 : page,
                        size == null || size < 0 ? 20 : size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new ResponseEntity<>(extensions, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_EXTENSION_CREATE)")
    public ResponseEntity<Extension> createExtension(@RequestBody Extension createdExtension) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Extension extension = new Extension();
        extension.setUserId(createdExtension.getUserId());
        extension.setAssignmentId(createdExtension.getAssignmentId());
        extension.setEndTime(createdExtension.getEndTime());
        extension.setCreatedBy(user.getId());
        extension.setCreatedAt(new Date());
        extensionRepository.save(extension);
        logger.info(String.format("CreateExtension %s %s", extension, user));
        return new ResponseEntity<>(extension, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_EXTENSION_DELETE)")
    public ResponseEntity<Extension> deleteExtension(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Extension extension = extensionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        extensionRepository.delete(extension);
        logger.info(String.format("DeleteExtension %s %s", extension, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
