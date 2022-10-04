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
    public static final String ROLE_GUEST = "ROLE_GUEST";

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
    @Deprecated
    public static final String OP_SUBMISSION_TOKEN_MANAGE = "OP_SUBMISSION_TOKEN_MANAGE";
    public static final String OP_EXTENSION_READ = "OP_EXTENSION_READ";
    public static final String OP_EXTENSION_CREATE = "OP_EXTENSION_CREATE";
    public static final String OP_EXTENSION_DELETE = "OP_EXTENSION_DELETE";
    public static final String OP_BACKUP_READ_SELF = "OP_BACKUP_READ_SELF";
    public static final String OP_BACKUP_READ_ALL = "OP_BACKUP_READ_ALL";
    public static final String OP_BACKUP_CREATE = "OP_BACKUP_CREATE";
    public static final String OP_BACKUP_DELETE = "OP_BACKUP_DELETE";
    public static final String OP_PLAGIARISM_READ_ALL = "OP_PLAGIARISM_READ_ALL";
    public static final String OP_PLAGIARISM_READ_SELF = "OP_PLAGIARISM_READ_SELF";
    public static final String OP_PLAGIARISM_CREATE = "OP_PLAGIARISM_CREATE";
    public static final String OP_PLAGIARISM_DELETE = "OP_PLAGIARISM_DELETE";
    public static final String OP_SCORE_READ_SELF = "OP_SCORE_READ_SELF";
    public static final String OP_SCORE_READ_ALL = "OP_SCORE_READ_ALL";

    private static final HashMap<String, List<String>> grantedAuthoritiesMap;

    static {
        grantedAuthoritiesMap = new HashMap<>();
        grantedAuthoritiesMap.put(OP_USER_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_USER_READ, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_USER_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_USER_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_READ_BEGUN, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_ASSIGNMENT_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER));
        grantedAuthoritiesMap.put(OP_SUBMISSION_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_SUBMISSION_READ_SELF, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_SUBMISSION_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SUBMISSION_UPDATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SUBMISSION_DELETE, List.of(ROLE_ADMIN));
        grantedAuthoritiesMap.put(OP_EXTENSION_READ, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_EXTENSION_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_EXTENSION_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_BACKUP_READ_SELF, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_BACKUP_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_BACKUP_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_BACKUP_DELETE, List.of(ROLE_ADMIN));
        grantedAuthoritiesMap.put(OP_PLAGIARISM_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_PLAGIARISM_READ_SELF, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_PLAGIARISM_CREATE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_PLAGIARISM_DELETE, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
        grantedAuthoritiesMap.put(OP_SCORE_READ_SELF, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF, ROLE_STUDENT, ROLE_GUEST));
        grantedAuthoritiesMap.put(OP_SCORE_READ_ALL, List.of(ROLE_ADMIN, ROLE_TEACHER, ROLE_STAFF));
    }

    public static HashMap<String, List<String>> getGrantedAuthoritiesMap() {
        return grantedAuthoritiesMap;
    }

}
