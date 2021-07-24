package cn.edu.nju.sicp.models;

import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collection;
import java.util.Date;

public class User implements UserDetails {

    @Id
    private String id;

    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private boolean enabled;
    private boolean locked;
    private Date expires;

    private String fullName;
    private int ring;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public boolean validatePassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, this.password);
    }

    public void setPassword(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        this.password = encoder.encode(password);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        Date now = new Date();
        return expires == null || now.before(expires);
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public int getRing() {
        return ring;
    }

    public void setRing(int ring) {
        this.ring = ring;
    }

}
