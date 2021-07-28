import React, {useEffect, useState} from "react";
import {useHistory, useParams} from "react-router-dom";
import moment from "moment";
import http from "../../../http";

import {Button, Descriptions, Divider, message, Popconfirm, Skeleton, Typography, Upload} from "antd";
import {DashboardOutlined, EditOutlined, UploadOutlined} from "@ant-design/icons";
import AdminSubmissionTable from "../submissions/table";

const AdminAssignmentGrader = () => {
    const history = useHistory();
    const {id} = useParams();
    const [assignment, setAssignment] = useState(null);
    const [grader, setGrader] = useState(null);
    const [disabled, setDisabled] = useState(null);

    useEffect(() => {
        http()
            .get(`/repositories/assignments/${id}`)
            .then((res) => setAssignment({...res.data, id}))
            .catch((err) => console.error(err));
        http()
            .get(`/assignments/${id}/grader`)
            .then((res) => setGrader(res.data))
            .catch((err) => console.error(err));
    }, [id]);

    const beforeUpload = (file) => {
        return window.confirm(`请确认上传的评分文件，上传后会覆盖现有文件！\n作业：${assignment.title}\n文件：${file.name}`);
    };

    const uploadGrader = ({file, onProgress, onSuccess, onError}) => {
        setDisabled(true);
        const formData = new FormData();
        formData.append("file", file);
        http()
            .post(`/assignments/${id}/grader`, formData, {
                headers: {"Content-Type": "multipart/form-data"},
                onUploadProgress: (e) => onProgress({percent: e.loaded * 100 / e.total})
            })
            .then((res) => {
                onSuccess(res.data);
                setGrader(res.data);
                message.success("上传评分文件成功！");
            })
            .catch((err) => {
                console.error(err);
                onError(err);
            })
            .finally(() => setDisabled(false));
    };

    const deleteGrader = () => {
        setDisabled(true);
        http()
            .delete(`/assignments/${id}/grader`)
            .then((res) => {
                setGrader({size: 0, modifiedAt: 0});
                message.success("删除评分文件成功！");
            })
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
    };

    return (
        <>
            <Typography.Title level={2}>
                <DashboardOutlined/> 评分管理
                <Button style={{float: "right"}} type="primary"
                        onClick={() => history.push(`/admin/assignments/${id}`)}>
                    <EditOutlined/> 编辑作业
                </Button>
            </Typography.Title>
            {!assignment
                ? <Skeleton/>
                : <>
                    <Descriptions>
                        <Descriptions.Item label="作业ID">{id}</Descriptions.Item>
                        <Descriptions.Item label="作业名称">{assignment.title}</Descriptions.Item>
                        <Descriptions.Item label="作业总分">
                            满分{assignment.totalScore}，总评占比{assignment.percentage}%
                        </Descriptions.Item>
                        {grader !== null && <>
                            <Descriptions.Item label="自动评分">
                                {grader.modifiedAt === 0
                                    ? <>未设置</>
                                    : <>
                                        已设置，文件大小 {grader.size} bytes，
                                        最后更新于 {moment(grader.modifiedAt).format("YYYY-MM-DD HH:mm:ss")}
                                        <Popconfirm title="确定要删除自动评分文件吗？" onConfirm={deleteGrader}>
                                            <Button type="link" size="small" danger>删除自动评分文件</Button>
                                        </Popconfirm>
                                    </>}
                            </Descriptions.Item>
                        </>}
                    </Descriptions>
                    {grader !== null && <>
                        <Upload.Dragger style={{maxHeight: "5em"}} name="file" accept=".zip" maxCount={1}
                                        beforeUpload={beforeUpload} customRequest={uploadGrader} disabled={disabled}>
                            <UploadOutlined/> 上传自动评分文件
                        </Upload.Dragger>
                    </>}
                    <Divider/>
                    <AdminSubmissionTable assignment={assignment}/>
                </>}
        </>
    );
};

export default AdminAssignmentGrader;
