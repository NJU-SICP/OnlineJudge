import React, {useEffect, useState} from "react";
import http from "../../../http";

import {Skeleton, Table, Typography} from "antd";
import {TableOutlined} from "@ant-design/icons";
import AdminAssignmentInfo from "../assignments/info";
import AdminScoreSingle from "./single";
import {Link} from "react-router-dom";

const AdminScoreTable = () => {
    const [error, setError] = useState(null);
    const [users, setUsers] = useState(null);
    const [columns, setColumns] = useState(null);

    useEffect(() => {
        Promise.all([http().get(`/scores/all`), http().get(`/users/all`), http().get(`/assignments/all`)])
            .then(([res1, res2, res3]) => {
                const scores = res1.data;
                setUsers(res2.data.filter(u => u.roles.indexOf("ROLE_STUDENT") >= 0));
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
                for (const assignment of res3.data.reverse()) {
                    const userStats = scores[assignment.id] ?? {};
                    cols.push({
                        title: <>
                            {assignment.slug
                                ? <Link to={`/admin/assignments/${assignment.id}`}>{assignment.slug}</Link>
                                : <AdminAssignmentInfo assignmentId={assignment.id} />}
                        </>,
                        key: `assignment-${assignment.id}`,
                        render: (_, record) =>
                            <AdminScoreSingle userId={record.id} assignmentId={assignment.id} statistics={userStats[record.id]} />
                    });
                }
                setColumns(cols);
            })
            .catch((err) => {
                console.error(err);
                setError(err);
            });
    }, []);

    return (<>
        <Typography.Title level={2}>
            <TableOutlined /> 成绩查询
        </Typography.Title>
        {!columns && !error
            ? <Skeleton />
            : <>
                {!error
                    ? <Table columns={columns} dataSource={users} rowKey="id" pagination={false} />
                    : <Typography.Text>加载失败：{error}</Typography.Text>
                }
            </>
        }
    </>);
};

export default AdminScoreTable;
