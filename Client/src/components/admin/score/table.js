import React, {useEffect, useState} from "react";
import http from "../../../http";

import {Button, Skeleton, Table, Typography} from "antd";
import {TableOutlined} from "@ant-design/icons";
import AdminAssignmentInfo from "../assignments/info";
import AdminScoreSingle from "./single";
import {Link} from "react-router-dom";

const AdminScoreTable = () => {
    const [show, setShow] = useState(false);
    const [users, setUsers] = useState(null);
    const [statistics, setStatistics] = useState(null);
    const [columns, setColumns] = useState(null);

    useEffect(() => {
        if (!show) return;
        http()
            .get(`/scores/all`)
            .then((res) => setStatistics(res.data))
            .catch((err) => console.error(err));
    }, [show]);

    useEffect(() => {
        if (!show) return;
        if (!statistics) return;
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
                for (const assignment of res.data.reverse()) {
                    const userStats = statistics[assignment.id] ?? {};
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
            .catch((err) => console.error(err));
    }, [show, statistics]);

    return (<>
        <Typography.Title level={2}>
            <TableOutlined /> 成绩查询
        </Typography.Title>
        {!show
            ? <div>
                <p>本页面用于展示所有学生的作业和成绩，由于技术过烂，服务器计算所有数据需要大约1分钟时间，请务必耐心等待。如需查看，请点击下方按钮。</p>
                <p><Button onClick={() => setShow(true)}>查看所有成绩</Button></p>
            </div>
            : <>
                {!statistics || !users || !columns
                    ? <Skeleton />
                    : <Table columns={columns} dataSource={users} rowKey="id" pagination={false} />
                }
            </>
        }
    </>);
};

export default AdminScoreTable;
