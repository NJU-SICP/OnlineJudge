import React, {useEffect, useState} from "react";
import {Col, Row, Statistic, Typography} from "antd";
import http from "../../../http";

const AdminSubmissionStatistics = ({assignment}) => {
    const [count, setCount] = useState(null);
    const [graded, setGraded] = useState(null);
    const [submitted, setSubmitted] = useState(null);
    const [notSubmitted, setNotSubmitted] = useState(null);
    const [statistics, setStatistics] = useState(null);

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
            .get(`/submissions/count`, {
                params: {
                    assignmentId: assignment.id,
                    graded: true
                }
            })
            .then((res) => setGraded(res.data))
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
        http()
            .get(`/submissions/scores/statistics`, {
                params: {
                    assignmentId: assignment.id
                }
            })
            .then((res) => setStatistics(res.data))
            .catch((err) => console.error(err));
    }, [assignment]);

    return (<>
        <Typography.Title level={3}>提交信息</Typography.Title>
        <Row>
            <Col span={8}>
                <Statistic title="提交数量" loading={count === null} value={count} suffix="次"/>
            </Col>
            <Col span={8}>
                <Statistic title="已提交人数（仅含学生）" loading={submitted === null || notSubmitted === null}
                           value={submitted} suffix="人"/>
            </Col>
            <Col span={8}>
                <Statistic title="未提交人数（仅含学生）" loading={notSubmitted === null}
                           value={notSubmitted} suffix="人"/>
            </Col>
        </Row>
        <Row style={{marginTop: "1em"}}>
            <Col span={8}>
                <Statistic title="已评分数量" loading={graded === null} value={graded} suffix="次"/>
            </Col>
            <Col span={8}>
                <Statistic title="平均得分" loading={statistics === null}
                           value={statistics && Number(statistics.average).toFixed(1)}/>
            </Col>
            <Col span={8}>
                <Statistic title="最高得分" loading={statistics === null} value={statistics && statistics.max}/>
            </Col>
        </Row>
    </>);
};

export default AdminSubmissionStatistics;
