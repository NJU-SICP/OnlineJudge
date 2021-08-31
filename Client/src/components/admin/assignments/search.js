import React, {useState} from "react";
import {AutoComplete, Input} from "antd";
import http from "../../../http";

const AdminAssignmentSearch = ({disabled, onSelect, ...props}) => {
    const [options, setOptions] = useState([]);

    const onSearch = (value) => {
        http()
            .get("/assignments/search", {
                params: {
                    prefix: value
                }
            })
            .then((res) => setOptions(res.data.map(assignment => {
                return {
                    label: <p>{assignment.slug} {assignment.title}</p>,
                    value: `${assignment.slug} ${assignment.title} [${assignment.id}]`,
                    assignment: assignment
                };
            })))
            .catch((err) => console.error(err));
    };

    return (
        <AutoComplete options={options} onSearch={onSearch} onSelect={onSelect} disabled={disabled}>
            <Input.Search placeholder="输入作业代号或标题搜索作业" {...props} />
        </AutoComplete>
    );
};

export default AdminAssignmentSearch;
