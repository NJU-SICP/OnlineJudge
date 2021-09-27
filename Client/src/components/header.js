import React from "react";
import {useDispatch, useSelector} from "react-redux";
import {useHistory} from "react-router-dom";

import {Button, Layout, Typography} from "antd";
import {UserOutlined} from "@ant-design/icons";

import {clear} from "../store/auth";
import Time from "./time";

const Header = () => {
    const history = useHistory();
    const dispatch = useDispatch();
    const auth = useSelector((state) => state.auth.value);

    const logout = () => {
        dispatch(clear());
        history.push("/auth/login");
    }

    return (
        <Layout.Header className="header" style={{zIndex: 1000}}>
            <div style={{float: "left"}}>
                <Typography.Text strong>SICP Online Judge</Typography.Text>
                <span style={{marginLeft: "6em"}}>当前服务器时间：<Time/></span>
            </div>
            <div style={{float: "right"}}>
                {!!auth && <>
                    <UserOutlined/> &nbsp;
                    <span>{auth.username} {auth.fullName}</span>
                    <Button type="text" danger onClick={logout}>退出</Button>
                </>}
            </div>
        </Layout.Header>
    );
};

export default Header;
