import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import moment from "moment";
import {saveAs} from "file-saver";
import http from "../../../http";

import {Affix, Button, Card, Col, Pagination, Row, Skeleton, Table} from "antd";
import {ArrowRightOutlined, DownloadOutlined} from "@ant-design/icons";
import SubmissionTimeline from "../../submissions/timeline";
import AdminSubmissionGrader from "./grader";

const AdminSubmissionTable = ({assignment}) => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();

    const [submissions, setSubmissions] = useState(null);
    const [pagination, setPagination] = useState(null);
    const [selected, setSelected] = useState(null);
    const [submission, setSubmission] = useState(null);
    const [disabled, setDisabled] = useState(false);

    useEffect(() => {
        if (assignment == null) return;
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/repositories/submissions/search/findByAssignmentIdOrderByCreatedAtDesc`, {
                params: {
                    assignmentId: assignment.id,
                    page: page - 1
                }
            })
            .then((res) => {
                const list = [];
                res.data._embedded.submissions.forEach((s, index) => {
                    list.push({
                        ...s,
                        index: index
                    });
                });
                setSubmissions(list);
                setPagination(res.data.page);
            })
            .catch((err) => console.error(err));
    }, [assignment, location.search]);

    useEffect(() => {
        if (!selected) {
            setSubmission(null);
        } else {
            http()
                .get(`/repositories/submissions/${selected.id}`)
                .then((res) => setSubmission(res.data))
                .catch((err) => console.error(err));
        }
    }, [selected]);

    const gradeSubmission = (values) => {
        setDisabled(true);
        http()
            .put(`/repositories/submissions/${selected.id}`, {
                ...submission,
                score: values.score ?? values.results.reduce((sum, result) => sum + result.score, 0),
                message: values.message,
                results: values.results,
                gradedAt: moment(),
                gradedBy: `${auth.username} ${auth.fullName}`
            })
            .then((res) => {
                setSubmission(res.data);
                const list = [...submissions];
                list[selected.index] = {...res.data, id: selected.id};
                setSubmissions(list);
                setSubmission(res.data);
            })
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
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
            render: (id) => <code>{id.substr(-8)}</code>
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
            dataIndex: "score"
        },
        {
            title: "查看",
            key: "selected",
            render: (record) => <>{selected && record.id === selected.id && <ArrowRightOutlined/>}</>
        }
    ];

    return (
        <>
            {(!submissions || !pagination)
                ? <Skeleton/>
                : <Row gutter={10}>
                    <Col span={9}>
                        <Table columns={columns} dataSource={submissions} rowKey="id" pagination={false}
                               onRow={(record) => {
                                   return {onClick: () => setSelected(record)};
                               }}/>
                        <div style={{float: "right", marginTop: "1em"}}>
                            <Pagination current={pagination.number + 1} pageSize={pagination.size}
                                        total={pagination.totalElements}
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
                                            extra={
                                                <Button type="link" size="small" onClick={downloadSubmission}>
                                                    <DownloadOutlined/> 下载文件
                                                </Button>
                                            }>
                                        <SubmissionTimeline id={selected.id} submission={submission}/>
                                        <Card>
                                            <AdminSubmissionGrader totalScore={assignment.totalScore}
                                                                   submission={submission}
                                                                   onFinish={gradeSubmission}
                                                                   disabled={disabled}/>
                                        </Card>
                                    </Card>}
                            </Affix>}
                    </Col>
                </Row>
            }
        </>
    );
};

export default AdminSubmissionTable;
