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
                .get(`/repositories/assignments/${assignmentId}`)
                .then((res) => {
                    setAssignment(res.data);
                    cached[assignmentId] = res.data;
                })
                .catch((err) => console.error(err));
        }
    }, [assignmentId]);

    return (<>
        <Typography.Link onClick={() => history.push(`/admin/assignments/${assignmentId}`)}>
            {!assignment
                ? <><code>{assignmentId.substr(-8)}</code><LoadingOutlined/></>
                : <>{assignment.title}</>}
        </Typography.Link>
    </>);
};

export default AdminAssignmentInfo;

