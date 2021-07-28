import React from "react";
import {Timeline} from "antd";
import moment from "moment";

const SubmissionTimeline = ({id, submission}) => {
    return (
        <Timeline pending={submission.score === null ? "等待系统测试/人工评分……" : null}>
            {!!submission.createdBy &&
            <Timeline.Item>
                由 {submission.createdBy} 允许手动提交。
            </Timeline.Item>
            }
            <Timeline.Item>
                SICP Online Judge 在
                {moment(submission.createdAt).format(" YYYY-MM-DD HH:mm:ss ")}
                收到提交，编号为 <code>{id.substr(-8)}</code>。
            </Timeline.Item>
            {!!submission.results &&
            <Timeline.Item color="green">
                {submission.gradedBy} 在
                {moment(submission.gradedAt).format(" YYYY-MM-DD HH:mm:ss ")}
                对提交进行评分：
                <ul>
                    {submission.results.map((result, index) => (
                       <li key={index}>
                           {result.title}：{result.score}
                           {!!result.message && `（${result.message}）`}；
                       </li>
                    ))}
                    <li>提交总得分：{submission.score}{!!submission.message && `（${submission.message}）`}。</li>
                </ul>
            </Timeline.Item>
            }
        </Timeline>
    );
};

export default SubmissionTimeline;
