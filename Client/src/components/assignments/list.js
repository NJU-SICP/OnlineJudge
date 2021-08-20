import React, {useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import moment from "moment";
import "moment/locale/zh-cn";
import qs from "qs";
import http from "../../http";

import {Button, Pagination, Skeleton, Table, Typography} from "antd";
import {EditOutlined} from "@ant-design/icons";
import AssignmentScore from "./score";

const AssignmentList = () => {
    const history = useHistory();
    const location = useLocation();
    const [page, setPage] = useState(null);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/assignments?page=${page - 1}`)
            .then((res) => {
                const list = [];
                const now = moment();
                res.data.content.forEach((assignment) => {
                    const ddl = moment(assignment.endTime);
                    list.push({
                        ...assignment,
                        ended: now.isAfter(ddl)
                    });
                });
                setPage({...res.data, content: list});
            })
            .catch((err) => console.error(err));
    }, [location.search]);

    const columns = [
        {
            title: "作业标题",
            key: "title",
            dataIndex: "title",
            render: (title) => <Typography.Text strong>{title}</Typography.Text>
        },
        {
            title: "截止时间",
            key: "endTime",
            dataIndex: "endTime",
            render: (time, record) => {
                const ddl = moment(time).locale('zh_cn');
                if (record.ended) {
                    return <span><s>{ddl.format("YYYY-MM-DD HH:mm")}</s></span>;
                } else {
                    return <span>{ddl.format("YYYY-MM-DD HH:mm")}（{ddl.fromNow()}）</span>;
                }
            }
        },
        {
            title: "提交类型",
            key: "submitFileType",
            render: (text, record) => <span>{record.submitFileType} ({record.submitFileSize} MiB)</span>
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
            title: "总评占比",
            key: "percentage",
            dataIndex: "percentage",
            render: (value) => `${value}%`
        },
        {
            title: "最高得分",
            key: "score",
            render: (_, record) => <AssignmentScore assignmentId={record.id} totalScore={record.totalScore}/>
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => (
                <>
                    <Button type={record.ended ? "text" : "link"} size="small"
                            onClick={() => history.push(`/assignments/${record.id}`)}>
                        查看
                    </Button>
                </>
            )
        }

    ];

    return (
        <>
            <Typography.Title level={2}>
                <EditOutlined/> 作业列表
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
    )
};

export default AssignmentList;
