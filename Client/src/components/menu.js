import React from "react";
import {useSelector} from "react-redux";
import {useHistory} from "react-router-dom";

import {Menu as AntMenu} from "antd";
import {EditOutlined, HomeOutlined, PaperClipOutlined, SettingOutlined, UserSwitchOutlined} from "@ant-design/icons";

const Menu = () => {
    const history = useHistory();
    const auth = useSelector((state) => state.auth.value);
    const redirect = (to) => history.push(to);
    return (
        <AntMenu mode="inline" style={{height: '100%', borderRight: 0}}
                 defaultSelectedKeys={[history.location.pathname]}>
            <AntMenu.Item key="/" icon={<HomeOutlined/>} onClick={() => redirect("/")}>
                系统主页
            </AntMenu.Item>
            <AntMenu.Item key="/assignments" icon={<EditOutlined/>}
                          onClick={() => redirect("/assignments")}>
                作业列表
            </AntMenu.Item>
            <AntMenu.Item key="/configuration" icon={<UserSwitchOutlined/>}
                          onClick={() => redirect("/config")}>
                用户设置
            </AntMenu.Item>
            {!!auth && auth.roles && auth.authorities &&
            auth.roles.filter(role => role !== "ROLE_STUDENT").length > 0 && <>
                <AntMenu.SubMenu key="/admin" title="系统管理" icon={<SettingOutlined/>}>
                    {auth.authorities.indexOf("OP_USER_READ") > 0 &&
                    <AntMenu.Item key="/admin/users" icon={<UserSwitchOutlined/>}
                                  onClick={() => redirect("/admin/users")}>
                        用户管理
                    </AntMenu.Item>
                    }
                    {auth.authorities.indexOf("OP_ASSIGNMENT_READ_ALL") > 0 &&
                    <AntMenu.Item key="/admin/assignments" icon={<EditOutlined/>}
                                  onClick={() => redirect("/admin/assignments")}>
                        作业管理
                    </AntMenu.Item>
                    }
                    {auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") > 0 &&
                    <AntMenu.Item key="/admin/submissions" icon={<PaperClipOutlined/>}
                                  onClick={() => redirect("/admin/submissions")}>
                        提交管理
                    </AntMenu.Item>
                    }
                </AntMenu.SubMenu>
            </>}
        </AntMenu>
    );
};

export default Menu;
