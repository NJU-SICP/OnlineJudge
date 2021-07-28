import React, {useEffect} from "react";
import {Button, Divider, Form, Input, InputNumber, Space} from "antd";
import {CheckOutlined, MinusCircleOutlined, PlusOutlined} from "@ant-design/icons";

const AdminSubmissionGrader = ({totalScore, submission, onFinish, disabled}) => {
    const [form] = Form.useForm();

    useEffect(() => {
        const fields = form.getFieldsValue();
        if (submission.score !== null) {
            fields.score = submission.score;
            fields.message = submission.message;
            fields.results = submission.results.map((result) => ({
                title: result.title,
                score: result.score,
                message: result.message
            }));
        } else {
            fields.score = fields.message = null;
            if (fields.results === undefined) {
                fields.results = [];
            } else {
                fields.results.forEach((result) => result.score = result.message = null);
            }
        }
        form.setFieldsValue(fields);
    }, [submission, form]);

    return (
        <Form form={form} onFinish={onFinish}>
            <Form.List name="results">
                {(fields, {add, remove}, {errors}) => (
                    <>
                        {fields.map((field, index) => (
                            <Space key={field.key} align="baseline">
                                <Form.Item label={`项目 ${index + 1}`} name={[field.name, "title"]}
                                           rules={[{required: true, message: "请输入项目名称"}]}>
                                    <Input disabled={disabled}/>
                                </Form.Item>
                                <Form.Item label="分数" name={[field.name, "score"]}
                                           rules={[{required: true, message: "请输入项目分数"}]}>
                                    <InputNumber min={0} max={totalScore ?? 100} disabled={disabled}/>
                                </Form.Item>
                                <Form.Item label="评语" name={[field.name, "message"]}>
                                    <Input disabled={disabled}/>
                                </Form.Item>
                                <MinusCircleOutlined disabled={disabled} onClick={() => remove(field.name)}/>
                            </Space>
                        ))}
                        <Button type="dashed" style={{width: "100%"}} disabled={disabled}
                                onClick={() => add()} icon={<PlusOutlined/>}>
                            添加评分项目
                        </Button>
                    </>
                )}
            </Form.List>
            <Divider/>
            <Form.Item name="score" label="总分">
                <InputNumber min={0} max={totalScore ?? 100} style={{width: "100%"}} disabled={disabled}
                             placeholder={`请输入 0 到 ${totalScore} 之间的整数作为该次提交的总分，如不输入则为各项目之和`}/>
            </Form.Item>
            <Form.Item name="message" label="评语">
                <Input.TextArea placeholder="请输入作业评语（可选）" disabled={disabled}/>
            </Form.Item>
            <Form.Item style={{marginBottom: "unset"}}>
                <Button type="primary" htmlType="submit" style={{width: "100%"}} disabled={disabled}>
                    <CheckOutlined/> 提交数据
                </Button>
            </Form.Item>
        </Form>
    )
};

export default AdminSubmissionGrader;
