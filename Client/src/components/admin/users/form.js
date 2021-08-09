import React from "react";

import {Button, Checkbox, DatePicker, Form, Input, Switch} from "antd";
import {CheckOutlined} from "@ant-design/icons";

const AdminUserForm = ({initialValues, onFinish, disabled}) => {
    return (
        <Form initialValues={initialValues} onFinish={onFinish}>
            {!!initialValues.id &&
            <Form.Item name="id" label="用户ID">
                <Input value={initialValues.id} disabled/>
            </Form.Item>
            }
            <Form.Item name="username" label="学号" rules={[{required: true, message: "请输入学号"}]}>
                <Input disabled={disabled || !!initialValues.id}/>
            </Form.Item>
            <Form.Item name="password" label="密码" rules={[{required: !initialValues.id, message: "请输入密码"}]}>
                <Input placeholder={!!initialValues.id ? "如不修改密码则留空" : ""} disabled={disabled}/>
            </Form.Item>
            <Form.Item name="fullName" label="姓名" rules={[{required: true, message: "请输入姓名"}]}>
                <Input disabled={disabled}/>
            </Form.Item>
            <Form.Item name="roles" label="角色" rules={[{required: true, message: "请选择至少一个角色"}]}>
                <Checkbox.Group options={[
                    {label: "管理 [ROLE_ADMIN]", value: "ROLE_ADMIN"},
                    {label: "教师 [ROLE_TEACHER]", value: "ROLE_TEACHER"},
                    {label: "助教 [ROLE_STAFF]", value: "ROLE_STAFF"},
                    {label: "学生 [ROLE_STUDENT]", value: "ROLE_STUDENT"}
                ]} disabled={disabled}/>
            </Form.Item>
            <Form.Item name="expires" label="过期日（如为空值则该用户不会过期）">
                <DatePicker disabled={disabled}/>
            </Form.Item>
            <Form.Item name="enabled" label="用户已启用" valuePropName="checked">
                <Switch disabled={disabled}/>
            </Form.Item>
            <Form.Item name="locked" label="用户已锁定" valuePropName="checked">
                <Switch disabled={disabled}/>
            </Form.Item>
            <Form.Item>
                <Button type="primary" style={{width: "100%"}} htmlType="submit" disabled={disabled}>
                    <CheckOutlined/>提交数据
                </Button>
            </Form.Item>
        </Form>
    );
};

export default AdminUserForm;
