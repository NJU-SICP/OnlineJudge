import React, {useEffect, useState} from "react";
import {useLocation, useParams} from "react-router-dom";
import qs from "qs";
import http from "../../http";

import {Col, Divider, message, Row, Skeleton, Statistic, Typography, Upload} from "antd";
import {BookOutlined, InboxOutlined, StopOutlined} from "@ant-design/icons";
import moment from "moment";
import SubmissionTable from "../submissions/table";
import {useSelector} from "react-redux";

const AssignmentView = () => {
    const {id} = useParams();
    const location = useLocation();
    const auth = useSelector((state) => state.auth.value);
    const [assignment, setAssignment] = useState(null);
    const [submissionsPage, setSubmissionsPage] = useState(null);
    const [disabled, setDisabled] = useState(false);

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
    }, [id, auth]);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/submissions`, {
                params: {
                    userId: auth.userId,
                    assignmentId: id,
                    page: page - 1
                }
            })
            .then((res) => {
                const list = [];
                const total = res.data.totalElements;
                res.data.content.forEach((s, index) => {
                    list.push({
                        ...s,
                        index: total - 20 * (page - 1) - index
                    });
                });
                setSubmissionsPage({...res.data, content: list});
            })
            .catch((err) => console.error(err));
    }, [id, auth, location.search]);

    const beforeUpload = (file) => {
        return window.confirm(`请确认提交作业和文件名称：\n作业：${assignment.title}\n文件：${file.name}\n点击“确定”提交作业，点击“取消”取消提交。`);
    };

    const createSubmission = ({file, onProgress, onSuccess, onError}) => {
        setDisabled(true);
        const formData = new FormData();
        formData.append("assignmentId", id);
        formData.append("file", file);
        http()
            .post("/submissions", formData, {
                headers: {"Content-Type": "multipart/form-data"},
                onUploadProgress: (e) => onProgress({percent: e.loaded * 100 / e.total})
            })
            .then((res) => {
                onSuccess(res.data);
                message.success("提交作业成功！");
                setSubmissionsPage(oldSubmissionPage => {
                    return {
                        ...oldSubmissionPage,
                        content: [{
                            ...res.data,
                            index: oldSubmissionPage.content.length === 0 ? 1 : (oldSubmissionPage.content[0].index + 1)
                        }, ...oldSubmissionPage.content.slice(0, 20 - 1)]
                    };
                });
            })
            .catch((err) => {
                console.error(err);
                onError(err);
            })
            .finally(() => setDisabled(false));
    };

    return (
        <>
            {!assignment
                ? <Skeleton/>
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
                            <Statistic title="提交次数" loading={!submissionsPage}
                                       value={submissionsPage?.totalElements ?? 0}
                                       suffix={assignment.submitCountLimit <= 0 ? "次" : `/ ${assignment.submitCountLimit} 次`}/>
                        </Col>
                    </Row>
                    <Upload.Dragger style={{maxHeight: "5em"}} name="file" accept={assignment.submitFileType}
                                    beforeUpload={beforeUpload} customRequest={createSubmission} maxCount={1}
                                    disabled={assignment.ended || assignment.submitCountLimit === 0 || disabled}>
                        {assignment.ended
                            ? <Typography.Text disabled><StopOutlined/> 作业已截止，无法提交</Typography.Text>
                            : <>
                                {assignment.submitCountLimit === 0
                                    ? <Typography.Text type="danger"><StopOutlined/> 此作业不允许自行提交</Typography.Text>
                                    : <Typography.Text>
                                        <InboxOutlined/> 点击或将文件拖拽到此处上传提交
                                        {assignment.submitCountLimit > 0 &&
                                        <span>（剩余{assignment.submitCountLimit - (submissionsPage && submissionsPage.totalElements ? submissionsPage.totalElements : 0)}次提交机会）</span>
                                        }
                                    </Typography.Text>}
                            </>}
                    </Upload.Dragger>
                    {submissionsPage &&
                    <>
                        <Divider/>
                        <SubmissionTable assignment={assignment} page={submissionsPage}/>
                    </>}
                </>
            }
        </>
    );
};

export default AssignmentView;
