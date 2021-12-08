import {HighlightOutlined} from "@ant-design/icons";
import {Alert} from "antd";
import React, {useEffect, useState} from "react";
import http from "../../http";

const AssignmentPlagiarism = ({assignment, setPlagiarized}) => {
    const [plagiarism, setPlagiarism] = useState(null);

    useEffect(() => {
        if (assignment) {
            http()
                .get(`/plagiarisms/my`, {
                    params: {
                        assignmentId: assignment.id
                    }
                })
                .then((res) => {
                    setPlagiarism(res.data);
                    setPlagiarized(!!res.data);
                })
                .catch((err) => {
                    if (err.response.status !== 404) {
                        console.error(err);
                    }
                });
        }
    }, [assignment, setPlagiarized]);

    return (
        <>
            {plagiarism &&
                <Alert style={{marginBottom: "1em"}} showIcon icon={<HighlightOutlined/>} type="error"
                    message={`你的提交存在违反学术诚信的行为，本次作业的成绩按${plagiarism.score ?? "0"}分计。`}
                    description={`具体信息：${plagiarism.detail}`} />
            }
        </>
    );
};

export default AssignmentPlagiarism;
