const isDevelopment = !process.env.NODE_ENV || process.env.NODE_ENV === 'development';

const config = {
    version: "2022.10.05",
    storageKeys: {
        auth: "sicp-auth"
    },
    baseNames: {
        web: isDevelopment ? "/" : "/oj",
        api: isDevelopment ? "http://localhost:8080" : "https://sicp.pascal-lab.net/api"
    }
};

export default config;
