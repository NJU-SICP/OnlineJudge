import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import moment from "moment";
import {saveAs} from "file-saver";
import http from "../../../http";

import {
    Affix,
    Button,
    Card,
    Checkbox,
    Col, Divider,
    Form,
    message,
    Pagination,
    Popconfirm,
    Row,
    Skeleton,
    Table,
    Typography
} from "antd";
import {ArrowRightOutlined, DownloadOutlined, RedoOutlined} from "@ant-design/icons";
import SubmissionTimeline from "../../submissions/timeline";
import AdminSubmissionGrader from "./grader";
import AdminUserInfo from "../users/info";
import AdminUserSearch from "../users/search";

const AdminSubmissionTable = ({assignment}) => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();

    const [queryUserId, setQueryUserId] = useState(null);
    const [queryGraded, setQueryGraded] = useState(null);
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
                    userId: queryUserId,
                    assignmentId: assignment.id,
                    graded: queryGraded,
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
    }, [assignment, queryUserId, queryGraded, location.search]);

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
                graded: true,
                result: {
                    score: values.score ?? values.details.reduce((sum, result) => sum + result.score, 0),
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

    const deleteSubmission = () => {
        setDisabled(true);
        http()
            .delete(`/submissions/${selected.id}`)
            .then(() => {
                message.info("删除提交成功！");
                setSelected(null);
            })
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
    };

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
            render: (time) => moment(time).format("MM-DD HH:mm")
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
            <Row>
                <Form layout="inline">
                    <Form.Item label="根据学生查询">
                        <AdminUserSearch onSelect={(value, option) => setQueryUserId(option.user.id)}/>
                    </Form.Item>
                    <Form.Item label="根据结果查询">
                        <Checkbox.Group options={[
                            {label: "未评分", value: false},
                            {label: "已评分", value: true}
                        ]} defaultValue={[false, true]} onChange={(values) => {
                            setQueryGraded(values.length > 1 ? null : values[0]);
                        }}/>
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" disabled={queryUserId === null && queryGraded === null}
                                onClick={() => {
                                    setQueryUserId(null);
                                    setQueryGraded(null);
                                }}>清除搜索条件</Button>
                    </Form.Item>
                </Form>
            </Row>
            <Divider/>
            {!page
                ? <Skeleton/>
                : <Row gutter={10}>
                    <Col md={24} xl={9} style={{marginBottom: "1em"}}>
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
                    <Col md={24} xl={15}>
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
                                                                   onDelete={deleteSubmission}
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
