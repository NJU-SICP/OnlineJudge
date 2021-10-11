import axios from "axios";
import config from "./config";
import {message} from "antd";

const http = () => {
    const options = {
        baseURL: config.baseNames.api,
        headers: {
            "Content-Type": "application/json"
        }
    };
    const instance = axios.create(options);
    const auth = JSON.parse(window.localStorage.getItem(config.storageKeys.auth));
    if (auth != null && auth.token != null) {
        instance.interceptors.request.use((config) => {
            config.headers.Authorization = `Bearer ${auth.token}`;
            return config;
        });
    }
    instance.interceptors.response.use((res) => {
        return res;
    }, (err) => {
        if (err.response.status === 401) {
            message.error(`您没有登录或您的凭证已过期，请重新登录！`);
            window.localStorage.removeItem(config.storageKeys.auth);
            window.location.href = `${config.baseNames.web}/auth/login?redirect=${window.location.pathname}`;
        } else if (err.response.status === 403) {
            message.error(`对不起，您没有权限访问该内容！`);
        }
        return Promise.reject(err);
    });
    return instance;
};

window.http = http;

export default http;
