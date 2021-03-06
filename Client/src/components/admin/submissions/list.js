import React, {useEffect, useState} from "react";
import {Link, useHistory, useLocation} from "react-router-dom";
import {useSelector} from "react-redux";
import http from "../../../http";
import qs from "qs";
import moment from "moment";

import {Button, Checkbox, Divider, Form, Pagination, Skeleton, Table, Typography} from "antd";
import {AuditOutlined, PaperClipOutlined} from "@ant-design/icons";
import AdminUserInfo from "../users/info";
import AdminAssignmentInfo from "../assignments/info";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";

const AdminSubmissionList = () => {
    const auth = useSelector((state) => state.auth.value);
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
            title: "??????ID",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "??????",
            key: "userId",
            dataIndex: "userId",
            render: (id) => <AdminUserInfo userId={id} />
        },
        {
            title: "??????",
            key: "assignmentId",
            dataIndex: "assignmentId",
            render: (id) => <AdminAssignmentInfo assignmentId={id} />
        },
        {
            title: "??????",
            key: "key",
            dataIndex: "key",
            render: (key) => key.replace(/submissions\//, "")
        },
        {
            title: "????????????",
            key: "createdAt",
            dataIndex: "createdAt",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "?????????",
            key: "score",
            dataIndex: "result",
            render: (result) => (result && result.error) ?
                <Typography.Text type="danger">????????????</Typography.Text> : result?.score
        },
        {
            title: "?????????",
            key: "gradedBy",
            dataIndex: "result",
            render: (result) => result?.gradedBy ?? (result?.score !== null ? "????????????" : "")
        },
        {
            title: "????????????",
            key: "gradedAt",
            dataIndex: "result",
            render: (result) => !result?.gradedAt ? "" : moment(result.gradedAt).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "??????",
            key: "actions",
            render: (text, record) =>
                <Link to={`/admin/assignments/${record.assignmentId}/grader?selectedId=${record.id}`}>
                    <Button type="link" size="small">
                        ??????
                    </Button>
                </Link>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <PaperClipOutlined /> ????????????
                {auth.authorities && auth.authorities.indexOf("OP_SUBMISSION_TOKEN_MANAGE") >= 0 &&
                    <div style={{float: "right"}}>
                        <Link to="/admin/submissions/tokens">
                            <Button type="primary">
                                <AuditOutlined /> ??????????????????
                            </Button>
                        </Link>
                    </div>
                }
            </Typography.Title>
            <Form layout="inline">
                <Form.Item label="??????????????????">
                    <AdminUserSearch disabled={queryUserIdDisabled}
                        onSelect={(text, option) => setQueryUserId(option.user.id)} />
                </Form.Item>
                <Form.Item label="??????????????????">
                    <AdminAssignmentSearch disabled={queryAssignmentIdDisabled}
                        onSelect={(text, option) => setQueryAssignmentId(option.assignment.id)} />
                </Form.Item>
                <Form.Item label="??????????????????">
                    <Checkbox.Group options={[
                        {label: "?????????", value: false},
                        {label: "?????????", value: true}
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
                        }}>??????????????????</Button>
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
