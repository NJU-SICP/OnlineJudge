import {useSelector} from "react-redux";
import {Route, Redirect, Switch, useLocation} from "react-router-dom";

import {Layout} from "antd";
import Welcome from "../components/welcome";
import Menu from "../components/menu";
import AdminUserList from "../components/admin/users/list";
import AdminUserCreator from "../components/admin/users/creator";
import AdminUserEditor from "../components/admin/users/editor";
import AdminAssignmentList from "../components/admin/assignments/list";
import AdminAssignmentCreator from "../components/admin/assignments/creator";
import AdminAssignmentEditor from "../components/admin/assignments/editor";
import AssignmentList from "../components/assignments/list";
import AssignmentView from "../components/assignments/view";
import UserConfig from "../components/config";
import AdminAssignmentGrader from "../components/admin/assignments/grader";
import AdminSubmissionList from "../components/admin/submissions/list";
import Header from "../components/header";
import React from "react";

const MainLayout = () => {
    const location = useLocation();
    const auth = useSelector((state) => state.auth.value);
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
                                <Route path="/admin/users/:id" children={<AdminUserEditor/>}/>
                                <Route path="/admin/users" children={<AdminUserList/>}/>
                                <Route path="/admin/assignments/create" children={<AdminAssignmentCreator/>}/>
                                <Route path="/admin/assignments/:id/grader" children={<AdminAssignmentGrader/>}/>
                                <Route path="/admin/assignments/:id" children={<AdminAssignmentEditor/>}/>
                                <Route path="/admin/assignments" children={<AdminAssignmentList/>}/>
                                <Route path="/admin/submissions" children={<AdminSubmissionList/>}/>
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
