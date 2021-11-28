import React from "react";

import {Button, DatePicker, Form, Input, InputNumber, Radio, Select} from "antd";
import {CheckOutlined} from "@ant-design/icons";

const AdminAssignmentForm = ({initialValues, onFinish, disabled}) => {
    return (
        <Form initialValues={initialValues} onFinish={onFinish}>
            {!!initialValues.id &&
            <Form.Item name="id" label="作业ID">
                <Input value={initialValues.id} disabled/>
            </Form.Item>
            }
            <Form.Item name="slug" label="代号" rules={[{required: true, message: "请输入代号"}]}>
                <Input value={initialValues.slug} placeholder="如lab00，hw01等" disabled={disabled}/>
            </Form.Item>
            <Form.Item name="title" label="标题" rules={[{required: true, message: "请输入标题"}]}>
                <Input value={initialValues.title} disabled={disabled}/>
            </Form.Item>
            <Form.Item name="rangeTime" label="时间" rules={[{required: true, message: "请输入时间"}]}>
                <DatePicker.RangePicker showTime format="YYYY-MM-DD HH:mm" disabled={disabled}/>
            </Form.Item>
            <Form.Item name="submitFileName" label="文件名称" rules={[{required: true, message: "请输入文件名称"}]}>
                <Input placeholder="不包含扩展名，例如lab00、submit等"/>
            </Form.Item>
            <Form.Item name="submitFileType" label="文件类型" rules={[{required: true, message: "请选择文件类型"}]}>
                <Radio.Group>
                    <Radio value=".pdf">PDF文件（*.pdf）</Radio>
                    <Radio value=".py">Python代码文件（*.py）</Radio>
                    <Radio value=".scm">Scheme代码文件（*.scm）</Radio>
                    <Radio value=".zip">压缩文件夹（*.zip）</Radio>
                </Radio.Group>
            </Form.Item>
            <Form.Item name="submitFileSize" label="文件大小" rules={[{required: true, message: "请输入文件大小"}]}>
                <InputNumber min="1" max="10" formatter={(value) => `${value}MiB`}/>
            </Form.Item>
            <Form.Item name="submitCountLimit" label="提交次数" rules={[{required: true, message: "请选择提交次数"}]}>
                <Select disabled={disabled}>
                    <Select.Option value={-1}>无次数限制</Select.Option>
                    <Select.Option value={0}>不允许提交</Select.Option>
                    <Select.Option value={1}>最多1次</Select.Option>
                    <Select.Option value={16}>最多16次</Select.Option>
                    <Select.Option value={32}>最多32次</Select.Option>
                </Select>
            </Form.Item>
            <Form.Item name="totalScore" label="作业总分" rules={[{required: true, message: "请输入作业总分"}]}>
                <InputNumber min="0"/>
            </Form.Item>
            <Form.Item name="percentage" label="总评占比" rules={[{required: true, message: "请输入总评占比"}]}>
                <InputNumber min="0" max="100" step="0.1" stringMode formatter={(value) => `${value}%`}/>
            </Form.Item>
            <Form.Item>
                <Button type="primary" style={{width: "100%"}} htmlType="submit" disabled={disabled}>
                    <CheckOutlined/>提交数据
                </Button>
            </Form.Item>
        </Form>
    );
};

export default AdminAssignmentForm;
