import React, {useEffect, useState} from "react";
import {Link, useHistory, useLocation} from "react-router-dom";
import moment from "moment";
import "moment/locale/zh-cn";
import qs from "qs";
import http from "../../http";

import {Button, Pagination, Skeleton, Table, Typography} from "antd";
import {EditOutlined} from "@ant-design/icons";
import AssignmentScore from "./score";

const AssignmentList = () => {
    const history = useHistory;
    const location = useLocation();
    const [page, setPage] = useState(null);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/assignments/begun?page=${page - 1}`)
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
            title: "作业代号",
            key: "slug",
            dataIndex: "slug",
            render: (slug) => <Typography.Text strong>{slug}</Typography.Text>
        },
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
                return <Typography.Text delete={record.ended}>
                    {ddl.format("YYYY-MM-DD HH:mm")}
                    {!record.ended && <>（{ddl.fromNow()}）</>}
                </Typography.Text>;
            }
        },
        {
            title: "最高得分",
            key: "score",
            render: (_, record) => <AssignmentScore assignmentId={record.id} totalScore={record.totalScore} />
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) =>
                <Link to={`/assignments/${record.slug ?? record.id}`}>
                    <Button type={record.ended ? "text" : "link"} size="small">
                        查看
                    </Button>
                </Link>
        }

    ];

    return (
        <>
            <Typography.Title level={2}>
                <EditOutlined /> 作业列表
            </Typography.Title>
            {!page
                ? <Skeleton />
                : <>
                    <Table columns={columns} dataSource={page.content} rowKey="id" pagination={false} />
                    <div style={{float: "right", marginTop: "1em"}}>
                        <Pagination current={page.number + 1} pageSize={page.size} total={page.totalElements}
                            onChange={(p) => history.push({
                                pathname: location.pathname,
                                search: `?page=${p}`
                            })} />
                    </div>
                </>}
        </>
    )
};

export default AssignmentList;
