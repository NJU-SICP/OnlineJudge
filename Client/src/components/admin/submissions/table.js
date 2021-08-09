import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import moment from "moment";
import {saveAs} from "file-saver";
import http from "../../../http";

import {Affix, Button, Card, Col, message, Pagination, Popconfirm, Row, Skeleton, Table, Typography} from "antd";
import {ArrowRightOutlined, DownloadOutlined, RedoOutlined} from "@ant-design/icons";
import SubmissionTimeline from "../../submissions/timeline";
import AdminSubmissionGrader from "./grader";
import AdminUserInfo from "../users/info";

const AdminSubmissionTable = ({assignment}) => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();

    const [page, setPage] = useState(null);
    const [selected, setSelected] = useState(null);
    const [submission, setSubmission] = useState(null);
    const [disabled, setDisabled] = useState(false);

    useEffect(() => {
        const selectedId = qs.parse(location.search, {ignoreQueryPrefix: true}).selectedId;
        if (selectedId != null) {
            setSelected({id: selectedId});
        }
    }, [location.search]);

    useEffect(() => {
        if (assignment == null) return;
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/submissions`, {
                params: {
                    assignmentId: assignment.id,
                    page: page - 1
                }
            })
            .then((res) => {
                const list = [];
                res.data.content.forEach((s, index) => {
                    list.push({
                        ...s,
                        index: index
                    });
                });
                setPage({...res.data, content: list});
            })
            .catch((err) => console.error(err));
    }, [assignment, location.search]);

    useEffect(() => {
        if (!selected) {
            setSubmission(null);
        } else {
            http()
                .get(`/submissions/${selected.id}`)
                .then((res) => setSubmission(res.data))
                .catch((err) => console.error(err));
        }
    }, [selected]);

    const gradeSubmission = (values) => {
        setDisabled(true);
        http()
            .put(`/submissions/${selected.id}`, {
                ...submission,
                result: {
                    score: values.score ?? values.results.reduce((sum, result) => sum + result.score, 0),
                    message: values.message,
                    details: values.details && values.details.length > 0 ? values.details : null,
                    gradedAt: moment(),
                    gradedBy: `${auth.username} ${auth.fullName}`
                }
            })
            .then((res) => {
                setSubmission(res.data);
                const list = [...page.content];
                if (selected.index) {
                    list[selected.index] = {...res.data, id: selected.id};
                }
                setPage({...page, content: list});
            })
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
    };

    // TODO: add delete submission button

    const rejudgeSubmission = () => {
        http()
            .post(`/submissions/${selected.id}/rejudge`)
            .then((res) => {
                message.success("已提交重测请求！");
                setSubmission(res.data);
            })
            .catch((err) => console.error(err));
    };

    const downloadSubmission = () => {
        http()
            .get(`/submissions/${selected.id}/download`, {
                responseType: "blob"
            })
            .then((res) => {
                saveAs(res.data, `${selected.id}${assignment.submitFileType}`);
            })
            .catch((err) => console.error(err));
    };

    const columns = [
        {
            title: "编号",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "用户",
            key: "userId",
            dataIndex: "userId",
            render: (id) => <AdminUserInfo userId={id}/>
        },
        {
            title: "时间",
            key: "createdAt",
            dataIndex: "createdAt",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "得分",
            key: "score",
            dataIndex: "result",
            render: (result) => (result && result.error) ?
                <Typography.Text type="danger">评分失败</Typography.Text> : result?.score
        },
        {
            title: "查看",
            key: "selected",
            render: (record) => <>{selected && record.id === selected.id && <ArrowRightOutlined/>}</>
        }
    ];

    return (
        <>
            {!page
                ? <Skeleton/>
                : <Row gutter={10}>
                    <Col span={9}>
                        <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false}
                               onRow={(record) => {
                                   return {onClick: () => setSelected(record)};
                               }}/>
                        <div style={{float: "right", marginTop: "1em"}}>
                            <Pagination current={page.number + 1} pageSize={page.size}
                                        total={page.totalElements}
                                        onChange={(p) => history.push({
                                            pathname: location.pathname,
                                            search: `?page=${p}`
                                        })}/>
                        </div>
                    </Col>
                    <Col span={15}>
                        {!selected
                            ? <p style={{margin: "1em"}}>在左侧列表中点击某次提交来查看详情。</p>
                            : <Affix offsetTop={10}>
                                {!submission
                                    ? <Skeleton/>
                                    : <Card title={`提交 #${selected.id.substr(-8)}`}
                                            extra={<>
                                                {assignment.grader !== null &&
                                                <Popconfirm title="确定要重新评分该提交吗？" onConfirm={rejudgeSubmission}>
                                                    <Button type="link" size="small" danger>
                                                        <RedoOutlined/> 重新评分
                                                    </Button>
                                                </Popconfirm>
                                                }
                                                <Button type="link" size="small" onClick={downloadSubmission}>
                                                    <DownloadOutlined/> 下载文件
                                                </Button>
                                            </>}>
                                        <SubmissionTimeline id={selected.id} submission={submission}/>
                                        {assignment.grader === null &&
                                        <Card>
                                            <AdminSubmissionGrader totalScore={assignment.totalScore}
                                                                   submission={submission}
                                                                   onFinish={gradeSubmission}
                                                                   disabled={disabled}/>
                                        </Card>
                                        }
                                    </Card>}
                            </Affix>}
                    </Col>
                </Row>
            }
        </>
    );
};

export default AdminSubmissionTable;
