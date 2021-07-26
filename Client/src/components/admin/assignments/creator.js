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
        title: null,
        beginTime: null,
        endTime: null,
        submitFileType: null,
        submitCountLimit: null,
        totalScore: null,
        percentage: null
    };

    const createAssignment = (values) => {
        console.log(values);
        setDisabled(true);
        http()
            .post("/assignments", {
                title: values.title,
                beginTime: moment(values.rangeTime[0]),
                endTime: moment(values.rangeTime[1]),
                submitFileType: values.submitFileType,
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
