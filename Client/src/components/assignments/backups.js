import React, {useEffect, useState} from "react";
import {useSelector} from "react-redux";
import {useHistory, useLocation, useParams} from "react-router-dom";
import moment from "moment";
import qs from "qs";
import http from "../../http";

import {Pagination, Skeleton, Table, Typography} from "antd";
import {CloudServerOutlined, LoadingOutlined} from "@ant-design/icons";
import Download from "../download";

const BackupList = () => {
    const auth = useSelector((state) => state.auth.value);
    const {id} = useParams();
    const history = useHistory();
    const location = useLocation();

    const [assignment, setAssignment] = useState(null);
    const [page, setPage] = useState(null);

    useEffect(() => {
        http()
            .get(`/assignments/${id}`)
            .then((res) => setAssignment(res.data))
            .catch((err) => console.error(err));
    }, [id]);

    useEffect(() => {
        const page = qs.parse(location.search, {ignoreQueryPrefix: true}).page ?? 1;
        http()
            .get(`/backups`, {
                params: {
                    userId: auth.userId,
                    assignmentId: id,
                    page: page - 1
                }
            })
            .then((res) => setPage(res.data))
            .catch((err) => console.error(err));
    }, [location.search, auth.userId, id]);

    const columns = [
        {
            title: "备份ID",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "文件",
            key: "key",
            dataIndex: "key",
            render: (key) => key.replace(/backups\//, "")
        },
        {
            title: "时间",
            key: "time",
            dataIndex: "analytics",
            render: (analytics, record) => moment(analytics?.time ?? record.createdAt).format("YYYY-MM-DD HH:mm:ss")
        },
        {
            title: "操作",
            key: "actions",
            render: (text, record) => <>
                {!assignment
                    ? <LoadingOutlined/>
                    : <Download link={`/backups/${record.id}/download`} name={record.key}/>}
            </>
        }
    ];

    return (
        <>
            <Typography.Title level={2}>
                <CloudServerOutlined/> 备份列表
                {assignment && <>（作业：{assignment.slug} {assignment.title}）</>}
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

export default BackupList;
