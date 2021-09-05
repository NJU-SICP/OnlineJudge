import React, {useCallback, useEffect, useState} from "react";
import {useHistory, useLocation, useParams} from "react-router-dom";
import qs from "qs";
import http from "../../http";

import {Button, Col, Divider, Form, Input, message, Popover, Row, Skeleton, Statistic, Typography, Upload} from "antd";
import {
    AuditOutlined,
    BookOutlined,
    CloudServerOutlined,
    DeleteOutlined,
    InboxOutlined,
    StopOutlined
} from "@ant-design/icons";
import moment from "moment";
import SubmissionTable from "../submissions/table";
import {useSelector} from "react-redux";

const AssignmentView = () => {
    const auth = useSelector((state) => state.auth.value);
    const {id} = useParams();
    const history = useHistory();
    const location = useLocation();

    const [assignment, setAssignment] = useState(null);
    const [submissionsPage, setSubmissionsPage] = useState(null);
    const [disabled, setDisabled] = useState(false);
    const [token, setToken] = useState(null);

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

    const fetchSubmissions = useCallback(() => {
        if (!assignment) return;
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/submissions`, {
                params: {
                    userId: auth.userId,
                    assignmentId: assignment.id,
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
    }, [assignment, location.search, auth]);

    useEffect(fetchSubmissions, [id, auth, location.search, fetchSubmissions]);
    useEffect(() => {
        if (submissionsPage &&
            submissionsPage.content.filter(s => s.result === null || s.result.retryAt != null).length > 0) {
            setTimeout(fetchSubmissions, 3000);
        }
    }, [submissionsPage, fetchSubmissions]);

    const beforeUpload = (file) => {
        return window.confirm(`请确认提交作业和文件名称：\n`
            + `作业：${assignment.title}\n`
            + `文件：${file.name}\n`
            + `点击“确定”提交作业，点击“取消”取消提交。`
            + (!token ? `` : `\n\n提交后提交密钥会失效。`));
    };

    const createSubmission = ({file, onProgress, onSuccess, onError}) => {
        setDisabled(true);
        const formData = new FormData();
        formData.append("assignmentId", id);
        formData.append("file", file);
        if (typeof token === `string`) formData.append("token", token);
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
                        totalElements: oldSubmissionPage.totalElements + 1,
                        content: [{
                            ...res.data,
                            index: oldSubmissionPage.content.length === 0 ? 1 : (oldSubmissionPage.content[0].index + 1)
                        }, ...oldSubmissionPage.content.slice(0, 20 - 1)]
                    };
                });
                if (token) {
                    setToken(null);
                }
            })
            .catch((err) => {
                console.error(err);
                if (token && err.response && err.response.status === 400) {
                    message.error(`提交失败：${err.response.data.message}`);
                    setToken(null);
                }
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
                        <div style={{float: "right"}}>
                            <Button type="link" onClick={() => history.push(`/assignments/${id}/backups`)}>
                                <CloudServerOutlined/> 查看备份列表
                            </Button>
                            {!token
                                ? <>
                                    <Popover placement="left" trigger="click" title="输入提交密钥" content={<>
                                        <Form layout="inline" onFinish={(values) => setToken(values.token)}>
                                            <Form.Item name="token" label="密钥"
                                                       rules={[{required: true, message: "请输入密钥"}]}>
                                                <Input/>
                                            </Form.Item>
                                            <Form.Item>
                                                <Button type="primary" htmlType="submit">确定</Button>
                                            </Form.Item>
                                        </Form>
                                    </>}>
                                        <Button type="text">
                                            <AuditOutlined/> 使用提交密钥
                                        </Button>
                                    </Popover>
                                </>
                                : <>
                                    <Button type="text" danger onClick={() => setToken(null)}>
                                        <DeleteOutlined/> 删除提交密钥
                                    </Button>
                                </>}
                        </div>
                    </Typography.Title>
                    <Row style={{margin: "2em auto"}}>
                        <Col span={6}>
                            <Statistic title="截止日期" value={moment(assignment.endTime).format("YYYY-MM-DD HH:mm")}/>
                        </Col>
                        <Col span={6}>
                            <Statistic title="总评占比" value={assignment.percentage} suffix="%"/>
                        </Col>
                        <Col span={6}>
                            <Statistic title="提交文件"
                                       value={`${assignment.submitFileName}${assignment.submitFileType} (${assignment.submitFileSize} MiB)`}/>
                        </Col>
                        <Col span={6}>
                            <Statistic title="提交次数" loading={!submissionsPage}
                                       value={submissionsPage?.totalElements ?? 0}
                                       suffix={assignment.submitCountLimit <= 0 ? "次" : `/ ${assignment.submitCountLimit} 次`}/>
                        </Col>
                    </Row>
                    <Upload.Dragger style={{maxHeight: "5em"}} name="file" accept={assignment.submitFileType}
                                    beforeUpload={beforeUpload} customRequest={createSubmission} maxCount={1}
                                    disabled={!token && (assignment.ended || assignment.submitCountLimit === 0 || disabled)}>
                        {(assignment.ended && !token)
                            ? <Typography.Text disabled><StopOutlined/> 作业已截止，无法提交</Typography.Text>
                            : <>
                                {(assignment.submitCountLimit === 0 && !token)
                                    ? <Typography.Text type="danger"><StopOutlined/> 此作业不允许自行提交</Typography.Text>
                                    : <Typography.Text>
                                        <InboxOutlined/> 点击或将文件拖拽到此处上传提交
                                        {!token && assignment.submitCountLimit > 0 &&
                                        <span>（剩余{assignment.submitCountLimit - (submissionsPage && submissionsPage.totalElements ? submissionsPage.totalElements : 0)}次提交机会）</span>
                                        }
                                        {!!token && <Typography.Text type="danger">（使用提交密钥）</Typography.Text>}
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
