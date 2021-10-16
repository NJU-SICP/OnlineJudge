import React, { useEffect, useState } from "react";
import moment from "moment";
import http from "../../../http";

import { Card, Empty, Skeleton, Table, Typography } from "antd";
import { LoadingOutlined } from "@ant-design/icons";

const HogScoreboard = () => {
    const [entries, setEntries] = useState(null);

    useEffect(() => {
        http()
            .get(`/contests/hog/scoreboard`)
            .then((res) => {
                const list = [];
                const valids = res.data.map(e => e.id);
                res.data.forEach(e => {
                    if (!e.key) return;
                    let playCount = 0;
                    let winCount = 0;
                    let winRate = 0;
                    for (let o in e.wins) {
                        if (valids.indexOf(o) >= 0) {
                            ++playCount;
                            if (e.wins[o] >= 5e7) {
                                ++winCount;
                            }
                            winRate += e.wins[o];
                        }
                    }
                    winRate /= 1e6 * playCount;
                    list.push({
                        ...e,
                        playCount,
                        winCount,
                        winRate,
                    });
                });
                list.sort((e1, e2) => {
                    if (e1.winCount !== e2.winCount) {
                        return e2.winCount - e1.winCount;
                    } else {
                        return e1.size - e2.size;
                    }
                });
                for (let i = 0; i < list.length; ++i) {
                    list[i].rank = i + 1;
                }
                setEntries(list);
            })
            .catch((err) => console.error(err));
    }, []);

    const columns = [
        {
            title: "排名",
            key: "rank",
            dataIndex: "rank",
            render: (rank, record) => <>
                {rank}
                <span style={{ marginLeft: "1rem" }}>
                    {record.playCount !== entries.length - 1 && <LoadingOutlined />}
                </span>
            </>
        },
        {
            title: "编号",
            key: "id",
            dataIndex: "submissionId",
            render: (id) => <code>{id.substr(-8)}</code>
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
            排行榜（排行榜更新可能会需要数小时，可以去做一些有意义的事情）
        </Typography.Title>
        {!entries
            ? <Skeleton />
            : <>
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
