import React, { useEffect, useState } from "react";
import http from "../../../http";

import { Divider, Typography } from "antd";
import { RobotOutlined } from "@ant-design/icons";
import HogSubmission from "./submission";
import HogScoreboard from "./scoreboard";

const HogContest = () => {
    const [entries, setEntries] = useState(null);
    const [disabled, setDisabled] = useState(false);

    const loadEntries = () => {
        setDisabled(true);
        http()
            .get(`/contests/hog/entries`)
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
                            if (e.wins[o] >= 5e5) {
                                ++winCount;
                            }
                            winRate += e.wins[o];
                        }
                    }
                    winRate /= 1e4 * playCount;
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
            .catch((err) => console.error(err))
            .finally(() => setDisabled(false));
    };

    useEffect(() => loadEntries(), []);

    return (<>
        <Typography.Title level={1} style={{ textAlign: "center" }}>
            <RobotOutlined /> Hog Contest 2021
        </Typography.Title>
        <Typography.Title level={4} style={{ textAlign: "center" }}>
            截止时间：2021-11-07 23:59
        </Typography.Title>
        <Divider />
        <HogSubmission entries={entries} reload={loadEntries} disabled={disabled}/>
        <Divider />
        <HogScoreboard entries={entries} reload={loadEntries} disabled={disabled}/>
    </>);
};

export default HogContest;