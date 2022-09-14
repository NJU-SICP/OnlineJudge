import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useLocation} from "react-router-dom";
import qs from "qs";
import http from "../http";

import {Alert, Button, Card, Col, Divider, Form, Input, message, Popconfirm, Row, Skeleton, Typography} from "antd";
import {
    BookOutlined,
    DisconnectOutlined,
    GitlabOutlined,
    LinkOutlined,
    UserSwitchOutlined, WarningOutlined
} from "@ant-design/icons";
import config from "../config";

const UserConfig = () => {
    const auth = useSelector((state) => state.auth.value);
    const location = useLocation();

    const [user, setUser] = useState(null);
    const [error, setError] = useState(null);

    useEffect(() => {
        http()
            .get(`/users/self`)
            .then((res) => setUser(res.data))
            .catch((err) => console.error(err));
    }, []);

    useEffect(() => {
        const error = qs.parse(location.search, {ignoreQueryPrefix: true}).error;
        if (typeof error !== `undefined`) {
            setError(error);
        }
    }, [location.search]);

    const linkGitlab = () => {
        const redirect = location.pathname;
        const state = `oauth-${btoa("/auth/gitlab/link")}-${btoa(redirect)}`;
        window.location.href = `${config.baseNames.api}/auth/gitlab/login?state=${state}`;
    };

    const unlinkGitlab = () => {
        http()
            .delete(`/auth/gitlab/link`)
            .then((res) => {
                message.success("解除外部登录账户绑定成功！");
                http().get(`/users/self`)
                    .then((res) => setUser(res.data))
                    .catch((err) => console.error(err));
            })
            .catch((err) => console.error(err));
    };

    return (
        <>
            <Typography.Title level={2}>
                <UserSwitchOutlined/> 用户信息
            </Typography.Title>
            {!user
                ? <Skeleton/>
                : <>
                    <Form initialValues={{
                        info: `${user.username} ${user.fullName}`,
                        enroll: `${auth.roles.indexOf("ROLE_STUDENT") >= 0 ? "已选课" : "未选课"}`
                    }} style={{marginTop: "2em"}}>
                        <Form.Item name="info" label="学生">
                            <Input bordered={false} readOnly/>
                        </Form.Item>
                        <Form.Item name="enroll" label="选课">
                            <Input bordered={false} readOnly prefix={<>
                                <Typography.Text type={auth.roles.indexOf("ROLE_STUDENT") >= 0 ? "success" : "danger"}>
                                    {auth.roles.indexOf("ROLE_STUDENT") >= 0
                                        ? <BookOutlined/>
                                        : <WarningOutlined/>}
                                </Typography.Text>
                            </>}/>
                        </Form.Item>
                    </Form>
                </>}
            <Divider/>
            {error && <Alert message={error} type={"error"} style={{marginBottom: "1em"}}
                             closable onClose={() => setError(null)}/>}
            <Row gutter={40}>
                <Col xs={24} sm={24} md={12} xl={12} xxl={8}>
                    <Card>
                        <Typography.Title level={2}>
                            <GitlabOutlined/> 外部登录
                        </Typography.Title>
                        {!user
                            ? <Skeleton/>
                            : <>
                                {!user.gitlabUserId
                                    ? <>
                                        <p>可绑定南京大学代码托管服务（<code>git.nju.edu.cn</code>）账户。</p>
                                        <Popconfirm title="请点击确定，在新页面中登录南京大学代码托管服务，完成绑定。"
                                                    onConfirm={linkGitlab}>
                                            <Button type="primary" style={{width: "100%"}}>
                                                <LinkOutlined/> 绑定账户
                                            </Button>
                                        </Popconfirm>
                                    </>
                                    : <>
                                        <p>已绑定账户：UID={user.gitlabUserId}，Email={user.gitlabUserEmail}</p>
                                        <Popconfirm title="确定要解除绑定吗？"
                                                    okType="danger" onConfirm={unlinkGitlab}>
                                            <Button type="primary" danger style={{width: "100%"}}>
                                                <DisconnectOutlined/> 解除绑定
                                            </Button>
                                        </Popconfirm>
                                    </>
                                }
                            </>}
                    </Card>
                </Col>
            </Row>
        </>
    );
};

export default UserConfig;
