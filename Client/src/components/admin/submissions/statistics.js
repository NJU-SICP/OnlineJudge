import React, {useEffect, useState} from "react";
import {Col, Row, Statistic, Typography} from "antd";
import http from "../../../http";

const AdminSubmissionStatistics = ({assignment}) => {
    const [count, setCount] = useState(null);
    const [submitted, setSubmitted] = useState(null);
    const [notSubmitted, setNotSubmitted] = useState(null);
    const [averageScore, setAverageScore] = useState(null);
    const [maximumScore, setMaximumScore] = useState(null);

    useEffect(() => {
        http()
            .get(`/submissions/count`, {
                params: {
                    assignmentId: assignment.id
                }
            })
            .then((res) => setCount(res.data))
            .catch((err) => console.error(err));
        http()
            .get(`/submissions/users/count`, {
                params: {
                    assignmentId: assignment.id,
                    submitted: true,
                    role: "ROLE_STUDENT"
                }
            })
            .then((res) => setSubmitted(res.data))
            .catch((err) => console.error(err));
        http()
            .get(`/submissions/users/count`, {
                params: {
                    assignmentId: assignment.id,
                    submitted: false,
                    role: "ROLE_STUDENT"
                }
            })
            .then((res) => setNotSubmitted(res.data))
            .catch((err) => console.error(err));
    }, [assignment]);

    return (<>
        <Typography.Title level={3}>提交信息</Typography.Title>
        <Row>
            <Col span={6}>
                <Statistic title="提交数量" loading={count === null} value={count} suffix="次"/>
            </Col>
            <Col span={6}>
                <Statistic title="提交人数（仅含学生）" loading={submitted === null || notSubmitted === null}
                           value={submitted} suffix={`/ ${submitted + notSubmitted} 人`}/>
            </Col>
            <Col span={6}>
                <Statistic title="未提交人数（仅含学生）" loading={notSubmitted === null}
                           value={notSubmitted} suffix={`人`}/>
            </Col>
        </Row>
    </>);
};

export default AdminSubmissionStatistics;
