import React, {useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import moment from "moment";
import "moment/locale/zh-cn";
import qs from "qs";
import http from "../../http";

import {Button, Table, Typography} from "antd";
import {EditOutlined} from "@ant-design/icons";

const AssignmentList = () => {
    const history = useHistory();
    const location = useLocation();
    const [assignments, setAssignments] = useState([]);
    const [pagination, setPagination] = useState(null);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/assignments?sort=endTime,desc&page=${page - 1}`)
            .then((res) => {
                const list = [];
                const now = moment();
                res.data._embedded.assignments.forEach((assignment) => {
                    const ddl = moment(assignment.endTime);
                    list.push({
                        ...assignment,
                        ended: now.isAfter(ddl)
                    });
                });
                setAssignments(list);
                setPagination(res.page);
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
            title: "总评占比",
            key: "percentage",
            dataIndex: "percentage",
            render: (value) => `${value}%`
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => (
                <>
                    <Button type={record.ended ? "" : "primary"} size="small"
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
            <Table columns={columns} dataSource={assignments} rowKey="id" pagination={false}/>
        </>
    )
};

export default AssignmentList;
