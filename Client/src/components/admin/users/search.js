import React, {useState} from "react";
import {AutoComplete, Input} from "antd";
import http from "../../../http";

const AdminUserSearch = ({onSelect, ...props}) => {
    const [options, setOptions] = useState([]);

    const onSearch = (value) => {
        http()
            .get("/users/search", {
                params: {
                    prefix: value
                }
            })
            .then((res) => setOptions(res.data.map(user => {
                return {
                    label: <p>{user.username} {user.fullName}</p>,
                    value: `${user.username} ${user.fullName} [${user.id}]`,
                    user: user
                };
            })))
            .catch((err) => console.error(err));
    };

    return (
        <AutoComplete options={options} onSearch={onSearch} onSelect={onSelect}>
            <Input.Search placeholder="输入学号或姓名搜索用户" {...props} />
        </AutoComplete>
    );
};

export default AdminUserSearch;
