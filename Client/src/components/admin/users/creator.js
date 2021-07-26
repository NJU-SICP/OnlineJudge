import React, {useState} from "react";
import {useHistory} from "react-router-dom";
import moment from "moment";
import http from "../../../http";

import {message, Typography} from "antd";
import AdminUserForm from "./form";

const AdminUserCreator = () => {
    const history = useHistory();
    const [disabled, setDisabled] = useState(false);

    const initialValues = {
        username: null,
        password: null,
        fullName: null,
        ring: null,
        expires: null,
        enabled: true,
        locked: false
    };

    const createUser = (values) => {
        setDisabled(true);
        http()
            .post("/users", {
                username: values.username,
                password: values.password,
                authorities: [],
                fullName: values.fullName,
                ring: values.ring,
                expires: !!values.expires ? moment(values.expires) : null,
                enabled: values.enabled,
                locked: values.locked
            })
            .then(() => {
                message.success("创建用户成功！");
                history.push("/admin/users");
            })
            .catch((err) => {
                console.error(err);
                setDisabled(false);
            });
    };

    return (
        <>
            <Typography.Title level={2}>创建用户</Typography.Title>
            <AdminUserForm initialValues={initialValues} onFinish={createUser} disabled={disabled}/>
        </>
    );
};

export default AdminUserCreator;
