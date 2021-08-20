import React, {useCallback, useEffect, useState} from "react";
import {useHistory, useLocation} from "react-router-dom";
import moment from "moment";
import http from "../../http";

import {Affix, Card, Col, Pagination, Row, Skeleton, Table, Typography} from "antd";
import {ArrowRightOutlined, LoadingOutlined} from "@ant-design/icons";
import SubmissionTimeline from "./timeline";
import Download from "../download";

const SubmissionTable = ({assignment, page}) => {
    const history = useHistory();
    const location = useLocation();
    const [selected, setSelected] = useState(null);
    const [submission, setSubmission] = useState(null);

    const fetchSubmission = useCallback(() => {
        if (selected) {
            http()
                .get(`/submissions/${selected.id}`)
                .then((res) => setSubmission(res.data))
                .catch((err) => console.error(err));
        }
    }, [selected]);

    useEffect(() => {
        if (selected === null) {
            setSubmission(null);
        } else {
            fetchSubmission();
        }
    }, [selected, fetchSubmission]);

    useEffect(() => {
        if (selected && submission && submission.result === null) {
            const selectedId = selected.id;
            setTimeout(() => {
                if (selected.id === selectedId) {
                    fetchSubmission();
                }
            }, 3000);
        }
    }, [selected, submission, fetchSubmission]);

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
            render: (result) => {
                if (result === null) {
                    return <LoadingOutlined/>;
                } else if (result.error === null) {
                    return result?.score;
                } else {
                    return <Typography.Text type="danger">评分失败</Typography.Text>;
                }
            }
        },
        {
            title: "查看",
            key: "selected",
            render: (record) => <>{selected && record.index === selected.index && <ArrowRightOutlined/>}</>
        }
    ];

    return (
        <>
            {!page
                ? <Skeleton/>
                : <>
                    <Row gutter={10}>
                        <Col span={9}>
                            <Table columns={columns} dataSource={page.content} pagination={false} rowKey="id"
                                   onRow={(record) => {
                                       return {onClick: () => setSelected(record)};
                                   }}/>
                            <div style={{float: "right", marginTop: "1em"}}>
                                <Pagination current={page.number + 1} pageSize={page.size} total={page.totalElements}
                                            onChange={(p) => history.push({
                                                pathname: location.pathname,
                                                search: `?page=${p}`
                                            })}/>
                            </div>
                        </Col>
                        <Col span={15}>
                            {!selected
                                ? <p style={{margin: "1em"}}>在左侧列表中点击某次提交来查看详情。</p>
                                : <Affix offsetTop={10}>
                                    <Card title={`提交 #${selected.index}`}
                                          extra={
                                              <Download link={`/submissions/${selected.id}/download`}
                                                        name={`${selected.id}${assignment.submitFileType}`}/>
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
