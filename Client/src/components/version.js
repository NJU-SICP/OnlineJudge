import React, {useEffect, useState} from "react";
import config from "../config";
import http from "../http";
import Download from "./download";
import {LoadingOutlined} from "@ant-design/icons";

const Version = () => {
    const [serverVersion, setServerVersion] = useState(null);
    const [clientVersion, setClientVersion] = useState(null);

    useEffect(() => {
        http()
            .get(`/misc/version`)
            .then((res) => setServerVersion(res.data))
            .catch(() => setServerVersion("unknown"));
        http()
            .get(`/misc/ok-client/version`)
            .then((res) => setClientVersion(res.data))
            .catch(() => setClientVersion("unknown"));
    }, []);

    return (<>
        Version:
        web {config.version},&nbsp;
        server {!serverVersion ? <LoadingOutlined /> : serverVersion},&nbsp;
        client {!clientVersion
            ? <LoadingOutlined />
            : <>
                {clientVersion === "unknown"
                    ? "unknown"
                    : <Download name="ok" title={clientVersion} link={`/misc/ok-client/${clientVersion}`} />}
            </>}
    </>);
};

export default Version;
