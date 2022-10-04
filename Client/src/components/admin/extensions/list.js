import React, { useCallback, useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import moment from "moment";
import qs from "qs";
import http from "../../../http";

import {
  Button,
  DatePicker,
  Divider,
  Form,
  message,
  Pagination,
  Popconfirm,
  Skeleton,
  Table,
  Typography,
} from "antd";
import { ClockCircleOutlined } from "@ant-design/icons";
import AdminUserInfo from "../users/info";
import AdminAssignmentInfo from "../assignments/info";
import AdminUserSearch from "../users/search";
import AdminAssignmentSearch from "../assignments/search";
import TextArea from "antd/lib/input/TextArea";

const AdminExtensionList = () => {
  const location = useLocation();
  const history = useHistory();

  const [form] = Form.useForm();
  const [page, setPage] = useState(null);
  const [disabled, setDisabled] = useState(false);

  const fetchExtensions = useCallback(() => {
    const page =
      qs.parse(location.search, { ignoreQueryPrefix: true }).page ?? 1;
    http()
      .get(`/extensions`, {
        params: {
          page: page - 1,
        },
      })
      .then((res) => setPage(res.data))
      .catch((err) => console.error(err));
  }, [location.search]);
  useEffect(fetchExtensions, [location.search, fetchExtensions]);

  const createExtension = (values) => {
    setDisabled(true);
    http()
      .post(`/extensions`, {
        userId: values.userId,
        assignmentId: values.assignmentId,
        endTime: values.endTime,
        message: values.message,
      })
      .then((res) => {
        message.success("创建迟交许可成功！");
        fetchExtensions();
      })
      .catch((err) => console.error(err))
      .finally(() => setDisabled(false));
  };

  const deleteExtension = (id) => {
    http()
      .delete(`/extensions/${id}`)
      .then(() => {
        message.success("删除迟交许可成功！");
        fetchExtensions();
      })
      .catch((err) => console.error(err));
  };

  const columns = [
    {
      title: "迟交ID",
      key: "id",
      dataIndex: "id",
      render: (id) => <code>{id.substr(-8)}</code>,
    },
    {
      title: "学生",
      key: "userId",
      dataIndex: "userId",
      render: (id) => <AdminUserInfo userId={id} />,
    },
    {
      title: "作业",
      key: "assignmentId",
      dataIndex: "assignmentId",
      render: (id) => <AdminAssignmentInfo assignmentId={id} />,
    },
    {
      title: "截止时间",
      key: "endTime",
      dataIndex: "endTime",
      render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss"),
    },
    {
      title: "迟交事由",
      key: "message",
      dataIndex: "message",
    },
    {
      title: "创建用户",
      key: "createdBy",
      dataIndex: "createdBy",
      render: (id) => <AdminUserInfo userId={id} />,
    },
    {
      title: "创建时间",
      key: "createdAt",
      dataIndex: "createdAt",
      render: (time) => moment(time).format("YYYY-MM-DD HH:mm:ss"),
    },
    {
      title: "操作",
      key: "actions",
      render: (record) => (
        <Popconfirm
          title="确定要删除此条许可吗？"
          onConfirm={() => deleteExtension(record.id)}
        >
          <Button type="link" size="small">
            删除
          </Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <>
      <Typography.Title level={2}>
        <ClockCircleOutlined /> 迟交管理
      </Typography.Title>
      <Form style={{ marginTop: "2em" }} form={form} onFinish={createExtension}>
        <Form.Item
          name="userId"
          label="学生"
          rules={[{ required: true, message: "请输入学生" }]}
        >
          <AdminUserSearch
            disabled={disabled}
            onSelect={(value, option) =>
              form.setFieldsValue({ userId: option.user.id })
            }
          />
        </Form.Item>
        <Form.Item
          name="assignmentId"
          label="作业"
          rules={[{ required: true, message: "请输入作业" }]}
        >
          <AdminAssignmentSearch
            disabled={disabled}
            onSelect={(value, option) =>
              form.setFieldsValue({ assignmentId: option.assignment.id })
            }
          />
        </Form.Item>
        <Form.Item
          name="endTime"
          label="截止时间"
          rules={[{ required: true, message: "请输入时间" }]}
        >
          <DatePicker showTime format="YYYY-MM-DD HH:mm" disabled={disabled} />
        </Form.Item>{" "}
        <Form.Item
          name="message"
          label="迟交事由"
          rules={[{ required: true, message: "请输入迟交事由" }]}
        >
          <TextArea />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" disabled={disabled}>
            创建迟交许可
          </Button>
        </Form.Item>
      </Form>
      <Divider />
      {!page ? (
        <Skeleton />
      ) : (
        <>
          <Table
            columns={columns}
            dataSource={page.content}
            rowKey="id"
            pagination={false}
          />
          <div style={{ float: "right", marginTop: "1em" }}>
            <Pagination
              current={page.number + 1}
              pageSize={page.size}
              total={page.totalElements}
              onChange={(p) =>
                history.push({
                  pathname: location.pathname,
                  search: `?page=${p}`,
                })
              }
            />
          </div>
        </>
      )}
    </>
  );
};

export default AdminExtensionList;
