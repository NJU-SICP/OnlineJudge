import React from "react";
import {useSelector} from "react-redux";

import {Typography} from "antd";
import {HomeOutlined} from "@ant-design/icons";

const Welcome = () => {
    const auth = useSelector((state) => state.auth.value);
    const username = auth?.username;
    const fullName = auth?.fullName;

    return (
        <>
            <Typography.Title level={2}>
                <HomeOutlined/> 系统主页
            </Typography.Title>
            <p>欢迎访问SICP Online Judge，您已经以{fullName}（{username}）的身份登录。</p>
        </>
    );
};

export default Welcome;
