import React, {useState} from "react";
import {saveAs} from "file-saver"
import http from "../http";

import {Button, message} from "antd";
import {DownloadOutlined, LoadingOutlined} from "@ant-design/icons";

const Download = ({name, link, title, type, size}) => {
    const [disabled, setDisabled] = useState(false);

    const download = () => {
        setDisabled(true);
        http()
            .get(link, {
                responseType: "blob"
            })
            .then((res) => {
                saveAs(res.data, name);
            })
            .catch((err) => {
                console.error(err);
                if (err.response.status === 404) {
                    message.error("下载失败：文件不存在！");
                }
            })
            .finally(() => setDisabled(false));
    }

    return (
        <Button type={type ?? "link"} size={size ?? "small"} onClick={download} disabled={disabled}>
            {disabled
                ? <><LoadingOutlined/> 正在下载</>
                : <><DownloadOutlined/> {title ?? "下载文件"}</>
            }
        </Button>
    );
};

export default Download;
