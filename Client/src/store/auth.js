import {createSlice} from "@reduxjs/toolkit";
import config from "../config";

const isSSR = () => typeof window === `undefined`;
const loadAuth = () => {
    if (isSSR()) return null;
    const json = window.localStorage.getItem(config.storageKeys.auth);
    try {
        return !!json ? JSON.parse(json) : null;
    } catch (err) {
        console.error(`Cannot load auth from localStorage: ${err}`);
        window.localStorage.removeItem(config.storageKeys.auth);
        return null;
    }
};

export const authSlice = createSlice({
    name: "auth",
    initialState: {
        value: loadAuth()
    },
    reducers: {
        set: (state, action) => {
            state.value = action.payload;
            if (!isSSR()) {
                const json = JSON.stringify(state.value);
                window.localStorage.setItem(config.storageKeys.auth, json);
            }
        },
        clear: (state) => {
            state.value = null;
            if (!isSSR()) {
                window.localStorage.removeItem(config.storageKeys.auth);
            }
        }
    }
});

export const {set, clear} = authSlice.actions;

export default authSlice.reducer;
