import React from "react";
import {Link} from "react-router-dom";

const AdminScoreSingle = ({userId, assignmentId, statistics}) => {
    return (<>
        {!statistics || statistics.count === 0
            ? "-"
            : <Link to={{
                pathname: "/admin/submissions",
                search: `?userId=${userId}&assignmentId=${assignmentId}`
            }}>{statistics.max}</Link>
        }
    </>);
};

export default AdminScoreSingle;
