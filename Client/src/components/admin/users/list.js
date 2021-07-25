import React, {useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../../../http";

import {Button, Pagination, Table, Typography} from "antd";
import {PlusOutlined, UserSwitchOutlined} from "@ant-design/icons";

const AdminUserList = () => {
    const history = useHistory();
    const location = useLocation();
    const [users, setUsers] = useState([]);
    const [pagination, setPagination] = useState(null);
    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/users?sort=username,asc&page=${page - 1}`)
            .then((res) => {
                setUsers(res.data._embedded.users);
                setPagination(res.data.page);
            })
            .catch((err) => console.error(err));
    }, [location.search]);

    const columns = [
        {
            title: "学号",
            key: "username",
            dataIndex: "username"
        },
        {
            title: "姓名",
            key: "fullName",
            dataIndex: "fullName"
        },
        {
            title: "权限组",
            key: "ring",
            dataIndex: "ring",
            render: (ring) => (
                <Typography.Text type={ring < 3 ? "danger" : "primary"}>{ring}</Typography.Text>
            )
        },
        {
            title: "状态",
            key: "status",
            render: (text, record) => {
                if (!record.enabled) {
                    return <Typography.Text type="danger">禁用</Typography.Text>;
                } else if (!record.accountNonLocked) {
                    return <Typography.Text type="danger">锁定</Typography.Text>
                } else if (!record.accountNonExpired) {
                    return <Typography.Text type="danger">过期</Typography.Text>;
                } else {
                    return <Typography.Text type="success">正常</Typography.Text>;
                }
            }
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => (
                <Button type="primary" size="small"
                        onClick={() => history.push(`/admin/users/${record.id}`)}>
                    查看
                </Button>
            )
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <UserSwitchOutlined/> 用户管理
                <Button style={{float: "right"}} onClick={() => history.push("/admin/users/create")}>
                    <PlusOutlined/> 添加用户
                </Button>
            </Typography.Title>
            <Table columns={columns} dataSource={users} rowKey="id" pagination={false}/>
            <div style={{float: "right", marginTop: "1em"}}>
                {!!pagination &&
                <Pagination current={pagination.number + 1} pageSize={pagination.size} total={pagination.totalElements}
                            onChange={(p) => history.push({pathname: location.pathname, search: `?page=${p}`})}/>
                }
            </div>
        </>
    );
};

export default AdminUserList;
