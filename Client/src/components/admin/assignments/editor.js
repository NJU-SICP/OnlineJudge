import React, {useEffect, useState} from "react";

import {useHistory, useParams} from "react-router-dom";
import moment from "moment";
import http from "../../../http";

import {Typography, message, Popconfirm, Button, Skeleton} from "antd";
import {DeleteOutlined} from "@ant-design/icons";
import AdminAssignmentForm from "./form";
import {useSelector} from "react-redux";

const AdminAssignmentEditor = () => {
    const auth = useSelector((state) => state.auth.value);
    const {id} = useParams();
    const history = useHistory();
    const [user, setAssignment] = useState(null);
    const [disabled, setDisabled] = useState(false);

    useEffect(() => {
        http()
            .get(`/assignments/${id}`)
            .then((res) => setAssignment({
                ...res.data,
                id: id,
                rangeTime: [
                    moment(res.data.beginTime),
                    moment(res.data.endTime)
                ]
            }))
            .catch((err) => console.error(err));
    }, [id]);

    const updateAssignment = (values) => {
        setDisabled(true);
        http()
            .put(`/assignments/${id}`, {
                title: values.title,
                beginTime: moment(values.rangeTime[0]),
                endTime: moment(values.rangeTime[1]),
                submitFileType: values.submitFileType,
                submitFileSize: values.submitFileSize,
                submitCountLimit: values.submitCountLimit,
                totalScore: values.totalScore,
                percentage: values.percentage
            })
            .then(() => {
                message.success("修改作业成功！");
                history.push("/admin/assignments");
            })
            .catch((err) => console.error(err));
    };

    const deleteAssignment = () => {
        setDisabled(true);
        http()
            .delete(`/assignments/${id}`)
            .then(() => {
                message.success("删除作业成功！");
                history.push("/admin/assignments");
            })
            .catch((err) => {
                console.error(err);
                setDisabled(false);
            });
    };

    return (
        <>
            <Typography.Title level={2}>
                编辑作业
                {auth.authorities && auth.authorities.indexOf("OP_ASSIGNMENT_DELETE") >= 0 &&
                <Popconfirm title="确定要删除作业吗？" onConfirm={deleteAssignment}
                            okText="删除" okType="danger" cancelText="取消">
                    <Button style={{float: "right"}} type="danger" disabled={disabled}>
                        <DeleteOutlined/> 删除作业
                    </Button>
                </Popconfirm>
                }
            </Typography.Title>
            {!user
                ? <Skeleton/>
                : <AdminAssignmentForm initialValues={user} onFinish={updateAssignment} disabled={disabled}/>}
        </>
    );
};

export default AdminAssignmentEditor;
