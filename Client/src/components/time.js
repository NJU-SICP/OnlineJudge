import React, {useEffect, useState} from "react";
import http from "../http";
import moment from "moment";
import {message} from "antd";


const Time = () => {
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

    return (
        <span>{time && time.format("YYYY-MM-DD HH:mm:ss")}</span>
    );
};

export default Time;
