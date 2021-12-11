import React from "react";
import {useSelector} from "react-redux";
import {Link, useHistory} from "react-router-dom";

import {Menu as AntMenu} from "antd";
import {
    CloudServerOutlined,
    EditOutlined,
    HighlightOutlined,
    HomeOutlined,
    PaperClipOutlined,
    RobotOutlined,
    TableOutlined,
    UserSwitchOutlined
} from "@ant-design/icons";

const Menu = () => {
    const history = useHistory();
    const auth = useSelector((state) => state.auth.value);
    return (
        <AntMenu mode="inline" style={{height: '100%', paddingTop: "1.5em"}}
            defaultSelectedKeys={[history.location.pathname]}
            selectedKeys={[history.location.pathname]}>
            <AntMenu.ItemGroup key="g1" title="系统导航">
                <AntMenu.Item key="/" icon={<HomeOutlined />}>
                    <Link to="/">系统主页</Link>
                </AntMenu.Item>
                <AntMenu.Item key="/assignments" icon={<EditOutlined />}>
                    <Link to="/assignments">作业列表</Link>
                </AntMenu.Item>
                <AntMenu.Item key="/config" icon={<UserSwitchOutlined />}>
                    <Link to="/config">用户设置</Link>
                </AntMenu.Item>
            </AntMenu.ItemGroup>
            <AntMenu.Divider style={{
                marginTop: "1.5em",
                marginBottom: "1.5em",
                marginLeft: "1em",
                marginRight: "1em"
            }} />
            <AntMenu.ItemGroup key="g2" title="额外内容">
                <AntMenu.Item key="/contests/hog" icon={<RobotOutlined />}>
                    <Link to="/contests/hog">Hog Contest</Link>
                </AntMenu.Item>
            </AntMenu.ItemGroup>
            {!!auth && auth.roles && auth.authorities &&
                auth.roles.filter(role => role !== "ROLE_STUDENT" && role !== "ROLE_GUEST").length > 0 && <>
                    <AntMenu.Divider style={{
                        marginTop: "1.5em",
                        marginBottom: "1.5em",
                        marginLeft: "1em",
                        marginRight: "1em"
                    }} />
                    <AntMenu.ItemGroup key="g3" title="系统管理">
                        {auth.authorities.indexOf("OP_USER_READ") >= 0 &&
                            <AntMenu.Item key="/admin/users" icon={<UserSwitchOutlined />}>
                                <Link to="/admin/users">用户管理</Link>
                            </AntMenu.Item>
                        }
                        {auth.authorities.indexOf("OP_ASSIGNMENT_READ_ALL") >= 0 &&
                            <AntMenu.Item key="/admin/assignments" icon={<EditOutlined />}>
                                <Link to="/admin/assignments">作业管理</Link>

                            </AntMenu.Item>
                        }
                        {auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") >= 0 &&
                            <AntMenu.Item key="/admin/submissions" icon={<PaperClipOutlined />}>
                                <Link to="/admin/submissions">提交管理</Link>
                            </AntMenu.Item>
                        }
                        {auth.authorities.indexOf("OP_BACKUP_READ_ALL") >= 0 &&
                            <AntMenu.Item key="/admin/backups" icon={<CloudServerOutlined />}>
                                <Link to="/admin/backups">备份查询</Link>
                            </AntMenu.Item>
                        }
                        {auth.authorities.indexOf("OP_PLAGIARISM_READ_ALL") >= 0 &&
                            <AntMenu.Item key="/admin/plagiarisms" icon={<HighlightOutlined />}>
                                <Link to="/admin/plagiarisms">抄袭管理</Link>
                            </AntMenu.Item>
                        }
                        {auth.authorities.indexOf("OP_USER_READ") >= 0 &&
                            auth.authorities.indexOf("OP_ASSIGNMENT_READ_ALL") >= 0 &&
                            auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") >= 0 &&
                            <AntMenu.Item key="/admin/score-table" icon={<TableOutlined />}>
                                <Link to="/admin/score-table">成绩查询</Link>
                            </AntMenu.Item>
                        }
                    </AntMenu.ItemGroup>
                </>}
        </AntMenu>
    );
};

export default Menu;
