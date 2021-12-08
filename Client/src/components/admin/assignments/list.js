import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import moment from "moment";
import http from "../../../http";

import {Button, Pagination, Skeleton, Table, Typography} from "antd";
import {EditOutlined, PlusOutlined} from "@ant-design/icons";

const AdminAssignmentList = () => {
    const auth = useSelector((state) => state.auth.value);
    const history = useHistory();
    const location = useLocation();
    const [page, setPage] = useState(null);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/assignments?page=${page - 1}`)
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
            title: "开始时间",
            key: "beginTime",
            dataIndex: "beginTime",
            render: (time) => {
                return <Typography.Text delete={moment().isAfter(time)}>
                    {moment(time).format("YYYY-MM-DD HH:mm:ss")}
                </Typography.Text>;
            }
        },
        {
            title: "结束时间",
            key: "endTime",
            dataIndex: "endTime",
            render: (time) => {
                return <Typography.Text delete={moment().isAfter(time)}>
                    {moment(time).format("YYYY-MM-DD HH:mm:ss")}
                </Typography.Text>;
            }
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
                <Button type="link" size="small"
                        onClick={() => history.push(`/admin/assignments/${record.id}`)}>
                    编辑作业
                </Button>
                }
                {auth.authorities && auth.authorities.indexOf("OP_SUBMISSION_UPDATE") > 0 &&
                <Button type="link" size="small"
                        onClick={() => history.push(`/admin/assignments/${record.id}/grader`)}>
                    评分管理
                </Button>
                }
                {auth.authorities && auth.authorities.indexOf("OP_SUBMISSION_READ_ALL") > 0 &&
                <Button type="link" size="small"
                        onClick={() => history.push({
                            pathname: `/admin/submissions`,
                            search: `?assignmentId=${record.id}`
                        })}>
                    提交查询
                </Button>
                }
            </>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <EditOutlined/> 作业管理
                {auth.authorities && auth.authorities.indexOf("OP_ASSIGNMENT_CREATE") >= 0 &&
                <div style={{float: "right"}}>
                    <Button onClick={() => history.push("/admin/assignments/create")}>
                        <PlusOutlined/> 添加作业
                    </Button>
                </div>
                }
            </Typography.Title>
            {!page
                ? <Skeleton/>
                : <>
                    <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false}/>
                    <div style={{float: "right", marginTop: "1em"}}>
                        <Pagination current={page.number + 1} pageSize={page.size} total={page.totalElements}
                                    onChange={(p) => history.push({
                                        pathname: location.pathname,
                                        search: `?page=${p}`
                                    })}/>
                    </div>
                </>}
        </>
    );
};

export default AdminAssignmentList;
