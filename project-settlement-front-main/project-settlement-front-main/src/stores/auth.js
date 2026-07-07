import {defineStore} from "pinia";
import {ref} from "vue";
import axios from "/src/interceptors/axios/index.js";
import cookies from "vue-cookies";

export const useAuthStore = defineStore("auth", () => {
    const filesRoles = ["ROLE_MANAGER", "ROLE_ADMIN"];
    const transactionsRoles = ["ROLE_GUEST", "ROLE_OPERATOR", "ROLE_MANAGER", "ROLE_ADMIN"];
    const usersRoles = ["ROLE_ADMIN"];
    const fileExportRoles = ["ROLE_GUEST", "ROLE_OPERATOR", "ROLE_MANAGER", "ROLE_ADMIN"];

    const user = ref(null);
    const token = ref(null);
    const isAuthenticated = ref(false);

    /**
     * Log in the user
     * @param {string} email
     * @param {string} password
     */
    const login = async (email, password) => {
        try {
            const {data} = await axios.post("auth/signin", {email, password});
            token.value = data.jwtAuthenticationResponse.token;
            user.value = data.user;
            isAuthenticated.value = true;

            cookies.set("auth_token", token.value);
            axios.defaults.headers.common["Authorization"] = `Bearer ${token.value}`;
        } catch (error) {
            console.error("Login failed:", error);
            throw error;
        }
    };

    /**
     * Log out the user
     */
    const logout = async () => {
        try {
            await axios.post("auth/logout");
            token.value = null;
            user.value = null;
            isAuthenticated.value = false;

            cookies.remove("auth_token");
            delete axios.defaults.headers.common["Authorization"];
        } catch (error) {
            console.error("Logout failed:", error);
        }
    };

    /**
     * Restore user session
     */
    const restoreSession = async () => {
        const savedToken = cookies.get("auth_token");
        if (savedToken) {
            token.value = savedToken;
            isAuthenticated.value = true;
            axios.defaults.headers.common["Authorization"] = `Bearer ${savedToken}`;
            try {
                const {data} = await axios.get("auth/user");
                user.value = data;
            } catch (error) {
                console.error("Failed to restore session:", error);
                await logout();
            }
        }
    };

    return {
        filesRoles,
        transactionsRoles,
        usersRoles,
        fileExportRoles,
        user,
        token,
        isAuthenticated,
        login,
        logout,
        restoreSession,
    };
});
