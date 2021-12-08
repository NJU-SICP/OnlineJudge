import React, {useEffect, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {Route, Redirect, Switch, useLocation} from "react-router-dom";
import qs from "qs";
import http from "../http";
import config from "../config";
import {set} from "../store/auth";

import {Layout, Typography} from "antd";
import Header from "../components/header";
import Menu from "../components/menu";
import Welcome from "../components/welcome";
import AssignmentList from "../components/assignments/list";
import AssignmentView from "../components/assignments/view";
import UserConfig from "../components/config";
import AdminUserList from "../components/admin/users/list";
import AdminUserCreator from "../components/admin/users/creator";
import AdminUserImport from "../components/admin/users/import";
import AdminUserEditor from "../components/admin/users/editor";
import AdminAssignmentList from "../components/admin/assignments/list";
import AdminAssignmentCreator from "../components/admin/assignments/creator";
import AdminAssignmentEditor from "../components/admin/assignments/editor";
import AdminAssignmentGrader from "../components/admin/assignments/grader";
import AdminPlagiarismList from "../components/admin/plagiarisms/list";
import AdminSubmissionList from "../components/admin/submissions/list";
import AdminSubmissionTokens from "../components/admin/submissions/tokens";
import AdminBackupList from "../components/admin/backups/list";
import AdminScoreTable from "../components/admin/score/table";
import BackupList from "../components/assignments/backups";
import HogContest from "../components/contests/hog/contest";
import {LoadingOutlined} from "@ant-design/icons";

const MainLayout = () => {
    const auth = useSelector((state) => state.auth.value);
    const dispatch = useDispatch();
    const location = useLocation();

    const [holdOn, setHoldOn] = useState(true);

    // Check for oauth callback. If is callback, fetch access token and set to redux.
    useEffect(() => {
        const params = qs.parse(window.location.search, {ignoreQueryPrefix: true});
        if (!params.state || !params.state.startsWith("oauth") || !params.code) {
            setHoldOn(false);
        } else {
            const parts = params.state.split("-");
            if (parts.length !== 3) {
                window.alert(`无效的状态参数！\n${params.state}`);
                window.location.href = config.baseNames.web;
            } else {
                const url = atob(parts[1]);
                const redirect = atob(parts[2]);
                http().post(url, {
                    code: params.code,
                    state: params.state,
                    platform: `web-${config.version}`
                })
                    .then((res) => {
                        if (res.status === 200) {
                            dispatch(set(res.data));
                        }
                        window.location.href = `${config.baseNames.web}#`;
                    })
                    .catch((err) => {
                        console.error(err);
                        window.location.href = `${config.baseNames.web}#/auth/login?redirect=${redirect}` +
                            `&error=${err.response.data.message}`;
                    });
            }
        }
    }, [dispatch]);

    return (
        <Route render={() =>
            holdOn
                ? <Layout style={{
                    paddingTop: "10vh",
                    paddingBottom: "10vh",
                    paddingLeft: "10vw",
                    paddingRight: "10vw"
                }}>
                    <Typography.Title level={2}>
                        <LoadingOutlined/> 正在处理……
                    </Typography.Title>
                </Layout>
                : <>
                    {!auth
                        ? <Redirect to={{pathname: "/auth/login", search: `?redirect=${location.pathname}`}}/>
                        : <>
                            <Header/>
                            <Layout>
                                <Layout.Sider width="15em" collapsedWidth="0" breakpoint="lg">
                                    <Menu/>
                                </Layout.Sider>
                                <div style={{padding: '5em', width: "100%"}}>
                                    <Switch>
                                        <Route path="/admin/users/create" children={<AdminUserCreator/>}/>
                                        <Route path="/admin/users/import" children={<AdminUserImport/>}/>
                                        <Route path="/admin/users/:id" children={<AdminUserEditor/>}/>
                                        <Route path="/admin/users" children={<AdminUserList/>}/>
                                        <Route path="/admin/assignments/create" children={<AdminAssignmentCreator/>}/>
                                        <Route path="/admin/assignments/:id/grader" children={<AdminAssignmentGrader/>}/>
                                        <Route path="/admin/assignments/:id" children={<AdminAssignmentEditor/>}/>
                                        <Route path="/admin/assignments" children={<AdminAssignmentList/>}/>
                                        <Route path="/admin/plagiarisms" children={<AdminPlagiarismList/>}/>
                                        <Route path="/admin/submissions/tokens" children={<AdminSubmissionTokens/>}/>
                                        <Route path="/admin/submissions" children={<AdminSubmissionList/>}/>
                                        <Route path="/admin/backups" children={<AdminBackupList/>}/>
                                        <Route path="/admin/score-table" children={<AdminScoreTable/>}/>
                                        <Route path="/assignments/:id/backups" children={<BackupList/>}/>
                                        <Route path="/assignments/:id" children={<AssignmentView/>}/>
                                        <Route path="/assignments" children={<AssignmentList/>}/>
                                        <Route path="/contests/hog" children={<HogContest/>}/>
                                        <Route path="/config" children={<UserConfig/>}/>
                                        <Route path="/" exact children={<Welcome/>}/>
                                    </Switch>
                                </div>
                            </Layout>
                        </>}
                </>}/>
    );
};

export default MainLayout;
