import React from "react";
import {useSelector} from "react-redux";
import {useHistory} from "react-router-dom";

import {Menu as AntMenu} from "antd";
import {
    CloudServerOutlined,
    EditOutlined,
    HomeOutlined,
    PaperClipOutlined,
    TableOutlined,
    UserSwitchOutlined
} from "@ant-design/icons";

const Menu = () => {
    const history = useHistory();
    const auth = useSelector((state) => state.auth.value);
    const redirect = (to) => history.push(to);
    return (
        <AntMenu mode="inline" style={{height: '100%', paddingTop: "1.5em"}}
                 defaultSelectedKeys={[history.location.pathname]}>
            <AntMenu.ItemGroup key="g1" title="系统导航">
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
            </AntMenu.ItemGroup>
            {!!auth && auth.roles && auth.authorities &&
            auth.roles.filter(role => role !== "ROLE_STUDENT").length > 0 && <>
                <AntMenu.Divider style={{
                    marginTop: "1.5em",
                    marginBottom: "1.5em",
                    marginLeft: "1em",
                    marginRight: "1em"
                }}/>
                <AntMenu.ItemGroup key="g2" title="系统管理">
                    {auth.authorities.indexOf("OP_USER_READ") >= 0 &&
                    <AntMenu.Item key="/admin/users" icon={<UserSwitchOutlined/>}
                                  onClick={() => redirect("/admin/users")}>
                        用户管理
                    </AntMenu.Item>
                    }
                    {auth.authorities.indexOf("OP_ASSIGNMENT_READ_ALL") >= 0 &&
                    <AntMenu.Item key="/admin/assignments" icon={<EditOutlined/>}
                                  onClick={() => redirect("/admin/assignments")}>
                        作业管理
                    </AntMenu.Item>
                    }
                    {auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") >= 0 &&
                    <AntMenu.Item key="/admin/submissions" icon={<PaperClipOutlined/>}
                                  onClick={() => redirect("/admin/submissions")}>
                        提交管理
                    </AntMenu.Item>
                    }
                    {auth.authorities.indexOf("OP_BACKUP_READ_ALL") >= 0 &&
                    <AntMenu.Item key="/admin/backups" icon={<CloudServerOutlined/>}
                                  onClick={() => redirect("/admin/backups")}>
                        备份查询
                    </AntMenu.Item>
                    }
                    {auth.authorities.indexOf("OP_USER_READ") >= 0 &&
                    auth.authorities.indexOf("OP_ASSIGNMENT_READ_ALL") >= 0 &&
                    auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") >= 0 &&
                    <AntMenu.Item key="/admin/score-table" icon={<TableOutlined/>}
                                  onClick={() => redirect("/admin/score-table")}>
                        成绩查询
                    </AntMenu.Item>
                    }
                </AntMenu.ItemGroup>
            </>}
        </AntMenu>
    );
};

export default Menu;
