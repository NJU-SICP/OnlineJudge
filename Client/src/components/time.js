import React, {useEffect, useState} from "react";
import http from "../http";
import moment from "moment";
import {message, Typography} from "antd";
import {LoadingOutlined} from "@ant-design/icons";


const Time = () => {
    const [time, setTime] = useState(null);
    const [error, setError] = useState(null);

    const sync = (dict) => {
        http()
            .get(`/misc/time`)
            .then((res) => {
                if (dict.mounted) {
                    setTime(moment(res.data));
                }
            })
            .catch((err) => {
                console.error(err);
                message.error("无法连接至服务器，请检查网络环境。");
                if (!err.response && dict.mounted) {
                    setError("无法连接至服务器");
                }
            });
    };

    const tick = (dict) => {
        if (dict.mounted) {
            setTime(t => t == null ? null : moment(t).add(1, "seconds"));
        }
    };

    useEffect(() => {
        const dict = {mounted: true}; // avoid setState after unmount
        sync(dict);
        const interval1 = setInterval(() => sync(dict), 60 * 1000);
        const interval2 = setInterval(() => tick(dict), 1000);
        return () => {
            dict.mounted = false;
            clearInterval(interval1);
            clearInterval(interval2);
        };
    }, []);

    return (<>
        {!error
            ? <>
                <Typography.Text>
                    {!time
                        ? <LoadingOutlined/>
                        : time.format("YYYY-MM-DD HH:mm:ss")}
                </Typography.Text>
            </>
            : <><Typography.Text type="danger">{error}</Typography.Text></>
        }
    </>);
};

export default Time;
