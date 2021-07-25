import React, {useEffect, useState} from "react";
import {useParams} from "react-router-dom";
import http from "../../http";

import {Typography} from "antd";
import {BookOutlined} from "@ant-design/icons";

const AssignmentView = () => {
    const {id} = useParams();
    const [assignment, setAssignment] = useState(null);

    useEffect(() => {
        http()
            .get(`/assignments/${id}`)
            .then((res) => setAssignment(res.data))
            .catch((err) => console.error(err));
    }, [id]);

    return (
        <>
            {!assignment
                ? <p>加载数据中</p>
                : <>
                    <Typography.Title level={2}>
                        <BookOutlined/> 作业：{assignment.title}
                    </Typography.Title>
                </>
            }
        </>
    );
};

export default AssignmentView;
