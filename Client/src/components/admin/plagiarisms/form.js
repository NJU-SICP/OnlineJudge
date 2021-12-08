import React, {useState} from "react";
import http from "../../../http";

import {Button, Form, Input, Typography} from "antd";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";

const AdminPlagiarismForm = ({onCreate}) => {
    const [form] = Form.useForm();
    const [disabled, setDisabled] = useState(false);
    const [assignment, setAssignment] = useState(null);

    const createPlagiarism = (values) => {
        setDisabled(true);
        http()
            .post("/plagiarisms", {
                userId: values.userId,
                assignmentId: values.assignmentId,
                detail: values.detail,
                score: values.score
            })
            .then(() => onCreate())
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
    };

    return (
        <>
            <Typography.Text>
                添加被判定抄袭的信息后，该用户的该次作业得分即为抄袭处理后的分数，所有提交的原始得分不受影响，请小心管理。
            </Typography.Text>
            <Form style={{marginTop: "1em"}} form={form} layout="inline" onFinish={createPlagiarism}>
                <Form.Item name="userId" label="用户" rules={[{required: true, message: "请输入用户"}]}>
                    <AdminUserSearch disabled={disabled}
                        onSelect={(value, option) => form.setFieldsValue({userId: option.user.id})} />
                </Form.Item>
                <Form.Item name="assignmentId" label="作业" rules={[{required: true, message: "请输入作业"}]}>
                    <AdminAssignmentSearch disabled={disabled}
                        onSelect={(value, option) => {
                            setAssignment(option.assignment);
                            form.setFieldsValue({assignmentId: option.assignment.id});
                        }} />
                </Form.Item>
                <Form.Item name="detail" label="具体信息" rules={[{required: true, message: "请输入具体信息"}]}>
                    <Input.TextArea style={{minWidth: "20em"}} disabled={disabled}
                        placeholder="请在这里输入抄袭的判定结果、处理方式等具体信息。">
                    </Input.TextArea>
                </Form.Item>
                <Form.Item name="score" label="最终得分">
                    <Input type="number" disabled={disabled} placeholder={assignment ? `该作业满分为${assignment.totalScore}` : "此项留空视为0分"} />
                </Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit" disabled={disabled}>提交抄袭信息</Button>
                </Form.Item>
            </Form>
        </>
    );
};

export default AdminPlagiarismForm;
