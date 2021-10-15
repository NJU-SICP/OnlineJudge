import React from "react";

import { Divider, Typography } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import HogSubmission from "./submission";
import HogScoreboard from "./scoreboard";

const HogContest = () => {
    return (<>
        <Typography.Title level={1} style={{ textAlign: "center" }}>
            <RobotOutlined /> Hog Contest' 2021
        </Typography.Title>
        <Typography.Title level={4} style={{ textAlign: "center" }}>
            截止时间：2021-11-14 23:59
        </Typography.Title>
        <Divider />
        <Typography.Title level={3}> 我的提交 </Typography.Title>
        <HogSubmission />
        <Divider />
        <Typography.Title level={3}> 排行榜 </Typography.Title>
        <HogScoreboard />
    </>);
};

export default HogContest;