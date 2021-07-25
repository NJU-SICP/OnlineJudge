import React from 'react';
import ReactDOM from 'react-dom';
import {BrowserRouter, Route, Switch} from "react-router-dom";

import {Provider} from "react-redux";
import store from "./store";

import './index.css';
import {Layout} from "antd";

import MainLayout from "./layouts/main";
import AuthLayout from "./layouts/auth";
import Header from "./components/header";

console.info(
    `Welcome to SICP Online Judge!\n` +
    `To access API directly, call \`http()\` in console.\n` +
    `E.g. \`http().get("/assignments")\` fetches assignments.`
);

ReactDOM.render(
    <React.StrictMode>
        <Provider store={store}>
            <BrowserRouter>
                <Layout style={{minHeight: "100vh"}}>
                    <Header/>
                    <Switch>
                        <Route path="/auth/login" children={<AuthLayout/>}/>
                        <Route path="/" children={<MainLayout/>}/>
                    </Switch>
                </Layout>
            </BrowserRouter>
        </Provider>
    </React.StrictMode>,
    document.getElementById('root')
);
