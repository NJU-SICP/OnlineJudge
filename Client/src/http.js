import axios from "axios";
import config from "./config";

const http = () => {
    const options = {
        baseURL: config.baseURL,
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
    return instance;
};

window.http = http;

export default http;
