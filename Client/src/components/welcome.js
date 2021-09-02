import React, {useEffect, useState} from "react";
import {useHistory} from "react-router-dom";
import {useSelector} from "react-redux";
import moment from "moment";
import http from "../http";

import {Badge, Calendar, Typography} from "antd";
import {HomeOutlined} from "@ant-design/icons";

const Welcome = () => {
    const history = useHistory();
    const auth = useSelector((state) => state.auth.value);
    const username = auth?.username;
    const fullName = auth?.fullName;

    const [queryDate, setQueryDate] = useState(new moment());
    const [assignments, setAssignments] = useState(null);
    useEffect(() => {
        http()
            .get(`/assignments/calendar`, {
                params: {
                    date: queryDate.toJSON()
                }
            })
            .then((res) => setAssignments(res.data))
            .catch((err) => console.error(err));
    }, [queryDate]);

    const dateCellRender = (date) => {
        return (<ul style={{margin: 0, padding: 0, listStyle: "none"}}>
            {assignments
                .filter(assignment => moment(assignment.endTime).isSame(date, 'day'))
                .map(assignment => <li key={assignment.id}>
                    <Badge status={moment().isAfter(assignment.endTime) ? "error" : "success"}
                           text={assignment.title} onClick={() => history.push(`/assignments/${assignment.slug}`)}/>
                </li>)}
        </ul>)
    };

    return (
        <>
            <Typography.Title level={2}>
                <HomeOutlined/> 系统主页
            </Typography.Title>
            <p>欢迎访问SICP Online Judge，您已经以 {username} {fullName} 的身份登录。</p>
            {assignments && <Calendar dateCellRender={dateCellRender} onChange={setQueryDate}/>}
        </>
    );
};

export default Welcome;
