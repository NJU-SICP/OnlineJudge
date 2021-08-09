import React, {useEffect, useState} from "react";
import {useHistory, useParams} from "react-router-dom";
import http from "../../../http";

import {Button, Card, Descriptions, Divider, message, Popconfirm, Skeleton, Typography, Upload} from "antd";
import {DashboardOutlined, EditOutlined, UploadOutlined} from "@ant-design/icons";
import AdminSubmissionTable from "../submissions/table";
import AdminAssignmentInfo from "./info";

const AdminAssignmentGrader = () => {
    const history = useHistory();
    const {id} = useParams();
    const [assignment, setAssignment] = useState(null);
    const [grader, setGrader] = useState(null);
    const [disabled, setDisabled] = useState(null);
    const [showBuildLog, setShowBuildLog] = useState(false);

    useEffect(() => {
        http()
            .get(`/assignments/${id}`)
            .then((res) => setAssignment({...res.data, id}))
            .catch((err) => console.error(err));
        http()
            .get(`/assignments/${id}/grader`)
            .then((res) => setGrader(res.data))
            .catch((err) => console.error(err));
    }, [id]);

    const beforeUpload = (file) => {
        return window.confirm(`请确认上传的评分文件，上传后会覆盖现有文件并重新编译容器！\n作业：${assignment.title}\n文件：${file.name}`);
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
                if (grader !== null && grader.imageId === null) {
                    setShowBuildLog(true);
                }
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
                setGrader(null);
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
                        <Descriptions.Item label="作业名称">
                            <code>{id}</code>
                            （<AdminAssignmentInfo assignmentId={id}/>）
                        </Descriptions.Item>
                        <Descriptions.Item label="作业总分" span={2}>
                            满分{assignment.totalScore}，总评占比{assignment.percentage}%
                        </Descriptions.Item>
                        <Descriptions.Item label="自动评分">
                            {!grader
                                ? <>未设置</>
                                : <>
                                    已设置，
                                    {!grader.imageId
                                        ? <>
                                            {grader.imageBuildError
                                                ? <>编译失败，请查看编译日志</>
                                                : <>正在编译Docker镜像 {grader.imageTags}</>}
                                        </>
                                        : <>Docker镜像ID：{grader.imageId} {grader.imageTags}</>}
                                    <Typography.Link style={{marginLeft: 20}}
                                                     onClick={() => setShowBuildLog(!showBuildLog)}>
                                        查看编译日志
                                    </Typography.Link>
                                    <Popconfirm title="确定要删除自动评分文件吗？" onConfirm={deleteGrader}>
                                        <Typography.Link type="danger" style={{marginLeft: 20}}>
                                            删除自动评分文件
                                        </Typography.Link>
                                    </Popconfirm>
                                </>}
                        </Descriptions.Item>
                    </Descriptions>
                    {grader !== null && showBuildLog &&
                    <Card style={{marginBottom: 10}}>
                        <pre><code>{grader.imageBuildLog}</code></pre>
                    </Card>
                    }
                    {grader !== null && <>
                        <Upload.Dragger style={{maxHeight: "5em"}} name="file" accept=".zip" maxCount={1}
                                        beforeUpload={beforeUpload} customRequest={uploadGrader} disabled={disabled}>
                            <UploadOutlined/> 上传自动评分文件（包含Dockerfile的zip压缩文件夹）
                        </Upload.Dragger>
                    </>}
                    <Divider/>
                    <AdminSubmissionTable assignment={assignment}/>
                </>}
        </>
    );
};

export default AdminAssignmentGrader;
