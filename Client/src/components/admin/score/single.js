import React, {useEffect, useState} from "react";
import {LoadingOutlined} from "@ant-design/icons";
import http from "../../../http";
import {Link} from "react-router-dom";

const AdminScoreSingle = ({userId, assignmentId}) => {
    const [statistics, setStatistics] = useState(null);

    useEffect(() => {
        http()
            .get(`/submissions/scores/statistics`, {
                params: {
                    userId: userId,
                    assignmentId: assignmentId
                }
            })
            .then((res) => setStatistics(res.data))
            .catch((err) => console.error(err));
    }, [userId, assignmentId]);

    return (<>
        {!statistics
            ? <LoadingOutlined/>
            : <>
                {statistics.count === 0
                    ? "-"
                    : <Link to={{
                        pathname: "/admin/submissions",
                        search: `?userId=${userId}&assignmentId=${assignmentId}`
                    }}>{statistics.max}</Link>
                }
            </>}
    </>);
};

export default AdminScoreSingle;
