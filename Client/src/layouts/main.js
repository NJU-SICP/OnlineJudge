import {useSelector} from "react-redux";
import {Route, Redirect, Switch, useLocation} from "react-router-dom";

import {Layout, Menu} from "antd";
import {HomeOutlined} from '@ant-design/icons';
import Welcome from "../components/welcome";

const {SubMenu} = Menu;
const {Sider, Content} = Layout;

const MainLayout = ({children}) => {
    const location = useLocation();
    const auth = useSelector((state) => state.auth.value);
    return (
        <Route render={() =>
            !auth
                ? <Redirect to={{pathname: "/auth/login", search: `?redirect=${location.pathname}`}}/>
                : <>
                    <Layout>
                        <Sider width={200} className="site-layout-background">
                            <Menu mode="inline" style={{height: '100%', borderRight: 0}}
                                  defaultSelectedKeys={["welcome"]}>
                                <Menu.Item key="welcome" icon={<HomeOutlined/>}>系统主页</Menu.Item>
                            </Menu>
                        </Sider>
                        <Layout style={{padding: '1em'}}>
                            <Content style={{padding: "1em"}}>
                                <Switch>
                                    <Route path="/" exact children={<Welcome/>}/>
                                </Switch>
                            </Content>
                        </Layout>
                    </Layout>
                </>
        }/>
    );
};

export default MainLayout;
