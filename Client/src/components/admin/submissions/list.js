import React, {useEffect, useState} from "react";
import {Button, Pagination, Skeleton, Table, Typography} from "antd";
import {PaperClipOutlined} from "@ant-design/icons";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../../../http";
import moment from "moment";

const AdminSubmissionList = () => {
    const history = useHistory();
    const location = useLocation();
    const [submissions, setSubmissions] = useState(null);
    const [pagination, setPagination] = useState(null);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/repositories/submissions?sort=createdAt,desc&page=${page - 1}`)
            .then((res) => {
                setSubmissions(res.data._embedded.submissions);
                setPagination(res.data.page);
            })
            .catch((err) => console.error(err));
    }, [location.search]);

    const columns = [
        {
            title: "提交ID",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "用户ID",
            key: "userId",
            dataIndex: "userId",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "作业ID",
            key: "assignmentId",
            dataIndex: "assignmentId",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "提交时间",
            key: "createdAt",
            dataIndex: "createdAt",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "总得分",
            key: "score",
            dataIndex: "score"
        },
        {
            title: "评分人",
            key: "gradedBy",
            dataIndex: "gradedBy"
        },
        {
            title: "评分时间",
            key: "gradedAt",
            dataIndex: "gradedAt",
            render: (time) => !time ? "" : moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) =>
                <Button type="link" size="small"
                        onClick={() => history.push(`/admin/assignments/${record.assignmentId}/grader?selectedId=${record.id}`)}>
                    评分
                </Button>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <PaperClipOutlined/> 提交管理
            </Typography.Title>
            {!submissions
                ? <Skeleton/>
                : <>
                    <Table columns={columns} dataSource={submissions} rowId="id" pagination={false}/>
                    <div style={{float: "right", marginTop: "1em"}}>
                        {!!pagination &&
                        <Pagination current={pagination.number + 1} pageSize={pagination.size}
                                    total={pagination.totalElements}
                                    onChange={(p) => history.push({
                                        pathname: location.pathname,
                                        search: `?page=${p}`
                                    })}/>
                        }
                    </div>
                </>}
        </>
    )
};

export default AdminSubmissionList;
