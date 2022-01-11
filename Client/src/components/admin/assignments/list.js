import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {Link, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../../../http";

import {Button, Skeleton, Table, Typography} from "antd";
import {EditOutlined, PlusOutlined} from "@ant-design/icons";

const AdminAssignmentList = () => {
    const auth = useSelector((state) => state.auth.value);
    const location = useLocation();
    const [page, setPage] = useState(null);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/assignments?page=${page - 1}&size=99`)
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    }, [location.search]);

    const columns = [
        {
            title: "代号",
            key: "slug",
            dataIndex: "slug"
        },
        {
            title: "标题",
            key: "title",
            dataIndex: "title"
        },
        {
            title: "提交文件",
            key: "submitFile",
            render: (text, record) => <>
                <code>{record.submitFileName}{record.submitFileType}</code>{" "}
                ({record.submitFileSize} MiB)
            </>
        },
        {
            title: "提交次数",
            key: "submitCountLimit",
            dataIndex: "submitCountLimit",
            render: (limit) => {
                if (limit < 0) return "无次数限制";
                else if (limit === 0) return "不允许提交";
                else return `最多${limit}次`;
            }
        },
        {
            title: "总分占比",
            key: "score",
            render: (text, record) => (
                <span>{record.totalScore} ({record.percentage}%)</span>
            )
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => <>
                {auth.authorities && auth.authorities.indexOf("OP_ASSIGNMENT_UPDATE") > 0 &&
                    <Link to={`/admin/assignments/${record.id}`}>
                        <Button type="link" size="small">
                            编辑作业
                        </Button>
                    </Link>
                }
                {auth.authorities && auth.authorities.indexOf("OP_SUBMISSION_UPDATE") > 0 &&
                    <Link to={`/admin/assignments/${record.id}/grader`}>
                        <Button type="link" size="small">
                            评分管理
                        </Button>
                    </Link>
                }
                {auth.authorities && auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") > 0 &&
                    <Link to={{
                        pathname: `/admin/submissions`,
                        search: `?assignmentId=${record.id}`
                    }}>
                        <Button type="link" size="small">
                            提交查询
                        </Button>
                    </Link>
                }
            </>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <EditOutlined /> 作业管理
                {auth.authorities && auth.authorities.indexOf("OP_ASSIGNMENT_CREATE") >= 0 &&
                    <div style={{float: "right"}}>
                        <Link to="/admin/assignments/create">
                            <Button>
                                <PlusOutlined /> 添加作业
                            </Button>
                        </Link>
                    </div>
                }
            </Typography.Title>
            {!page
                ? <Skeleton />
                : <>
                    <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false} />
                </>}
        </>
    );
};

export default AdminAssignmentList;
