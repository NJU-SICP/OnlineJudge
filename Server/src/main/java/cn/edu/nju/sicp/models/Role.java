package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Role implements GrantedAuthority {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_STUDENT = "ROLE_STUDENT";

    public static final String OP_USER_CREATE = "OP_USER_CREATE";
    public static final String OP_USER_READ = "OP_USER_READ";
    public static final String OP_USER_UPDATE = "OP_USER_UPDATE";
    public static final String OP_USER_DELETE = "OP_USER_DELETE";
    public static final String OP_ASSIGNMENT_CREATE = "OP_ASSIGNMENT_CREATE";
    public static final String OP_ASSIGNMENT_READ = "OP_ASSIGNMENT_READ";
    public static final String OP_ASSIGNMENT_UPDATE = "OP_ASSIGNMENT_UPDATE";
    public static final String OP_ASSIGNMENT_DELETE = "OP_ASSIGNMENT_DELETE";
    public static final String OP_SUBMISSION_CREATE = "OP_SUBMISSION_CREATE";
    public static final String OP_SUBMISSION_READ_SELF = "OP_SUBMISSION_READ_SELF";
    public static final String OP_SUBMISSION_READ_ALL = "OP_SUBMISSION_READ_ALL";
    public static final String OP_SUBMISSION_UPDATE = "OP_SUBMISSION_UPDATE";
    public static final String OP_SUBMISSION_DELETE = "OP_SUBMISSION_DELETE";

    private static final HashMap<String, List<String>> grantedAuthoritiesMap;

    static {
        grantedAuthoritiesMap = new HashMap<>();
        grantedAuthoritiesMap.put(OP_USER_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_USER_READ, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_USER_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_USER_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_READ, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_SUBMISSION_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT));
        grantedAuthoritiesMap.put(OP_SUBMISSION_READ_SELF, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT));
        grantedAuthoritiesMap.put(OP_SUBMISSION_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SUBMISSION_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SUBMISSION_DELETE, List.of(ROLE_ADMIN));
    }

    @Id
    private final String id;

    public Role(String id) {
        this.id = id;
    }

    @Override
    public String getAuthority() {
        return id;
    }

    public String getId() {
        return id;
    }

    public List<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthoritiesMap.entrySet().stream()
                .filter((entry) -> entry.getValue().contains(id))
                .map((entry) -> new SimpleGrantedAuthority(entry.getKey()))
                .collect(Collectors.toList());
    }

}
