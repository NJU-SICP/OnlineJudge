const isDevelopment = !process.env.NODE_ENV || process.env.NODE_ENV === 'development';

const config = {
    version: "v1.3.1",
    storageKeys: {
        auth: "sicp-auth"
    },
    baseNames: {
        web: isDevelopment ? "/" : "/oj",
        api: isDevelopment ? "http://localhost:8080" : "https://sicp-api.njujb.com"
    }
};

export default config;
