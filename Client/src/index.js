import React from 'react';
import ReactDOM from 'react-dom';
import {HashRouter, Route, Switch} from "react-router-dom";

import {Provider} from "react-redux";
import store from "./store";

import './index.css';
import {ConfigProvider, Layout} from "antd";
import zhCN from 'antd/lib/locale/zh_CN';

import MainLayout from "./layouts/main";
import AuthLayout from "./layouts/auth";

console.info(
    `Welcome to SICP Online Judge!\n` +
    `To access API directly, call \`http()\` in console.\n` +
    `E.g. \`http().get("/assignments")\` fetches assignments.`
);

ReactDOM.render(
    <React.StrictMode>
        <Provider store={store}>
            <ConfigProvider locale={zhCN}>
                <HashRouter>
                    <Layout style={{minHeight: "100vh"}}>
                        <Switch>
                            <Route path="/auth/login" children={<AuthLayout/>}/>
                            <Route path="/" children={<MainLayout/>}/>
                        </Switch>
                    </Layout>
                </HashRouter>
            </ConfigProvider>
        </Provider>
    </React.StrictMode>,
    document.getElementById('root')
);
