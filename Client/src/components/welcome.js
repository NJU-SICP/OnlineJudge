import React, {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import moment from "moment";
import http from "../http";

import {Badge, Calendar, Typography} from "antd";
import {HomeOutlined} from "@ant-design/icons";
import AssignmentScore from "./assignments/score";

const Welcome = () => {
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
                    <Link to={`/assignments/${assignment.slug}`} style={{color: "unset"}}>
                        <Badge status={moment().isAfter(assignment.endTime) ? "error" : "success"} text={assignment.slug} /><br />
                        得分：<AssignmentScore assignmentId={assignment.id} totalScore={assignment.totalScore} />
                    </Link>
                </li>)}
        </ul>)
    };

    return (
        <>
            <Typography.Title level={2}>
                <HomeOutlined /> 系统主页
            </Typography.Title>
            {assignments && <Calendar dateCellRender={dateCellRender} onChange={setQueryDate} />}
        </>
    );
};

export default Welcome;
