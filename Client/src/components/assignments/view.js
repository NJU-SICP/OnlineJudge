import React, {useCallback, useEffect, useState} from "react";
import {Link, useLocation, useParams} from "react-router-dom";
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
import AssignmentPlagiarism from "./plagiarism";

const AssignmentView = () => {
    const auth = useSelector((state) => state.auth.value);
    const {id} = useParams();
    const location = useLocation();

    const [assignment, setAssignment] = useState(null);
    const [plagiarized, setPlagiarized] = useState(false);
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
        return window.confirm(`???????????????????????????????????????\n`
            + `?????????${assignment.title}\n`
            + `?????????${file.name}\n`
            + `??????????????????????????????????????????????????????????????????`
            + (!token ? `` : `\n\n?????????????????????????????????`));
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
                message.success("?????????????????????");
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
                    message.error(`???????????????${err.response.data.message}`);
                    setToken(null);
                }
                onError(err);
            })
            .finally(() => setDisabled(false));
    };

    return (
        <>
            {!assignment
                ? <Skeleton />
                : <>
                    <Typography.Title level={2}>
                        <BookOutlined /> ?????????{assignment.title}
                        <div style={{float: "right"}}>
                            <Link to={`/assignments/${id}/backups`}>
                                <Button type="link">
                                    <CloudServerOutlined /> ??????????????????
                                </Button>
                            </Link>
                            {!token
                                ? <>
                                    <Popover placement="left" trigger="click" title="??????????????????" content={<>
                                        <Form layout="inline" onFinish={(values) => setToken(values.token)}>
                                            <Form.Item name="token" label="??????"
                                                rules={[{required: true, message: "???????????????"}]}>
                                                <Input />
                                            </Form.Item>
                                            <Form.Item>
                                                <Button type="primary" htmlType="submit">??????</Button>
                                            </Form.Item>
                                        </Form>
                                    </>}>
                                        <Button type="text">
                                            <AuditOutlined /> ??????????????????
                                        </Button>
                                    </Popover>
                                </>
                                : <>
                                    <Button type="text" danger onClick={() => setToken(null)}>
                                        <DeleteOutlined /> ??????????????????
                                    </Button>
                                </>}
                        </div>
                    </Typography.Title>
                    <Row style={{margin: "2em auto"}}>
                        <Col span={6}>
                            <Statistic title="????????????" value={moment(assignment.endTime).format("YYYY-MM-DD HH:mm")} />
                        </Col>
                        <Col span={6}>
                            <Statistic title="????????????" value={assignment.totalScore} suffix="???" />
                        </Col>
                        <Col span={6}>
                            <Statistic title="????????????"
                                value={`${assignment.submitFileName}${assignment.submitFileType} (${assignment.submitFileSize} MiB)`} />
                        </Col>
                        <Col span={6}>
                            <Statistic title="????????????" loading={!submissionsPage}
                                value={submissionsPage?.totalElements ?? 0}
                                suffix={assignment.submitCountLimit <= 0 ? "???" : `/ ${assignment.submitCountLimit} ???`} />
                        </Col>
                    </Row>
                    <Upload.Dragger style={{maxHeight: "5em"}} name="file" accept={assignment.submitFileType}
                        beforeUpload={beforeUpload} customRequest={createSubmission} maxCount={1}
                        disabled={!token && (assignment.ended || assignment.submitCountLimit === 0 || disabled)}>
                        {(assignment.ended && !token)
                            ? <Typography.Text disabled><StopOutlined /> ??????????????????????????????</Typography.Text>
                            : <>
                                {(assignment.submitCountLimit === 0 && !token)
                                    ? <Typography.Text type="danger"><StopOutlined /> ??????????????????????????????</Typography.Text>
                                    : <Typography.Text>
                                        <InboxOutlined /> ?????????????????????????????????????????????
                                        {!token && assignment.submitCountLimit > 0 &&
                                            <span>?????????{assignment.submitCountLimit - (submissionsPage && submissionsPage.totalElements ? submissionsPage.totalElements : 0)}??????????????????</span>
                                        }
                                        {!!token && <Typography.Text type="danger">????????????????????????</Typography.Text>}
                                    </Typography.Text>}
                            </>}
                    </Upload.Dragger>
                    {submissionsPage &&
                        <>
                            <Divider />
                            <AssignmentPlagiarism assignment={assignment} setPlagiarized={setPlagiarized} />
                            <SubmissionTable assignment={assignment} page={submissionsPage} plagiarized={plagiarized} />
                        </>}
                </>
            }
        </>
    );
};

export default AssignmentView;
