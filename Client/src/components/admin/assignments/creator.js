import React, {useState} from "react";
import {useHistory} from "react-router-dom";
import moment from "moment";
import http from "../../../http";

import {message, Typography} from "antd";
import AdminAssignmentForm from "./form";

const AdminAssignmentCreator = () => {
    const history = useHistory();
    const [disabled, setDisabled] = useState(false);

    const initialValues = {
        title: "",
        beginTime: null,
        endTime: null,
        submitFileType: null,
        submitFileSize: 10,
        submitCountLimit: null,
        totalScore: 0,
        percentage: 0
    };

    const createAssignment = (values) => {
        setDisabled(true);
        http()
            .post("/assignments", {
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
                message.success("创建作业成功！");
                history.push("/admin/assignments");
            })
            .catch((err) => {
                console.error(err);
                setDisabled(false);
            });
    };

    return (
        <>
            <Typography.Title level={2}>创建作业</Typography.Title>
            <AdminAssignmentForm initialValues={initialValues} onFinish={createAssignment} disabled={disabled}/>
        </>
    );
};

export default AdminAssignmentCreator;
