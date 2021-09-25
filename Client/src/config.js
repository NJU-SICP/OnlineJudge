const isDevelopment = !process.env.NODE_ENV || process.env.NODE_ENV === 'development';

const config = {
    version: "v1.4.2",
    storageKeys: {
        auth: "sicp-auth"
    },
    baseNames: {
        web: isDevelopment ? "/" : "/oj",
        api: isDevelopment ? "http://localhost:8080" : "https://sicp-api.njujb.com:28300"
    }
};

export default config;
