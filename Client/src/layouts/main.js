import React, {useEffect} from "react";
import {useSelector, useDispatch} from "react-redux";
import {Route, Redirect, Switch, useLocation} from "react-router-dom";
import moment from "moment";
import {set} from "../store/auth";
import http from "../http";
import config from "../config";

import {Layout} from "antd";
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
import AdminSubmissionList from "../components/admin/submissions/list";
import AdminSubmissionTokens from "../components/admin/submissions/tokens";
import AdminBackupList from "../components/admin/backups/list";
import AdminScoreTable from "../components/admin/score/table";
import BackupList from "../components/assignments/backups";

const MainLayout = () => {
    const auth = useSelector((state) => state.auth.value);
    const dispatch = useDispatch();
    const location = useLocation();

    useEffect(() => {
        if (auth && moment(auth.issued).isBefore(moment().subtract(1, "hours"))) {
            http()
                .post(`/auth/refresh`, {
                    platform: `web-${config.version}`
                })
                .then((res) => dispatch(set(res.data)))
                .catch((err) => console.error(err));
        }
    }, [auth, dispatch]);

    return (
        <Route render={() =>
            !auth
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
                                <Route path="/admin/submissions/tokens" children={<AdminSubmissionTokens/>}/>
                                <Route path="/admin/submissions" children={<AdminSubmissionList/>}/>
                                <Route path="/admin/backups" children={<AdminBackupList/>}/>
                                <Route path="/admin/score-table" children={<AdminScoreTable/>}/>
                                <Route path="/assignments/:id/backups" children={<BackupList/>}/>
                                <Route path="/assignments/:id" children={<AssignmentView/>}/>
                                <Route path="/assignments" children={<AssignmentList/>}/>
                                <Route path="/config" children={<UserConfig/>}/>
                                <Route path="/" exact children={<Welcome/>}/>
                            </Switch>
                        </div>
                    </Layout>
                </>
        }/>
    );
};

export default MainLayout;
