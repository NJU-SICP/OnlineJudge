import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../../../http";

import {Button, Divider, Form, Pagination, Popconfirm, Skeleton, Table, Typography} from "antd";
import {HighlightOutlined} from "@ant-design/icons";
import AdminUserInfo from "../users/info";
import AdminAssignmentInfo from "../assignments/info";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";
import AdminPlagiarismForm from "./form";

const AdminPlagiarismList = () => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();

    const [queryUserId, setQueryUserId] = useState(null);
    const [queryAssignmentId, setQueryAssignmentId] = useState(null);
    const [page, setPage] = useState(null);

    const loadPlagiarisms = () => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/plagiarisms`, {
                params: {
                    userId: queryUserId,
                    assignmentId: queryAssignmentId,
                    page: page - 1
                }
            })
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    };
    useEffect(loadPlagiarisms, [location.search, queryUserId, queryAssignmentId]);

    const deletePlagiarism = (plagiarism) => {
        http()
            .delete(`/plagiarisms/${plagiarism.id}`)
            .then(() => loadPlagiarisms())
            .catch((err) => console.error(err));
    };

    const columns = [
        {
            title: "抄袭ID",
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
            title: "具体信息",
            key: "detail",
            dataIndex: "detail"
        },
        {
            title: "最终给分",
            key: "score",
            dataIndex: "score",
            render: (score) => score ?? "无效"
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => <>
                {auth.authorities.indexOf("OP_PLAGIARISM_DELETE") >= 0 && <>
                    <Popconfirm title={`确定要删除抄袭记录${record.id.substr(-8)}吗？`}
                        onConfirm={() => deletePlagiarism(record)}>
                        <Button type="link" size="small">删除</Button>
                    </Popconfirm>
                </>}
            </>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <HighlightOutlined /> 抄袭管理
            </Typography.Title>
            {auth.authorities.indexOf("OP_PLAGIARISM_READ_ALL") >= 0 && <>
                <AdminPlagiarismForm onCreate={loadPlagiarisms} />
                <Divider />
            </>}
            <Form layout="inline">
                <Form.Item label="根据用户搜索">
                    <AdminUserSearch onSelect={(text, option) => setQueryUserId(option.user.id)} />
                </Form.Item>
                <Form.Item label="根据作业搜索">
                    <AdminAssignmentSearch onSelect={(text, option) => setQueryAssignmentId(option.assignment.id)} />
                </Form.Item>
                <Form.Item>
                    <Button type="primary"
                        disabled={queryUserId === null && queryAssignmentId === null}
                        onClick={() => {
                            setQueryUserId(null);
                            setQueryAssignmentId(null);
                        }}>清除搜索条件</Button>
                </Form.Item>
            </Form>
            <Divider />
            {!page
                ? <Skeleton />
                : <>
                    <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false} />
                    <div style={{float: "right", marginTop: "1em"}}>
                        <Pagination current={page.number + 1} pageSize={page.size} total={page.totalElements}
                            onChange={(p) => history.push({
                                pathname: location.pathname,
                                search: `?page=${p}`
                            })} />
                    </div>
                </>}
        </>
    );
};

export default AdminPlagiarismList;
