import axios from "axios";
import type { InternalAxiosRequestConfig } from "axios";
import { router } from "src/router";

const axiosInstanse = axios.create({
    baseURL: 'http://localhost:8080'
});

axiosInstanse.interceptors.request.use(
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

axiosInstanse.interceptors.response.use(
    (response) => response,
    async (error: { response?: { status: number } }) => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem("token");
            void router?.push("/login")
        }

        return Promise.reject(error);
    }
);

export default axiosInstanse;