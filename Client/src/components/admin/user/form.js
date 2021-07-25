import React from "react";

import {Button, DatePicker, Form, Input, Select, Switch} from "antd";
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
            <Form.Item name="ring" label="权限组" rules={[{required: true, message: "请选择权限组"}]}>
                <Select placeholder="权限组" disabled={disabled}>
                    <Select.Option value={3}>3 - 学生</Select.Option>
                    <Select.Option value={2}>2 - 助教</Select.Option>
                    <Select.Option value={1}>1 - 管理员</Select.Option>
                    <Select.Option value={0}>0 - 超级管理员</Select.Option>
                </Select>
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
