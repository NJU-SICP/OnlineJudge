import {useState} from "react";
import {useDispatch} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";

import {Layout, Menu, Form, Input, Button, Alert, Typography} from "antd";
import {UserOutlined} from "@ant-design/icons";

import {set} from "../store/auth";
import http from "../http";

const AuthLayout = () => {
    const history = useHistory();
    const location = useLocation();
    const dispatch = useDispatch();
    const [disabled, setDisabled] = useState(false);
    const [error, setError] = useState(null);

    const attemptLogin = (credentials) => {
        setDisabled(true);
        setError(null);
        http()
            .post("/auth/login", {
                username: credentials.username,
                password: credentials.password
            })
            .then((res) => {
                dispatch(set(res.data));
                const to = qs.parse(location.search, {ignoreQueryPrefix: true}).redirect;
                if (to != null) {
                    history.push(to);
                } else {
                    history.push("/");
                }
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
        <Layout>
            <Layout.Sider width={200} className="site-layout-background">
                <Menu style={{height: '100%', borderRight: 0}} defaultSelectedKeys={["0"]}>
                    <Menu.Item key="0" icon={<UserOutlined/>}>用户登录</Menu.Item>
                </Menu>
            </Layout.Sider>
            <Layout style={{padding: '2em'}}>
                <Layout.Content>
                    <Typography.Title level={2}>
                        <UserOutlined/> 用户登录
                    </Typography.Title>
                    <Form name="login" style={{maxWidth: "20em", marginTop: "2em"}} onFinish={attemptLogin}>
                        <Form.Item label="学号" name="username" rules={[{required: true, message: "请输入学号"}]}>
                            <Input disabled={disabled}/>
                        </Form.Item>
                        <Form.Item label="密码" name="password" rules={[{required: true, message: "请输入密码"}]}>
                            <Input.Password disabled={disabled}/>
                        </Form.Item>
                        <Form.Item>
                            <Button type="primary" htmlType="submit" disabled={disabled} style={{width: "100%"}}>
                                登录
                            </Button>
                        </Form.Item>
                        {error && <Alert message={error} type={"error"} closable onClose={() => setError(null)}/>}
                    </Form>
                </Layout.Content>
            </Layout>
        </Layout>
    )
};

export default AuthLayout;
