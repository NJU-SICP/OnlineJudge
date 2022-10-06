import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {Link, useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../../../http";

import {Button, Pagination, Skeleton, Table, Tag, Typography} from "antd";
import {ImportOutlined, PlusOutlined, UserSwitchOutlined} from "@ant-design/icons";

const AdminUserList = () => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();
    const [page, setPage] = useState(null);
    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/users?page=${page - 1}`)
            .then((res) => setPage(res.data))
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
            title: "角色",
            key: "roles",
            dataIndex: "roles",
            render: (roles) => {
                const colors = {
                    "ROLE_ADMIN": "red",
                    "ROLE_TEACHER": "purple",
                    "ROLE_STAFF": "blue",
                    "ROLE_SENIOR": "green"
                };
                return <>{roles.map(role => <Tag key={role} color={colors[role]}>{role}</Tag>)}</>;
            }
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
            title: "Gitlab UID",
            key: "gitlabUserId",
            dataIndex: "gitlabUserId"
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => (<>
                {auth.authorities && auth.authorities.indexOf("OP_USER_UPDATE") >= 0 &&
                    <Link to={`/admin/users/${record.id}`}>
                        <Button type="link" size="small">
                            编辑
                        </Button>
                    </Link>
                }
            </>)
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <UserSwitchOutlined /> 用户管理
                {auth.authorities && auth.authorities.indexOf("OP_USER_CREATE") >= 0 && <>
                    <div style={{float: "right"}}>
                        <Link to="/admin/users/create">
                            <Button>
                                <PlusOutlined /> 添加用户
                            </Button>
                        </Link>
                        <Link to="/admin/users/import">
                            <Button>
                                <ImportOutlined /> 导入用户
                            </Button>
                        </Link>
                    </div>
                </>}
            </Typography.Title>
            {page === null
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

export default AdminUserList;
