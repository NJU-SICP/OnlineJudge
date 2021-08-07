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
                .get(`/repositories/users/${userId}`)
                .then((res) => {
                    setUser(res.data);
                    cached[userId] = user;
                })
                .catch((err) => console.error(err));
        }
    }, [userId]);

    return (<>
        <Typography.Link onClick={() => history.push(`/admin/users/${userId}`)}>
            {!user
                ? <><code>{userId.substr(-8)}</code><LoadingOutlined/></>
                : <>{user.username} {user.fullName}</>}
        </Typography.Link>
    </>);
};

export default AdminUserInfo;
