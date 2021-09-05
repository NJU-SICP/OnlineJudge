import React, {useEffect, useState} from "react";
import {Button, Col, Drawer, Row, Statistic, Typography} from "antd";
import http from "../../../http";
import AdminSubmissionUserList from "./userlist";
import Download from "../../download";

const AdminSubmissionStatistics = ({assignment}) => {
    const [count, setCount] = useState(null);
    const [graded, setGraded] = useState(null);
    const [submitted, setSubmitted] = useState(null);
    const [notSubmitted, setNotSubmitted] = useState(null);
    const [statistics1, setStatistics1] = useState(null);
    const [statistics2, setStatistics2] = useState(null);

    const [submittedDrawerVisible, setSubmittedDrawerVisible] = useState(false);
    const [notSubmittedDrawerVisible, setNotSubmittedDrawerVisible] = useState(false);

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
                    assignmentId: assignment.id,
                    unique: false
                }
            })
            .then((res) => setStatistics1(res.data))
            .catch((err) => console.error(err));
        http()
            .get(`/submissions/scores/statistics`, {
                params: {
                    assignmentId: assignment.id,
                    unique: true
                }
            })
            .then((res) => setStatistics2(res.data))
            .catch((err) => console.error(err));
    }, [assignment]);

    return (<>
        <Typography.Title level={3}>
            提交信息
            <div style={{float: "right"}}>
                <Download link={`/submissions/export?assignmentId=${assignment.id}`}
                          name={`submissions-export-${assignment.slug}.zip`} title={"导出全部提交"} type="primary"
                          size="normal"/>
            </div>
        </Typography.Title>
        <Row>
            <Col span={8}>
                <Statistic title="已评分 / 提交总数量（所有用户）" loading={count === null || graded === null}
                           value={`${graded} / ${count}`} suffix="次"/>
            </Col>
            <Col span={8}>
                <Statistic title="已提交人数（仅含学生）" loading={submitted === null || notSubmitted === null} value={submitted}
                           suffix={<>人 <Button type="link" size="small"
                                               onClick={() => setSubmittedDrawerVisible(true)}>查看列表</Button></>}/>
            </Col>
            <Col span={8}>
                <Statistic title="未提交人数（仅含学生）" loading={notSubmitted === null} value={notSubmitted}
                           suffix={<>人 <Button type="link" size="small"
                                               onClick={() => setNotSubmittedDrawerVisible(true)}>查看列表</Button></>}/>
            </Col>
        </Row>
        <Row style={{marginTop: "1em"}}>
            <Col span={8}>
                <Statistic title="提交最高分" loading={statistics1 === null}
                           value={statistics1 && (statistics1.count ? statistics1.max : "暂无数据")}/>
            </Col>
            <Col span={8}>
                <Statistic title="提交平均分" loading={statistics1 === null}
                           value={statistics1 && (statistics1.count ? Number(statistics1.average).toFixed(2) : "暂无数据")}/>
            </Col>
            <Col span={8}>
                <Statistic title="学生平均分" loading={statistics2 === null}
                           value={statistics2 && (statistics2.count ? Number(statistics2.average).toFixed(2) : "暂无数据")}/>
            </Col>
        </Row>
        <Drawer title="已提交用户列表" width={720} visible={submittedDrawerVisible}
                closable={true} onClose={() => setSubmittedDrawerVisible(false)}>
            <AdminSubmissionUserList assignment={assignment} submitted={true}/>
        </Drawer>
        <Drawer title="未提交用户列表" width={720} visible={notSubmittedDrawerVisible}
                closable={true} onClose={() => setNotSubmittedDrawerVisible(false)}>
            <AdminSubmissionUserList assignment={assignment} submitted={false}/>
        </Drawer>
    </>);
};

export default AdminSubmissionStatistics;
