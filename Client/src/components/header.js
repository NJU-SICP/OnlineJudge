import React from "react";
import {useDispatch, useSelector} from "react-redux";
import {useHistory} from "react-router-dom";

import {Button, Layout} from "antd";
import {UserOutlined} from "@ant-design/icons";

import {clear} from "../store/auth";

const Header = () => {
    const history = useHistory();
    const dispatch = useDispatch();
    const auth = useSelector((state) => state.auth.value);

    const logout = () => {
        dispatch(clear());
        history.push("/auth/login");
    }

    return (
        <Layout.Header style={{color: "white"}}>
            <div style={{float: "left"}}>SICP Online Judge</div>
            <div style={{float: "right"}}>
                {!!auth && <>
                    <UserOutlined/> &nbsp;
                    <span>{auth.fullName}</span>
                    <Button type="text" danger onClick={logout}>退出</Button>
                </>}
            </div>
        </Layout.Header>
    );
};

export default Header;
