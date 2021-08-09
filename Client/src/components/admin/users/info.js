import React, {useEffect, useState} from "react";
import {useHistory} from "react-router-dom";
import http from "../../../http";

import {Typography} from "antd";
import {LoadingOutlined} from "@ant-design/icons";

const cached = {};

const AdminUserInfo = ({userId}) => {
    const history = useHistory();
    const [user, setUser] = useState(null);

    useEffect(() => {
        if (cached[userId]) {
            setUser(cached[userId]);
        } else {
            http()
                .get(`/users/${userId}`)
                .then((res) => {
                    setUser(res.data);
                    cached[userId] = res.data;
                })
                .catch((err) => {
                    console.error(err);
                    if (err.response.status === 404) {
                        setUser({username: null});
                        cached[userId] = {username: null};
                    }
                });
        }
    }, [userId]);

    return (<>
        <Typography.Link type={user && user.username === null ? "danger" : "primary"}
                         onClick={() => history.push(`/admin/users/${userId}`)}>
            {!user
                ? <><code>{userId.substr(-8)}</code><LoadingOutlined/></>
                : <>{user.username ? `${user.username} ${user.fullName}` : "用户不存在"}</>}
        </Typography.Link>
    </>);
};

export default AdminUserInfo;
