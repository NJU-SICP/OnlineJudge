const isDevelopment = !process.env.NODE_ENV || process.env.NODE_ENV === 'development';

const config = {
    version: "2022.10.06",
    storageKeys: {
        auth: "sicp-auth"
    },
    baseNames: {
        web: "/",
        api: "https://sicp.pascal-lab.net/api"
    }
};

export default config;
