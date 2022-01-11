const isDevelopment = !process.env.NODE_ENV || process.env.NODE_ENV === 'development';

const config = {
    version: "2.0.2ws",
    storageKeys: {
        auth: "sicp-auth"
    },
    baseNames: {
        web: isDevelopment ? "/" : "/oj",
        api: "https://sicp-api.njujb.com:28300"
    }
};

export default config;
