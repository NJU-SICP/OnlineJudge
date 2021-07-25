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

const MainLayout = () => {
    const location = useLocation();
    const auth = useSelector((state) => state.auth.value);
    return (
        <Route render={() =>
            !auth
                ? <Redirect to={{pathname: "/auth/login", search: `?redirect=${location.pathname}`}}/>
                : <>
                    <Layout>
                        <Layout.Sider width={200} className="site-layout-background">
                            <Menu/>
                        </Layout.Sider>
                        <Layout style={{padding: '1em'}}>
                            <Layout.Content style={{padding: "1em"}}>
                                <Switch>
                                    <Route path="/admin/users/create" children={<AdminUserCreator/>}/>
                                    <Route path="/admin/users/:id" children={<AdminUserEditor/>}/>
                                    <Route path="/admin/users" children={<AdminUserList/>}/>
                                    <Route path="/admin/assignments/create" children={<AdminAssignmentCreator/>}/>
                                    <Route path="/admin/assignments/:id" children={<AdminAssignmentEditor/>}/>
                                    <Route path="/admin/assignments" children={<AdminAssignmentList/>}/>
                                    <Route path="/assignments/:id" children={<AssignmentView/>}/>
                                    <Route path="/assignments" children={<AssignmentList/>}/>
                                    <Route path="/config" children={<UserConfig/>}/>
                                    <Route path="/" exact children={<Welcome/>}/>
                                </Switch>
                            </Layout.Content>
                        </Layout>
                    </Layout>
                </>
        }/>
    );
};

export default MainLayout;
