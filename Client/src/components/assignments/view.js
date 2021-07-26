import React, {useEffect, useState} from "react";
import {useParams} from "react-router-dom";
import http from "../../http";

import {Col, Row, Statistic, Typography, Upload} from "antd";
import {BookOutlined, InboxOutlined, StopOutlined} from "@ant-design/icons";
import moment from "moment";
import {useSelector} from "react-redux";
import config from "../../config";

const AssignmentView = () => {
    const {id} = useParams();
    const auth = useSelector((state) => state.auth.value);
    const [assignment, setAssignment] = useState(null);

    useEffect(() => {
        http()
            .get(`/assignments/${id}`)
            .then((res) => {
                const now = moment();
                const ddl = moment(res.data.endTime);
                setAssignment({
                    ...res.data,
                    ended: now.isAfter(ddl)
                });
            })
            .catch((err) => console.error(err));
    }, [id]);

    return (
        <>
            {!assignment
                ? <p>加载数据中</p>
                : <>
                    <Typography.Title level={2}>
                        <BookOutlined/> 作业：{assignment.title}
                    </Typography.Title>
                    <Row style={{margin: "2em auto"}}>
                        <Col span={6}>
                            <Statistic title="截止日期" value={moment(assignment.endTime).format("YYYY-MM-DD HH:mm")}/>
                        </Col>
                        <Col span={6}>
                            <Statistic title="总评占比" value={assignment.percentage} suffix="%"/>
                        </Col>
                        <Col span={6}>
                            <Statistic title="提交类型" value={assignment.submitFileType}/>
                        </Col>
                        <Col span={6}>
                            <Statistic title="提交次数" value={0}
                                       suffix={assignment.submitCountLimit <= 0 ? "次" : `/ ${assignment.submitCountLimit} 次`}/>
                        </Col>
                    </Row>
                    <Upload.Dragger style={{maxHeight: "5em"}} name="file" accept={assignment.submitFileType}
                                    action={`${config.baseURL}/submissions`}  
                                    disabled={assignment.ended || assignment.submitCountLimit === 0}>
                        {assignment.ended
                            ? <Typography.Text disabled><StopOutlined/> 作业已截止，无法提交</Typography.Text>
                            : <>
                                {assignment.submitCountLimit === 0
                                    ? <Typography.Text type="danger"><StopOutlined/> 此作业不允许自行提交</Typography.Text>
                                    : <Typography.Text>
                                        <InboxOutlined/> 点击或将文件拖拽到此处上传提交
                                        {assignment.submitCountLimit > 0 &&
                                        <span>（剩余{assignment.submitCountLimit}次提交机会）</span>
                                        }
                                    </Typography.Text>}
                            </>}
                    </Upload.Dragger>
                </>
            }
        </>
    );
};

export default AssignmentView;
