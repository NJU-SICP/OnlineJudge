import {useState} from "react";
import {useDispatch} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";

import {Layout, Menu, Form, Input, Button, Alert} from "antd";
import {UserOutlined} from "@ant-design/icons";

import {set} from "../store/auth";
import http from "../http";

const AuthLayout = () => {
    const history = useHistory();
    const location = useLocation();
    const dispatch = useDispatch();
    const [error, setError] = useState(null);

    const attemptLogin = (credentials) => {
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
                    setError("学号或密码不正确，请重试。");
                }
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
                    <Form name="login" style={{maxWidth: "20em"}} onFinish={attemptLogin}>
                        <Form.Item label="学号" name="username" rules={[{required: true, message: "请输入学号"}]}>
                            <Input/>
                        </Form.Item>
                        <Form.Item label="密码" name="password" rules={[{required: true, message: "请输入密码"}]}>
                            <Input.Password/>
                        </Form.Item>
                        <Form.Item>
                            <Button type="primary" htmlType="submit" style={{width: "100%"}}>
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
