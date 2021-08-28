import React, {useState} from "react";
import {useHistory} from "react-router-dom";
import http from "../../../http";

import {Button, Form, Input, message, Typography} from "antd";
import {CheckOutlined} from "@ant-design/icons";

const AdminUserImport = () => {
    const history = useHistory();

    const [disabled, setDisabled] = useState(false);

    const importUsers = (values) => {
        setDisabled(true);
        const infos = values.infos.split('\n');
        const users = [];
        let prompt = "";
        for (const info of infos) {
            if (info.trim() === "") continue;
            const parts = info.trim().split(',');
            if (parts.length === 2 || parts.length === 3) {
                const user = {
                    username: parts[0],
                    fullName: parts[1],
                    password: parts[2] ?? parts[0],
                    roles: ["ROLE_STUDENT"],
                    expires: null,
                    enabled: true,
                    locked: false
                };
                users.push(user);
                prompt += `${user.username} - ${user.fullName} - ${user.password === user.username ? "学号密码" : "自定密码"}\n`;
            } else {
                message.error(`数据格式有误，请检查：${parts}`);
                setDisabled(false);
                return;
            }
        }

        if (window.confirm(`请确认要导入的学生信息，如学生已存在不会重复创建，按确认提交数据：\n${prompt}`)) {
            http()
                .post(`/users/import`, users)
                .then((res) => {
                    message.info(`成功导入${res.data.length}位用户！`);
                    history.push("/admin/users");
                })
                .catch((err) => console.error(err))
                .finally(() => setDisabled(false));
        } else {
            setDisabled(false);
        }
    };

    return (<>
        <Typography.Title level={2}>导入用户</Typography.Title>
        <Form onFinish={importUsers}>
            <Form.Item name="infos">
                <Input.TextArea rows={20} disabled={disabled}
                                placeholder={`学生信息CSV格式：学号,姓名,密码（可选），导入用户均为选课学生角色。例如：\n` +
                                `171860508,张天昀\n171860508,张天昀,password`}/>
            </Form.Item>
            <Form.Item>
                <Button type="primary" htmlType="submit" style={{width: "100%"}} disabled={disabled}>
                    <CheckOutlined/> 提交数据
                </Button>
            </Form.Item>
        </Form>
    </>);
};

export default AdminUserImport;
