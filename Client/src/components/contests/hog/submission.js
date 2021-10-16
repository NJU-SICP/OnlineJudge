import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import moment from "moment";
import http from "../../../http";

import { Card, Descriptions, Empty, Skeleton, Typography } from "antd";
import { LoadingOutlined } from "@ant-design/icons";

const HogSubmission = () => {
    const [loaded, setLoaded] = useState(false);
    const [submission, setSubmission] = useState(null);

    useEffect(() => {
        http()
            .get(`/contests/hog/submission`)
            .then((res) => {
                setLoaded(true);
                setSubmission(res.data);
            })
            .catch((err) => {
                if (err.response.status === 404) {
                    setLoaded(true);
                } else {
                    console.error(err);
                }
            });
    }, []);

    return (<>
        <Typography.Title level={3}>
            我的提交
            {loaded && submission &&
                <>
                    <span style={{ marginLeft: "1rem" }}>
                        <code>{submission.submissionId.substr(-8)}</code>
                    </span>
                    {submission.valid && !submission.key &&
                        <span style={{ marginLeft: "1rem" }}>
                            <code><LoadingOutlined /> 处理中</code>
                        </span>
                    }
                </>
            }
        </Typography.Title>
        {!loaded
            ? <Skeleton />
            : <Card>
                {!submission
                    ? <Empty description={<>
                        你还没有参与Hog Contest，请提交代码到<Link to="/assignments/hogcon"><code>hogcon</code>作业</Link>以参与比赛。
                    </>} />
                    : <Descriptions>
                        <Descriptions.Item label="玩家名称">{submission.name}</Descriptions.Item>
                        <Descriptions.Item label="代码长度">{submission.size}</Descriptions.Item>
                        {submission.message &&
                            <Descriptions.Item label="错误信息">
                                <pre>{submission.message}</pre>
                            </Descriptions.Item>
                        }
                        <Descriptions.Item label="提交时间">
                            {moment(submission.date).format("YYYY-MM-DD HH:mm:ss")}
                        </Descriptions.Item>
                        {submission.valid && submission.wins &&
                            <Descriptions.Item label="胜负历史">
                                {!Object.keys(submission.wins).length
                                    ? <>暂无</>
                                    : <ul>
                                        {Object.keys(submission.wins).map(o => <>
                                            <li>
                                                {submission.wins[o] / 1e6} v.s. <code>{o.substr(-8)}</code>
                                                {" "}({submission.wins[o] <= 5e7 ? "lose" : "win"})
                                            </li>
                                        </>)}
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
