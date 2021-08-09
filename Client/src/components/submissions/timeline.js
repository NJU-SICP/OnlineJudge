import React from "react";
import {Timeline} from "antd";
import moment from "moment";

const SubmissionTimeline = ({id, submission}) => (
    <Timeline pending={submission.result === null ? "等待系统处理……" : null}>
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
        {submission.result !== null && <>
            {submission.result.log &&
            <Timeline.Item>
                自动评分程序日志：<br/>
                <pre><code>{submission.result.log}</code></pre>
            </Timeline.Item>
            }
            {submission.result.error
                ? <>
                    <Timeline.Item color="red" className="ant-timeline-item-last">
                        自动评分遇到错误：{submission.result.error}<br/>
                        错误报告时间：{moment(submission.result.gradedAt).format(" YYYY-MM-DD HH:mm:ss ")}，请与管理员联系进行排查。
                    </Timeline.Item>
                </>
                : <>
                    {submission.result.score !== null
                        ? <>
                            <Timeline.Item color="green" className="ant-timeline-item-last">
                                {submission.result.gradedBy ?? "SICP Online Judge"} 在
                                {moment(submission.result.gradedAt).format(" YYYY-MM-DD HH:mm:ss ")}
                                {submission.result.gradedBy ? "对提交进行评分" : "完成自动评分"}，
                                总得分 {submission.result.score}。<br/>
                                {submission.result.message && <>评分信息：{submission.result.message}<br/></>}
                                {submission.result.details &&
                                <>
                                    评分详情：
                                    <ul>
                                        {submission.result.details && submission.result.details.map((detail, index) => (
                                            <li key={index}>
                                                {detail.title}：{detail.score}
                                                {!!detail.message && `（${detail.message}）`}
                                            </li>
                                        ))}
                                    </ul>
                                </>
                                }
                            </Timeline.Item>
                        </>
                        : <>
                            <Timeline.Item className="ant-timeline-item-last">
                                {submission.result.message}
                            </Timeline.Item>
                        </>}
                </>}
        </>}
    </Timeline>
);

export default SubmissionTimeline;
