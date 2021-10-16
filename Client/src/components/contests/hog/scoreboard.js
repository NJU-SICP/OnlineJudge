import React from "react";
import moment from "moment";

import { Alert, Button, Card, Empty, Skeleton, Table, Typography } from "antd";
import { RedoOutlined, LoadingOutlined } from "@ant-design/icons";

const HogScoreboard = ({ entries, reload, disabled }) => {
    const columns = [
        {
            title: "排名",
            key: "rank",
            dataIndex: "rank"
        },
        {
            title: "编号",
            key: "id",
            dataIndex: "submissionId",
            render: (id) => <Typography.Text copyable={{ text: id }}>
                <code>{id.substr(-8)}</code>
            </Typography.Text>
        },
        {
            title: "玩家名称",
            key: "name",
            dataIndex: "name"
        },
        {
            title: "代码长度",
            key: "size",
            dataIndex: "size"
        },
        {
            title: "获胜局数",
            key: "winCount",
            dataIndex: "winCount"
        },
        {
            title: "平均胜率",
            key: "winRate",
            dataIndex: "winRate",
            render: (rate) => `${rate.toFixed(4)}%`
        },
        {
            title: "提交时间",
            key: "date",
            dataIndex: "date",
            render: (d) => moment(d).format("YYYY-MM-DD HH:mm:ss")
        }
    ];

    return (<>
        <Typography.Title level={3}>
            比赛排行榜
            <div style={{ float: "right" }}>
                <Button type="primary" disabled={disabled} onClick={reload}>
                    <RedoOutlined />刷新
                </Button>
            </div>
        </Typography.Title>
        {!entries
            ? <Skeleton />
            : <>
                {entries.findIndex(e => Object.keys(e.wins).length < entries.length - 1) >= 0 &&
                    <Alert message={<>
                        <LoadingOutlined style={{ marginRight: "0.5rem" }} />
                        排行榜正在更新中，可能会需要数小时才能得到结果，可以去做一些有意义的事情。
                    </>} style={{ marginBottom: "1rem" }} />
                }
                {entries.length === 0
                    ? <Card>
                        <Empty description="现在还没有人参与Hog Contest，快成为第一个吧！" />
                    </Card>
                    : <Table columns={columns} dataSource={entries} rowKey="id" pagination={false} />
                }
            </>
        }
    </>);
};

export default HogScoreboard;
