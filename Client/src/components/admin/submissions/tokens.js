import React, {useCallback, useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import moment from "moment";
import qs from "qs";
import http from "../../../http";

import {Button, Divider, Form, message, Pagination, Popconfirm, Skeleton, Table, Typography} from "antd";
import {AuditOutlined} from "@ant-design/icons";
import AdminUserInfo from "../users/info";
import AdminAssignmentInfo from "../assignments/info";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";

const AdminSubmissionTokens = () => {
    const location = useLocation();
    const history = useHistory();

    const [form] = Form.useForm();
    const [page, setPage] = useState(null);
    const [token, setToken] = useState(null);
    const [disabled, setDisabled] = useState(false);

    const fetchTokens = useCallback(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/submissions/tokens`, {
                params: {
                    page: page - 1
                }
            })
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    }, [location.search]);
    useEffect(fetchTokens, [location.search, fetchTokens]);

    const createToken = (values) => {
        setDisabled(true);
        http()
            .post(`/submissions/tokens`, {
                userId: values.userId,
                assignmentId: values.assignmentId
            })
            .then((res) => {
                message.success("创建密钥成功！");
                setToken(res.data.token);
                fetchTokens();
            })
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
    };

    const deleteToken = (id) => {
        http()
            .delete(`/submissions/tokens/${id}`)
            .then(() => {
                message.success("删除密钥成功！");
                fetchTokens();
            })
            .catch((err) => console.error(err));
    };

    const columns = [
        {
            title: "密钥ID",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "密钥值",
            key: "token",
            dataIndex: "token",
            render: (token) => <code>{token}</code>
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
            title: "签发人",
            key: "issuedBy",
            dataIndex: "issuedBy",
            render: (id) => <AdminUserInfo userId={id}/>
        },
        {
            title: "签发时间",
            key: "issuedAt",
            dataIndex: "issuedAt",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "操作",
            key: "actions",
            render: (record) => <Popconfirm title="确定要删除密钥吗？" onConfirm={() => deleteToken(record.id)}>
                <Button type="link" size="small">删除</Button>
            </Popconfirm>
        }
    ]

    return (<>
        <Typography.Title level={2}>
            <AuditOutlined/> 提交密钥管理
        </Typography.Title>
        <Typography.Text>
            密钥可以用于绕过提交次数、截止时间检测进行作业提交，使用一次后便会被删除，请小心管理。
        </Typography.Text>
        <Form style={{marginTop: "2em"}} form={form} layout="inline" onFinish={createToken}>
            <Form.Item name="userId" label="用户" rules={[{required: true, message: "请输入用户"}]}>
                <AdminUserSearch disabled={disabled}
                                 onSelect={(value, option) => form.setFieldsValue({userId: option.user.id})}/>
            </Form.Item>
            <Form.Item name="assignmentId" label="作业" rules={[{required: true, message: "请输入作业"}]}>
                <AdminAssignmentSearch disabled={disabled}
                    onSelect={(value, option) => form.setFieldsValue({assignmentId: option.assignment.id})}/>
            </Form.Item>
            <Form.Item>
                <Button type="primary" htmlType="submit" disabled={disabled}>创建提交密钥</Button>
            </Form.Item>
            {token &&
            <Form.Item>
                已创建的密钥：<code>{token}</code>
            </Form.Item>
            }
        </Form>
        <Divider/>
        {!page
            ? <Skeleton/>
            : <>
                <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false}/>
                <div style={{float: "right", marginTop: "1em"}}>
                    <Pagination current={page.number + 1} pageSize={page.size}
                                total={page.totalElements}
                                onChange={(p) => history.push({
                                    pathname: location.pathname,
                                    search: `?page=${p}`
                                })}/>
                </div>
            </>}
    </>);
};

export default AdminSubmissionTokens;
