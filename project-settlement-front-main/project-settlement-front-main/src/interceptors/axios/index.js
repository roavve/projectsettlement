import axios from "axios";
import {useAuthStore} from "/src/stores/auth";

const axiosInstance = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "ngrok-skip-browser-warning": true
    }
});

axiosInstance.interceptors.request.use(config => {
    const authStore = useAuthStore();

    if (config.requiresAuth && authStore.token) {
        config.headers.Authorization = `Bearer ${authStore.token}`;
    }

    return config;
}, error => {
    return Promise.reject(error);
});

export default axiosInstance;
