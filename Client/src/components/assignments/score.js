import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import http from "../../http";

import {LoadingOutlined} from "@ant-design/icons";

const AssignmentScore = ({assignmentId, totalScore}) => {
    const auth = useSelector((state) => state.auth.value);

    const [statistics, setStatistics] = useState(null);

    useEffect(() => {
        http()
            .get(`/submissions/scores/statistics`, {
                params: {
                    assignmentId: assignmentId,
                    userId: auth.userId
                }
            })
            .then((res) => setStatistics(res.data))
            .catch((err) => console.error(err));
    }, [auth, assignmentId]);

    return (<>
        {!statistics
            ? <LoadingOutlined/>
            : <>
                {statistics.count === 0
                    ? <span>尚未提交</span>
                    : <span>{statistics.max} / {totalScore}</span>}
            </>}
    </>);
};

export default AssignmentScore;
