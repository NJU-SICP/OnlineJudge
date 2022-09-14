import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import moment from "moment";
import config from "../config";

import {Layout, Form, Button, Alert, Typography, Divider, Row, Col, List} from "antd";
import {
    CloseOutlined,
    CopyrightOutlined,
    GitlabOutlined,
    HomeOutlined,
    LinkOutlined,
    UserOutlined
} from "@ant-design/icons";
import Time from "../components/time";
import Version from "../components/version";

const AuthLayout = () => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();
    const [error, setError] = useState(null);

    useEffect(() => {
        const error = qs.parse(location.search, {ignoreQueryPrefix: true}).error;
        if (!!error) {
            setError(error);
        }
    }, [location.search]);

    useEffect(() => {
        if (!auth) return;
        const now = moment();
        const exp = moment(auth.expires);
        if (now.isBefore(exp)) {
            const params = qs.parse(location.search, {ignoreQueryPrefix: true});
            history.push({
                pathname: params.redirect ?? "/",
                search: params.error ? `?error=${params.error}` : null
            });
        }
    }, [auth, history, location.search]);

    const loginWithGitlab = () => {
        const redirect = qs.parse(location.search, {ignoreQueryPrefix: true}).redirect ?? "/";
        const state = `oauth-${btoa("/auth/gitlab/login/callback")}-${btoa(redirect)}`;
        window.location.href = `${config.baseNames.api}/auth/gitlab/login?state=${state}`;
    };

    return (
        <Layout style={{paddingTop: "10vh", paddingBottom: "10vh", paddingLeft: "10vw", paddingRight: "10vw"}}>
            <Typography.Title level={1}>SICP Online Judge</Typography.Title>
            <Typography.Text>
                当前服务器时间：<Time/>
            </Typography.Text>
            <Divider/>
            <Row gutter={24}>
                <Col xs={24} md={16}>
                    <Typography.Title level={2}>
                        <UserOutlined/> 用户登录
                    </Typography.Title>
                    <Form name="login"
                          style={{
                              maxWidth: "27em",
                              marginTop: "2em",
                              marginBottom: "2em",
                              padding: "1em",
                              border: "1px solid #1890ff",
                              borderRadius: "5px"
                          }}>
                        <Form.Item>
                            {!error
                                ? <Alert message="请登录以提交代码或查看成绩。"/>
                                : <Alert message={error} type={"error"} action={
                                    <Button type="text" size="small" style={{padding: "0"}}
                                            onClick={() => setError(null)}>
                                        <CloseOutlined/>
                                    </Button>
                                }/>
                            }
                        </Form.Item>
                        <Form.Item style={{marginBottom: "0.5em"}}>
                            <Button type="ghost" size="large" style={{width: "100%"}} onClick={() => loginWithGitlab()}>
                                <GitlabOutlined/> 使用南京大学代码托管服务登录
                            </Button>
                        </Form.Item>
                    </Form>
                </Col>
                <Col xs={24} md={8}>
                    <Typography.Title level={2}>
                        <LinkOutlined/> 相关链接
                    </Typography.Title>
                    <List>
                        <List.Item>
                            <Typography.Text>
                                <HomeOutlined style={{marginRight: "1em"}}/>
                                <a href="https://nju-sicp.bitbucket.io/" target="_blank" rel="noreferrer">
                                    SICP课程主页
                                </a>
                            </Typography.Text>
                        </List.Item>
                        <List.Item>
                            <Typography.Text>
                                <CopyrightOutlined style={{marginRight: "1em"}}/>
                                <span>南京大学 2021 版权所有</span>
                            </Typography.Text>
                        </List.Item>
                        <List.Item>
                            <Typography.Text>
                                <Version/>
                            </Typography.Text>
                        </List.Item>
                    </List>
                </Col>
            </Row>
        </Layout>
    )
};

export default AuthLayout;
