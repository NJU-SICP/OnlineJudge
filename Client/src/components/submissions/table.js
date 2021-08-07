import React, {useEffect, useState} from "react";
import moment from "moment";
import {saveAs} from "file-saver";
import http from "../../http";

import {Affix, Button, Card, Col, Row, Skeleton, Table, Typography} from "antd";
import {ArrowRightOutlined, DownloadOutlined} from "@ant-design/icons";
import SubmissionTimeline from "./timeline";

const SubmissionTable = ({assignment, submissions}) => {
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

    const downloadSubmission = () => {
        http()
            .get(`/submissions/${selected.id}/download`, {
                responseType: "blob"
            })
            .then((res) => {
                saveAs(res.data, `${selected.id}${assignment.submitFileType}`);
            })
            .catch((err) => console.error(err));
    };

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
            title: "得分",
            key: "score",
            dataIndex: "result",
            render: (result) => (result && result.error) ? <Typography.Text type="danger">评分失败</Typography.Text> : result?.score
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
                        <Col span={9}>
                            <Table columns={columns} dataSource={submissions} pagination={false} rowKey="id"
                                   onRow={(record) => {
                                       return {onClick: () => setSelected(record)};
                                   }}/>
                        </Col>
                        <Col span={15}>
                            {!selected
                                ? <p style={{margin: "1em"}}>在左侧列表中点击某次提交来查看详情。</p>
                                : <Affix offsetTop={10}>
                                    <Card title={`提交 #${selected.index}`}
                                          extra={
                                              <Button type="link" size="small" onClick={downloadSubmission}>
                                                  <DownloadOutlined/> 下载文件
                                              </Button>
                                          }>
                                        {!submission
                                            ? <Skeleton/>
                                            : <SubmissionTimeline id={selected.id} submission={submission}/>}
                                    </Card>
                                </Affix>}
                        </Col>
                    </Row>
                </>}
        </>
    )
};

export default SubmissionTable;
