import React, {useEffect, useState} from "react";
import http from "../../../http";

import {Skeleton, Table, Typography} from "antd";
import {TableOutlined} from "@ant-design/icons";
import AdminAssignmentInfo from "../assignments/info";
import AdminScoreSingle from "./single";
import {Link} from "react-router-dom";

const AdminScoreTable = () => {
    const [users, setUsers] = useState(null);
    const [columns, setColumns] = useState(null);

    useEffect(() => {
        http()
            .get(`/users/all`)
            .then((res) => setUsers(res.data.filter(u => u.roles.indexOf("ROLE_STUDENT") >= 0)))
            .catch((err) => console.error(err));
        http()
            .get(`/assignments/all`)
            .then((res) => {
                const cols = [
                    {
                        title: "学号",
                        dataIndex: "username",
                        key: "username",
                        render: (username, record) => <Link to={`/admin/users/${record.id}`}>{username}</Link>
                    },
                    {
                        title: "姓名",
                        dataIndex: "fullName",
                        key: "fullName"
                    }
                ];
                for (const assignment of res.data) {
                    cols.push({
                        title: <>
                            {assignment.slug
                                ? <Link to={`/admin/assignments/${assignment.id}`}>{assignment.slug}</Link>
                                : <AdminAssignmentInfo assignmentId={assignment.id}/>}
                        </>,
                        key: `assignment-${assignment.id}`,
                        render: (_, record) => <AdminScoreSingle userId={record.id} assignmentId={assignment.id}/>
                    });
                }
                setColumns(cols);
            })
            .catch((err) => console.error(err));
    }, []);

    return (<>
        <Typography.Title level={2}>
            <TableOutlined/> 成绩查询
        </Typography.Title>
        {!users || !columns
            ? <Skeleton/>
            : <>
                <Table columns={columns} dataSource={users} rowKey="id" pagination={false}/>
            </>
        }
    </>);
};

export default AdminScoreTable;
