import React, {useEffect, useState} from "react";
import {Link, useHistory, useLocation} from "react-router-dom";
import http from "../../../http";
import qs from "qs";
import moment from "moment";

import {Button, Checkbox, Divider, Form, Pagination, Skeleton, Table, Typography} from "antd";
import {EditOutlined, PaperClipOutlined} from "@ant-design/icons";
import AdminUserInfo from "../users/info";
import AdminAssignmentInfo from "../assignments/info";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";
import Download from "../../download";

const AdminSubmissionList = () => {
    const history = useHistory();
    const location = useLocation();

    const [queryUserId, setQueryUserId] = useState(null);
    const [queryUserIdDisabled, setQueryUserIdDisabled] = useState(false);
    const [queryAssignmentId, setQueryAssignmentId] = useState(null);
    const [queryAssignmentIdDisabled, setQueryAssignmentIdDisabled] = useState(false);
    const [queryGraded, setQueryGraded] = useState(null);
    const [page, setPage] = useState(null);

    useEffect(() => {
        const query = qs.parse(location.search, {ignoreQueryPrefix: true});
        const userId = query.userId;
        const assignmentId = query.assignmentId;
        setQueryUserIdDisabled(typeof userId !== `undefined`);
        setQueryAssignmentIdDisabled(typeof assignmentId !== `undefined`);
    }, [location.search]);

    useEffect(() => {
        const query = qs.parse(location.search, {ignoreQueryPrefix: true});
        const userId = query.userId;
        const assignmentId = query.assignmentId;
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/submissions`, {
                params: {
                    userId: userId ?? queryUserId,
                    assignmentId: assignmentId ?? queryAssignmentId,
                    graded: queryGraded,
                    page: page - 1
                }
            })
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    }, [location.search, queryUserId, queryAssignmentId, queryGraded]);

    const columns = [
        {
            title: "提交ID",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "用户",
            key: "userId",
            dataIndex: "userId",
            render: (id) => <AdminUserInfo userId={id} />
        },
        {
            title: "作业",
            key: "assignmentId",
            dataIndex: "assignmentId",
            render: (id) => <AdminAssignmentInfo assignmentId={id} />
        },
        {
            title: "文件",
            key: "key",
            dataIndex: "key",
            render: (key) => key.replace(/submissions\//, "")
        },
        {
            title: "提交时间",
            key: "createdAt",
            dataIndex: "createdAt",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "总得分",
            key: "score",
            dataIndex: "result",
            render: (result) => (result && result.error) ?
                <Typography.Text type="danger">评分失败</Typography.Text> : result?.score
        },
        {
            title: "评分人",
            key: "gradedBy",
            dataIndex: "result",
            render: (result) => result?.gradedBy ?? (result?.score !== null ? "自动评分" : "")
        },
        {
            title: "评分时间",
            key: "gradedAt",
            dataIndex: "result",
            render: (result) => !result?.gradedAt ? "" : moment(result.gradedAt).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) =>
                <>
                    <Download link={`/submissions/${record.id}/download`} name={record.key} />
                    <Link to={`/admin/assignments/${record.assignmentId}/grader?selectedId=${record.id}`}>
                        <Button type="link" size="small">
                            <EditOutlined/> 查看评分
                        </Button>
                    </Link>
                </>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <PaperClipOutlined /> 提交管理
            </Typography.Title>
            <Form layout="inline">
                <Form.Item label="根据用户搜索">
                    <AdminUserSearch disabled={queryUserIdDisabled}
                        onSelect={(text, option) => setQueryUserId(option.user.id)} />
                </Form.Item>
                <Form.Item label="根据作业搜索">
                    <AdminAssignmentSearch disabled={queryAssignmentIdDisabled}
                        onSelect={(text, option) => setQueryAssignmentId(option.assignment.id)} />
                </Form.Item>
                <Form.Item label="根据结果查询">
                    <Checkbox.Group options={[
                        {label: "未评分", value: false},
                        {label: "已评分", value: true}
                    ]} defaultValue={[false, true]} onChange={(values) => {
                        setQueryGraded(values.length > 1 ? null : values[0]);
                    }} />
                </Form.Item>
                <Form.Item>
                    <Button type="primary"
                        disabled={queryUserId === null && queryAssignmentId === null && queryGraded === null}
                        onClick={() => {
                            setQueryUserId(null);
                            setQueryAssignmentId(null);
                            setQueryGraded(null);
                        }}>清除搜索条件</Button>
                </Form.Item>
            </Form>
            <Divider />
            {!page
                ? <Skeleton />
                : <>
                    <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false} />
                    <div style={{float: "right", marginTop: "1em"}}>
                        <Pagination current={page.number + 1} pageSize={page.size}
                            total={page.totalElements}
                            onChange={(p) => history.push({
                                pathname: location.pathname,
                                search: `?page=${p}`
                            })} />
                    </div>
                </>}
        </>
    )
};

export default AdminSubmissionList;
