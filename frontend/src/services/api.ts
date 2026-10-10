import axios from "axios";
import type { InternalAxiosRequestConfig } from "axios";

import { router } from "src/router";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080'
});

api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const token = localStorage.getItem('token');

        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        return config;
    }, (error: Error) => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
        // A wrong password on the login form also answers 401, that is not an expired session
        const isAuthRequest = axios.isAxiosError(error) && (error.config?.url ?? '').startsWith('/api/auth/')
        if (axios.isAxiosError(error) && error.response?.status === 401 && !isAuthRequest) {
            localStorage.removeItem("token");
            void router?.push("/login")
        }

        return Promise.reject(error instanceof Error ? error : new Error('Request failed'));
    }
);

export default api;