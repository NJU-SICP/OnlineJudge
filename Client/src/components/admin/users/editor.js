import React, {useEffect, useState} from "react";
import {useHistory, useParams} from "react-router-dom";
import http from "../../../http";

import {Typography, message, Popconfirm, Button} from "antd";
import {DeleteOutlined} from "@ant-design/icons";
import AdminUserForm from "./form";

const AdminUserEditor = () => {
    const {id} = useParams();
    const history = useHistory();
    const [user, setUser] = useState(null);
    const [disabled, setDisabled] = useState(false);

    useEffect(() => {
        http()
            .get(`/users/${id}`)
            .then((res) => setUser({
                ...res.data,
                id: id,
                password: null,
                oldPassword: res.data.password
            }))
            .catch((err) => console.error(err));
    }, [id]);

    const updateUser = (values) => {
        setDisabled(true);
        http()
            .put(`/users/${id}`, {
                username: values.username,
                password: values.password ?? user.oldPassword,
                authorities: [],
                expires: values.expires,
                enabled: values.enabled,
                locked: values.locked,
                fullName: values.fullName,
                ring: values.ring
            })
            .then(() => {
                message.success("修改用户成功！");
                history.push("/admin/users");
            })
            .catch((err) => console.error(err));
    };

    const deleteUser = () => {
        setDisabled(true);
        http()
            .delete(`/users/${id}`)
            .then(() => {
                message.success("删除用户成功！");
                history.push("/admin/users");
            })
            .catch((err) => {
                console.error(err);
                setDisabled(false);
            });
    };

    return (
        <>
            <Typography.Title level={2}>
                编辑用户
                <Popconfirm title="确定要删除用户吗？" onConfirm={deleteUser}
                            okText="删除" okType="danger" cancelText="取消">
                    <Button style={{float: "right"}} type="danger" disabled={disabled}>
                        <DeleteOutlined/> 删除用户
                    </Button>
                </Popconfirm>
            </Typography.Title>
            {!!user
                ? <AdminUserForm initialValues={user} onFinish={updateUser} disabled={disabled}/>
                : <p>加载数据中</p>
            }
        </>
    );
};

export default AdminUserEditor;
