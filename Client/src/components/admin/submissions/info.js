import React, {useEffect, useState} from "react";
import http from "../../../http";
import {LoadingOutlined} from "@ant-design/icons";

const AdminSubmissionInfo = ({assignmentId, userId}) => {
    const [statistics, setStatistics] = useState(null);

    useEffect(() => {
        http()
            .get(`/submissions/scores/statistics`, {
                params: {
                    assignmentId: assignmentId,
                    userId: userId
                }
            })
            .then((res) => setStatistics(res.data))
            .catch((err) => console.error(err));
    }, [assignmentId, userId]);

    return (<>
        {!statistics
            ? <LoadingOutlined/>
            : <>
                提交次数：{statistics.count}<br/>
                平均得分：{Number(statistics.average).toFixed(2)}<br/>
                最高得分：{statistics.max}
            </>}
    </>)
};

export default AdminSubmissionInfo;
