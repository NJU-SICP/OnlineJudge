package cn.edu.nju.sicp.configs;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component("Roles")
public final class RolesConfig {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_STUDENT = "ROLE_STUDENT";

    public static final String OP_USER_CREATE = "OP_USER_CREATE";
    public static final String OP_USER_READ = "OP_USER_READ";
    public static final String OP_USER_UPDATE = "OP_USER_UPDATE";
    public static final String OP_USER_DELETE = "OP_USER_DELETE";
    public static final String OP_ASSIGNMENT_CREATE = "OP_ASSIGNMENT_CREATE";
    public static final String OP_ASSIGNMENT_READ_BEGUN = "OP_ASSIGNMENT_READ_BEGUN";
    public static final String OP_ASSIGNMENT_READ_ALL = "OP_ASSIGNMENT_READ_ALL";
    public static final String OP_ASSIGNMENT_UPDATE = "OP_ASSIGNMENT_UPDATE";
    public static final String OP_ASSIGNMENT_DELETE = "OP_ASSIGNMENT_DELETE";
    public static final String OP_SUBMISSION_CREATE = "OP_SUBMISSION_CREATE";
    public static final String OP_SUBMISSION_READ_SELF = "OP_SUBMISSION_READ_SELF";
    public static final String OP_SUBMISSION_READ_ALL = "OP_SUBMISSION_READ_ALL";
    public static final String OP_SUBMISSION_UPDATE = "OP_SUBMISSION_UPDATE";
    public static final String OP_SUBMISSION_DELETE = "OP_SUBMISSION_DELETE";
    public static final String OP_SUBMISSION_TOKEN_MANAGE = "OP_SUBMISSION_TOKEN_MANAGE";

    private static final HashMap<String, List<String>> grantedAuthoritiesMap;

    static {
        grantedAuthoritiesMap = new HashMap<>();
        grantedAuthoritiesMap.put(OP_USER_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_USER_READ, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_USER_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_USER_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_READ_BEGUN, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_SUBMISSION_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT));
        grantedAuthoritiesMap.put(OP_SUBMISSION_READ_SELF, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT));
        grantedAuthoritiesMap.put(OP_SUBMISSION_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SUBMISSION_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SUBMISSION_DELETE, List.of(ROLE_ADMIN));
        grantedAuthoritiesMap.put(OP_SUBMISSION_TOKEN_MANAGE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
    }

    public static HashMap<String, List<String>> getGrantedAuthoritiesMap() {
        return grantedAuthoritiesMap;
    }

}
