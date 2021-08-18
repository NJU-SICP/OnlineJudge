import React, {useEffect, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useHistory} from "react-router-dom";
import moment from "moment";
import http from "../http";

import {Button, Layout, message} from "antd";
import {UserOutlined} from "@ant-design/icons";

import {clear} from "../store/auth";

const Header = () => {
    const history = useHistory();
    const dispatch = useDispatch();
    const auth = useSelector((state) => state.auth.value);

    const [time, setTime] = useState(null);
    const fetchTime = () => {
        http()
            .get(`/misc/time`)
            .then((res) => {
                setTime(moment(res.data));
            })
            .catch((err) => {
                console.error(err);
                message.error("无法连接至服务器，请检查网络环境。");
            });
    };

    useEffect(() => {
        fetchTime();
        const interval1 = setInterval(fetchTime, 60 * 1000);
        const interval2 = setInterval(() => {
            setTime(t => t == null ? null : moment(t).add(1, "seconds"));
        }, 1000);
        return () => {
            clearInterval(interval1);
            clearInterval(interval2);
        };
    }, []);

    const logout = () => {
        dispatch(clear());
        history.push("/auth/login");
    }

    return (
        <Layout.Header style={{color: "white"}}>
            <div style={{float: "left"}}>
                SICP Online Judge
                <span style={{marginLeft: "5em"}}>
                    {time && <>服务器时间：{time.format("YYYY-MM-DD HH:mm:ss")}</>}
                </span>
            </div>
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
