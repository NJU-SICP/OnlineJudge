package cn.edu.nju.sicp.dtos;

import cn.edu.nju.sicp.models.User;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "UserInfo", types = User.class)
public interface UserInfo {

    String getId();

    String getUsername();

    String getFullName();

    boolean isAccountNonExpired();

    boolean isAccountNonLocked();

    boolean isEnabled();

    int getRing();

}
