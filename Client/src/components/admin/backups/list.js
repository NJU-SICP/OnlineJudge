import React, {useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import moment from "moment";
import http from "../../../http";

import {Button, Divider, Form, Pagination, Skeleton, Table, Typography} from "antd";
import {CloudServerOutlined} from "@ant-design/icons";
import AdminUserInfo from "../users/info";
import AdminAssignmentInfo from "../assignments/info";
import Download from "../../download";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";

const AdminBackupList = () => {
    const history = useHistory();
    const location = useLocation();

    const [queryUserId, setQueryUserId] = useState(null);
    const [queryAssignmentId, setQueryAssignmentId] = useState(null);
    const [page, setPage] = useState(null);


    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/backups`, {
                params: {
                    userId: queryUserId,
                    assignmentId: queryAssignmentId,
                    page: page - 1
                }
            })
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    }, [location.search, queryUserId, queryAssignmentId]);

    const columns = [
        {
            title: "备份ID",
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
            title: "作业",
            key: "assignmentId",
            dataIndex: "assignmentId",
            render: (id) => <AdminAssignmentInfo assignmentId={id}/>
        },
        {
            title: "文件",
            key: "key",
            dataIndex: "key",
            render: (key) => key.replace(/backups\//, "")
        },
        {
            title: "测试内容",
            key: "question",
            dataIndex: "analytics",
            render: (analytics) => {
                if (!analytics || !analytics.history) {
                    return 'N/A';
                }
                const question = analytics.history.question;
                const attempts = analytics.history.all_attempts;
                return question && question.length > 0
                    ? `${question} (${analytics.history.questions[question[0]]?.attempts}, ${attempts})`
                    : `ALL (${attempts})`;
            }
        },
        {
            title: "生成时间",
            key: "time",
            dataIndex: "analytics",
            render: (analytics) => analytics?.time ? moment(analytics?.time).format("YYYY-MM-DD HH:mm:ss") : null
        },
        {
            title: "上传时间",
            key: "createdAt",
            dataIndex: "createdAt",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => <>
                <Download link={`/backups/${record.id}/download`} name={record.key}/>
            </>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <CloudServerOutlined/> 备份查询
            </Typography.Title>
            <Form layout="inline">
                <Form.Item label="根据用户搜索">
                    <AdminUserSearch onSelect={(text, option) => setQueryUserId(option.user.id)}/>
                </Form.Item>
                <Form.Item label="根据作业搜索">
                    <AdminAssignmentSearch onSelect={(text, option) => setQueryAssignmentId(option.assignment.id)}/>
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
            <Divider/>
            {!page
                ? <Skeleton/>
                : <>
                    <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false}/>
                    <div style={{float: "right", marginTop: "1em"}}>
                        <Pagination current={page.number + 1} pageSize={page.size} total={page.totalElements}
                                    onChange={(p) => history.push({
                                        pathname: location.pathname,
                                        search: `?page=${p}`
                                    })}/>
                    </div>
                </>}
        </>
    );
};

export default AdminBackupList;
