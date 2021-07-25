import React, {useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useHistory} from "react-router-dom";
import http from "../http";
import {clear} from "../store/auth";

import {Alert, Button, Divider, Form, Input, message, Typography} from "antd";
import {CheckOutlined, KeyOutlined, UserSwitchOutlined} from "@ant-design/icons";

const UserConfig = () => {
    const auth = useSelector((state) => state.auth.value);
    const dispatch = useDispatch();
    const history = useHistory();
    const [disabled, setDisabled] = useState(false);
    const [error, setError] = useState(null);

    const initialValues = {
        username: auth.username,
        fullName: auth.fullName
    };

    const updatePassword = (values) => {
        setDisabled(true);
        setError(null);
        http()
            .put("/auth/password", {
                oldPassword: values.oldPassword,
                newPassword: values.newPassword
            })
            .then(() => {
                message.info("修改密码成功，请重新登录。");
                dispatch(clear());
                history.push("/");
            })
            .catch((err) => {
                console.error(err);
                if (err.response && err.response.status === 403) {
                    setError(err.response.data);
                }
                setDisabled(false);
            });
    };

    return (
        <>
            <Typography.Title level={2}>
                <UserSwitchOutlined/> 用户信息
            </Typography.Title>
            <Form initialValues={initialValues} style={{maxWidth: "20em", marginTop: "2em"}}>
                <Form.Item name="username" label="学号">
                    <Input bordered={false} readOnly/>
                </Form.Item>
                <Form.Item name="fullName" label="姓名">
                    <Input bordered={false} readOnly/>
                </Form.Item>
            </Form>
            <Divider/>
            <Typography.Title level={2}>
                <KeyOutlined/> 修改密码
            </Typography.Title>
            <Form style={{maxWidth: "20em"}} onFinish={updatePassword}>
                <Alert message="请妥善保管您的密码。" style={{marginBottom: "1.5em"}}/>
                <Form.Item name="oldPassword" label="当前密码" rules={[{required: true, message: "请输入旧密码"}]}>
                    <Input.Password disabled={disabled}/>
                </Form.Item>
                <Form.Item name="newPassword" label="修改密码" rules={[{required: true, message: "请输入新密码"}]}>
                    <Input.Password disabled={disabled}/>
                </Form.Item>
                <Form.Item name="verPassword" label="确认密码" dependencies={["newPassword"]}
                           rules={[{required: true, message: "请确认密码"},
                               ({getFieldValue}) => ({
                                   validator(_, value) {
                                       if (!value || getFieldValue("newPassword") === value) {
                                           return Promise.resolve();
                                       } else {
                                           return Promise.reject(new Error("两次输入的密码不一致"));
                                       }
                                   }
                               })]}>
                    <Input.Password disabled={disabled}/>
                </Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit" disabled={disabled} style={{width: "100%"}}>
                        <CheckOutlined/> 修改密码
                    </Button>
                </Form.Item>
                {error && <Alert message={error} type={"error"} closable onClose={() => setError(null)}/>}
            </Form>
        </>
    );
};

export default UserConfig;
