import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useParams} from "react-router-dom";
import moment from "moment";
import http from "../../../http";

import {Typography, message, Popconfirm, Button, Skeleton} from "antd";
import {DeleteOutlined} from "@ant-design/icons";
import AdminUserForm from "./form";

const AdminUserEditor = () => {
    const auth = useSelector((state) => state.auth.value);
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
                password: "",
                expires: !!res.data.expires ? moment(res.data.expires) : null,
                gitlab: !res.data.gitlabUserId
                    ? "未绑定"
                    : `已绑定：UID=${res.data.gitlabUserId}，Email=${res.data.gitlabUserEmail}`
            }))
            .catch((err) => console.error(err));
    }, [id]);

    const updateUser = (values) => {
        setDisabled(true);
        console.log(values.password);
        http()
            .put(`/users/${id}`, {
                username: values.username,
                password: values.password,
                fullName: values.fullName,
                roles: values.roles,
                expires: !!values.expires ? moment(values.expires) : null,
                enabled: values.enabled,
                locked: values.locked,
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
                {auth.authorities && auth.authorities.indexOf("OP_USER_DELETE") >= 0 &&
                <div style={{float: "right"}}>
                    <Popconfirm title="确定要删除用户吗？" onConfirm={deleteUser}
                                okText="删除" okType="danger" cancelText="取消">
                        <Button type="danger" disabled={disabled}>
                            <DeleteOutlined/> 删除用户
                        </Button>
                    </Popconfirm>
                </div>
                }
            </Typography.Title>
            {!user
                ? <Skeleton/>
                : <AdminUserForm initialValues={user} onFinish={updateUser} disabled={disabled}/>}
        </>
    );
};

export default AdminUserEditor;
