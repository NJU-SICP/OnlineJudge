package cn.edu.nju.sicp.dtos;

import cn.edu.nju.sicp.models.User;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

@Projection(name = "UserInfo", types = User.class)
public interface UserInfo {

    String getId();

    String getUsername();

    String getFullName();

    Date getValidAfter();

    Date getValidBefore();

    int getRing();

}
