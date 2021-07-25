import React from "react";
import {useSelector} from "react-redux";
import {useHistory} from "react-router-dom";

import {Menu as AntMenu} from "antd";
import {EditOutlined, HomeOutlined, SettingOutlined, UserSwitchOutlined} from "@ant-design/icons";

const Menu = () => {
    const history = useHistory();
    const auth = useSelector((state) => state.auth.value);
    const redirect = (to) => history.push(to);
    return (
        <AntMenu mode="inline" style={{height: '100%', borderRight: 0}}
                 defaultSelectedKeys={[history.location.pathname]}>
            <AntMenu.Item key="/" icon={<HomeOutlined/>} onClick={() => redirect("/")}>系统主页</AntMenu.Item>
            {!!auth && auth.ring < 3 && <>
                <AntMenu.SubMenu key="/admin" title="系统管理" icon={<SettingOutlined/>}>
                    <AntMenu.Item key="/admin/users" icon={<UserSwitchOutlined/>}
                                  onClick={() => redirect("/admin/users")}>
                        用户管理
                    </AntMenu.Item>
                    <AntMenu.Item key="/admin/assignments" icon={<EditOutlined/>}
                                  onClick={() => redirect("/admin/assignments")}>
                        作业管理
                    </AntMenu.Item>
                </AntMenu.SubMenu>
            </>}
        </AntMenu>
    );
};

export default Menu;
