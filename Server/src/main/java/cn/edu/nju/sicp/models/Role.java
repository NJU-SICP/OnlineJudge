package cn.edu.nju.sicp.models;

import cn.edu.nju.sicp.configs.RolesConfig;
import org.springframework.data.annotation.Id;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.stream.Collectors;

public class Role implements GrantedAuthority {

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
        return RolesConfig.getGrantedAuthoritiesMap().entrySet().stream()
                .filter((entry) -> entry.getValue().contains(id))
                .map((entry) -> new SimpleGrantedAuthority(entry.getKey()))
                .collect(Collectors.toList());
    }

}
