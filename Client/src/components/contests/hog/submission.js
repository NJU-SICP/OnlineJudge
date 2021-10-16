import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";
import moment from "moment";

import { Button, Card, Descriptions, Empty, Skeleton, Typography } from "antd";
import { LoadingOutlined, RedoOutlined } from "@ant-design/icons";

const HogSubmission = ({ entries, reload, disabled }) => {
    const auth = useSelector((state) => state.auth.value);
    const [entry, setEntry] = useState(null);

    useEffect(() => {
        setEntry(entries ? entries.find(e => e.userId === auth.userId) : null);
    }, [auth, entries]);

    return (<>
        <Typography.Title level={3}>
            我的提交
            {entries && entry &&
                <>
                    <span style={{ marginLeft: "1rem" }}>
                        <code>{entry.submissionId.substr(-8)}</code>
                    </span>
                    {entry.valid && !entry.key &&
                        <span style={{ marginLeft: "1rem" }}>
                            <LoadingOutlined /> 处理中
                        </span>
                    }
                </>
            }
            <div style={{ float: "right" }}>
                <Button type="primary" disabled={disabled} onClick={reload}>
                    <RedoOutlined />刷新
                </Button>
            </div>
        </Typography.Title>
        {!entries
            ? <Skeleton />
            : <Card>
                {!entries || !entry
                    ? <Empty description={<>
                        你还没有参与Hog Contest，请提交代码到<Link to="/assignments/hogcon"><code>hogcon</code>作业</Link>以参与比赛。
                    </>} />
                    : <Descriptions>
                        <Descriptions.Item label="玩家名称">{entry.name}</Descriptions.Item>
                        <Descriptions.Item label="代码长度">{entry.size}</Descriptions.Item>
                        {entry.message &&
                            <Descriptions.Item label="错误信息">
                                <pre>{entry.message}</pre>
                            </Descriptions.Item>
                        }
                        <Descriptions.Item label="提交时间">
                            {moment(entry.date).format("YYYY-MM-DD HH:mm:ss")}
                        </Descriptions.Item>
                        {entry.valid && entry.wins &&
                            <Descriptions.Item label="胜负数据">
                                {!Object.keys(entry.wins).length
                                    ? <>暂无</>
                                    : <ul>
                                        {Object.keys(entry.wins)
                                            .sort((o1, o2) => entry.wins[o2] - entry.wins[o1])
                                            .filter(o => entries.findIndex(e => e.id === o) >= 0)
                                            .map(o => <li key={o}>
                                                {(entry.wins[o] / 1e4).toFixed(4)}%{" "}
                                                {entry.wins[o] <= 5e5 ? "负" : "胜"}{" "}
                                                {entries.find(e => e.id === o)?.name}
                                            </li>)}
                                        {Object.keys(entry.wins)
                                            .filter(o => entries.findIndex(e => e.id === o) >= 0)
                                            .length < entries.length - 1 &&
                                            <li>
                                                <LoadingOutlined style={{ marginRight: "0.5rem" }} />
                                                其他对战计算中……
                                            </li>
                                        }
                                    </ul>
                                }
                            </Descriptions.Item>
                        }
                    </Descriptions>
                }
            </Card>
        }
    </>);
};

export default HogSubmission;
