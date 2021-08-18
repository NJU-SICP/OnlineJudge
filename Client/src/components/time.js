import React, {useEffect, useState} from "react";
import http from "../http";
import moment from "moment";
import {Button, message, Typography} from "antd";
import {LoadingOutlined} from "@ant-design/icons";


const Time = () => {
    const [time, setTime] = useState(null);
    const [error, setError] = useState(null);

    const sync = () => {
        http()
            .get(`/misc/time`)
            .then((res) => {
                setTime(moment(res.data));
            })
            .catch((err) => {
                console.error(err);
                message.error("无法连接至服务器，请检查网络环境。");
                if (!err.response) {
                    setError("无法连接至服务器");
                }
            });
    };

    const tick = () => {
        setTime(t => t == null ? null : moment(t).add(1, "seconds"));
    };

    useEffect(() => {
        sync();
        const interval1 = setInterval(sync, 60 * 1000);
        const interval2 = setInterval(tick, 1000);
        return () => {
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
