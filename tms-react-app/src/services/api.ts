import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    withCredentials: true,
});

api.interceptors.request.use((config) => {
    // Attach Access Token
    const token = localStorage.getItem("accessToken");
    const isPublicAuth = /\/(login|refreshToken)$/.test(config.url ?? "");
    if (token && !isPublicAuth) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    // Attach CSRF Token from Cookie
    const getCookie = (name: string) => {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop()?.split(';').shift();
        return null;
    };

    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken) {
        config.headers['X-XSRF-TOKEN'] = csrfToken;
    }


    return config;
});

api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;

        if (status === 401) {
            // Dispatch a custom event so AuthContext can handle the cleanup and redirect smoothly
            window.dispatchEvent(new Event('auth:logout'));
        }

        return Promise.reject(error);
    }
);

export default api;
