import React, {useEffect, useState} from "react";
import moment from "moment";
import http from "../../http";

import {Card, Col, Row, Skeleton, Table, Timeline, Typography} from "antd";
import {ArrowRightOutlined} from "@ant-design/icons";

const SubmissionTable = ({submissions}) => {
    const [selected, setSelected] = useState(null);
    const [submission, setSubmission] = useState(null);

    useEffect(() => {
        if (selected === null) {
            setSubmission(null);
        } else {
            http()
                .get(`/repositories/submissions/${selected.id}`)
                .then((res) => setSubmission(res.data))
                .catch((err) => console.error(err));
        }
    }, [selected]);

    const columns = [
        {
            title: "次序",
            key: "index",
            dataIndex: "index",
            render: (index) => (
                <Typography.Text strong>
                    #{index}
                </Typography.Text>
            )
        },
        {
            title: "编号",
            key: "id",
            dataIndex: "id",
            render: (id) => <code>{id.substr(-8)}</code>
        },
        {
            title: "时间",
            key: "createdAt",
            dataIndex: "createdAt",
            render: (time) => moment(time).format("MM-DD HH:mm")
        },
        {
            title: "查看",
            key: "selected",
            render: (record) => <>{selected && record.index === selected.index && <ArrowRightOutlined/>}</>
        }
    ];

    return (
        <>
            {submissions === null
                ? <Skeleton/>
                : <>
                    <Row gutter={10}>
                        <Col span={8}>
                            <Table columns={columns} dataSource={submissions} pagination={false}
                                   onRow={(record) => {
                                       return {onClick: () => setSelected(record)};
                                   }}/>
                        </Col>
                        <Col span={16}>
                            {!selected
                                ? <p style={{margin: "1em"}}>在左侧列表中点击某次提交来查看详情。</p>
                                : <>
                                    <Card title={`提交 #${selected.index}`} style={{width: "100%", height: "100%"}}>
                                        {!submission
                                            ? <Skeleton/>
                                            : <>
                                                <Timeline pending={!submission.results ? "等待系统测试/人工评分……" : null}>
                                                    {!!submission.createdBy &&
                                                    <Timeline.Item>
                                                        由 {submission.createdBy} 允许手动提交。
                                                    </Timeline.Item>
                                                    }
                                                    <Timeline.Item>
                                                        SICP Online Judge 在
                                                        {moment(submission.createdAt).format(" YYYY-MM-DD HH:mm:ss ")}
                                                        收到提交，编号为 <code>{selected.id.substr(-8)}</code>。
                                                    </Timeline.Item>
                                                </Timeline>
                                            </>}
                                    </Card>
                                </>}
                        </Col>
                    </Row>
                </>}
        </>
    )
};

export default SubmissionTable;
