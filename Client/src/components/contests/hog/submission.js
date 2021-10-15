import { LoadingOutlined } from "@ant-design/icons";
import { Card, Empty, Skeleton } from "antd";
import React, { useEffect, useState } from "react";
import http from "../../../http";

const HogSubmission = () => {
    const [loaded, setLoaded] = useState(false);
    const [submission, setSubmission] = useState(null);

    useEffect(() => {
        http().get(`/contests/hog/submission`)
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
        {!loaded
            ? <Skeleton />
            : <Card>
                {!submission
                    ? <Empty description="你还没有参与Hog Contest，请提交代码到hogcon作业以参与比赛。" />
                    : <p>TODO</p>
                }
            </Card>
        }
    </>);
};

export default HogSubmission;
