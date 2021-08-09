package cn.edu.nju.sicp.models;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class User implements UserDetails {

    private final static Pattern BCRYPT_PATTERN = Pattern.compile("\\A\\$2a?\\$\\d\\d\\$[./0-9A-Za-z]{53}");

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;
    private String fullName;
    private String password;
    private Collection<String> roles;
    private Date expires;
    private boolean enabled;
    private boolean locked;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassword() {
        return password;
    }

    public boolean validatePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, this.password);
    }

    public void setPassword(String password) {
        if (!StringUtils.isEmpty(password)) {
            if (BCRYPT_PATTERN.matcher(password).matches()) {
                this.password = password;
            } else {
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                this.password = encoder.encode(password);
            }
        }
    }

    public Collection<String> getRoles() {
        return roles;
    }

    public void setRoles(Collection<String> roles) {
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(Role::new)
                .map(Role::getAuthorities)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    @Override
    public boolean isAccountNonExpired() {
        Date now = new Date();
        return expires == null || now.before(expires);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roles=" + roles +
                ", enabled=" + enabled +
                ", locked=" + locked +
                ", expires=" + expires +
                '}';
    }

}
