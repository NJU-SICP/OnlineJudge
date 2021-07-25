import React, {useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../../../http";

import {Button, Pagination, Table, Typography} from "antd";
import {EditOutlined, PlusOutlined} from "@ant-design/icons";
import moment from "moment";

const AdminAssignmentList = () => {
    const history = useHistory();
    const location = useLocation();
    const [assignments, setAssignments] = useState([]);
    const [pagination, setPagination] = useState(null);
    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/assignments?sort=endTime,desc&page=${page - 1}`)
            .then((res) => {
                setAssignments(res.data._embedded.assignments);
                setPagination(res.data.page);
            })
            .catch((err) => console.error(err));
    }, [location.search]);

    const columns = [
        {
            title: "标题",
            dataIndex: "title",
            key: "title"
        },
        {
            title: "开始时间",
            dataIndex: "beginTime",
            key: "beginTime",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "结束时间",
            dataIndex: "endTime",
            key: "endTime",
            render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "提交类型",
            dataIndex: "submitFileType",
            key: "submitFileType"
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
            render: (text, record) => (
                <Button type="primary" size="small"
                        onClick={() => history.push(`/admin/assignments/${record.id}`)}>
                    查看
                </Button>
            )
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <EditOutlined/> 作业管理
                <Button style={{float: "right"}} onClick={() => history.push("/admin/assignments/create")}>
                    <PlusOutlined/> 添加作业
                </Button>
            </Typography.Title>
            <Table columns={columns} dataSource={assignments} rowKey="id" pagination={false}/>
            <div style={{float: "right", marginTop: "1em"}}>
                {!!pagination &&
                <Pagination current={pagination.number + 1} pageSize={pagination.size} total={pagination.totalElements}
                            onChange={(p) => history.push({pathname: location.pathname, search: `?page=${p}`})}/>
                }
            </div>
        </>
    );
};

export default AdminAssignmentList;
