import React, {useEffect, useState} from "react";
import http from "../../../http";

import {Pagination, Table} from "antd";
import {Skeleton} from "antd/es";
import AdminUserInfo from "../users/info";
import AdminSubmissionInfo from "./info";

const AdminSubmissionUserList = ({assignment, submitted}) => {
    const [_page, _setPage] = useState(0);
    const [_size, _setSize] = useState(20);
    const [page, setPage] = useState(null);

    useEffect(() => {
        http()
            .get(`/submissions/users`, {
                params: {
                    assignmentId: assignment.id,
                    submitted: submitted,
                    page: _page,
                    size: _size
                }
            })
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    }, [assignment, submitted, _page, _size]);

    const columns = [
        {
            title: "用户",
            key: "id",
            dataIndex: "id",
            render: (id) => <AdminUserInfo userId={id}/>
        },
        {
            title: "提交情况",
            key: "statistics",
            render: (_, record) => submitted
                ? <AdminSubmissionInfo assignmentId={assignment.id} userId={record.id}/>
                : "未提交"
        }
    ];

    return (<>
        {!page
            ? <Skeleton/>
            : <>
                <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false}/>
                <div style={{float: "right", marginTop: "1em"}}>
                    <Pagination current={page.number + 1} pageSize={page.size}
                                total={page.totalElements}
                                onChange={(p, s) => {
                                    _setPage(p - 1);
                                    _setSize(s);
                                }}/>
                </div>
            </>}
    </>);
};

export default AdminSubmissionUserList;
