package cn.edu.nju.sicp.controllers;

import cn.edu.nju.sicp.models.Token;
import cn.edu.nju.sicp.models.User;
import cn.edu.nju.sicp.repositories.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;

@RestController
@RequestMapping("/submissions/tokens")
public class SubmissionTokenController {

    @Autowired
    private TokenRepository repository;

    private final Logger logger;

    public SubmissionTokenController() {
        this.logger = LoggerFactory.getLogger(SubmissionTokenController.class);
    }

    @GetMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_TOKEN_MANAGE)")
    public ResponseEntity<Page<Token>> listTokens(@RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        Page<Token> tokens = repository
                .findAll(PageRequest.of(page == null || page < 0 ? 0 : page,
                        size == null || size < 0 ? 20 : size, Sort.by(Sort.Direction.DESC, "issuedAt")));
        return new ResponseEntity<>(tokens, HttpStatus.OK);
    }

    @PostMapping()
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_TOKEN_MANAGE)")
    public ResponseEntity<Token> createToken(@RequestBody Token createdToken) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Token token = new Token();
        token.setValues(createdToken);
        token.setToken(UUID.randomUUID().toString());
        token.setIssuedBy(user.getId());
        token.setIssuedAt(new Date());
        repository.save(token);
        logger.info(String.format("CreateToken %s by %s", token, user));
        return new ResponseEntity<>(token, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(@Roles.OP_SUBMISSION_TOKEN_MANAGE)")
    public ResponseEntity<Token> deleteToken(@PathVariable String id) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Token token = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(token);
        logger.info(String.format("DeleteToken %s by %s", token, user));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
