import React, {useEffect, useState} from "react";
import {useHistory} from "react-router-dom";
import http from "../../../http";

import {Typography} from "antd";
import {LoadingOutlined} from "@ant-design/icons";

const cached = {};

const AdminAssignmentInfo = ({assignmentId}) => {
    const history = useHistory();
    const [assignment, setAssignment] = useState(null);

    useEffect(() => {
        if (cached[assignmentId]) {
            setAssignment(cached[assignmentId]);
        } else {
            http()
                .get(`/assignments/${assignmentId}`)
                .then((res) => {
                    setAssignment(res.data);
                    cached[assignmentId] = res.data;
                })
                .catch((err) => {
                    console.error(err);
                    if (err.response.status === 404) {
                        setAssignment({title: null});
                        cached[assignmentId] = {title: null};
                    }
                });
        }
    }, [assignmentId]);

    return (<>
        <Typography.Link type={assignment && assignment.title === null ? "danger" : "primary"}
                         onClick={() => history.push(`/admin/assignments/${assignmentId}`)}>
            {!assignment
                ? <><code>{assignmentId.substr(-8)}</code><LoadingOutlined/></>
                : <>{assignment.title ? `[${assignment.slug}] ${assignment.title}` : "作业不存在"}</>}
        </Typography.Link>
    </>);
};

export default AdminAssignmentInfo;

