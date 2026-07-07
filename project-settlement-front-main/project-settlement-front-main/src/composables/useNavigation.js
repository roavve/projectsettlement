import {useRouter} from "vue-router";
import {useAuthStore} from "/src/stores/auth.js";
import {useFilterStore} from "/src/stores/filter.js";
import axios from "axios";
import {ref} from "vue";

export default function useNavigation() {
    const router = useRouter();
    const authStore = useAuthStore();
    const {filter} = useFilterStore();

    const exportLoading = ref(false);

    /**
     * Check if the user can view files.
     * @returns {boolean}
     */
    const showFiles = () => authStore.isAuthenticated && authStore.filesRoles.includes(authStore.user?.role);

    /**
     * Check if the user can view transactions.
     * @returns {boolean}
     */
    const showTransactions = () => authStore.isAuthenticated && authStore.transactionsRoles.includes(authStore.user?.role);

    /**
     * Check if the user can view users.
     * @returns {boolean}
     */
    const showUsers = () => authStore.isAuthenticated && authStore.usersRoles.includes(authStore.user?.role);

    /**
     * Check if the user can export files.
     * @returns {boolean}
     */
    const showFileExport = () => authStore.isAuthenticated && authStore.fileExportRoles.includes(authStore.user?.role) && checkCurrentRoute('/');

    /**
     * Check if the current route matches the given path.
     * @param {string} path
     * @returns {boolean}
     */
    const checkCurrentRoute = (path) => router.currentRoute.value.path === path;

    const buildExportParams = () => {
        const params = {};

        const dates = [
            'clarificationDateStart',
            'clarificationDateEnd',
            /*'changeDateStart',
            'changeDateEnd',*/
            'transferDateStart',
            'transferDateEnd',
            'extractionDateStart',
            'extractionDateEnd'
        ]
        const undefinedValues = [
            "აირჩიეთ რეგიონი",
            "აირჩიეთ მ/ც",
            "აირჩიეთ სტატუსი",
            "ჩანაწერის სტატუსი",
            "ორდერის სტატუსი"
        ]
        Object.entries(filter)
            .filter(([_, value]) => value && !undefinedValues.includes(value))
            .reduce((_, [key, value]) => {
                if (dates.includes(key)) {
                    params[key] = value
                    if (key.indexOf('extraction') === -1) {
                        params[key] += ` ${key.indexOf('Start') !== -1 ? '00' : '24'}:00:00.000000`
                    }
                } else {
                    params[key] = value;
                }
            }, {});

        if (params['withdrawType']?.length === 0) {
            delete params['withdrawType'];
        }

        return params;
    };

    const downloadExport = async () => {
        if (exportLoading.value) return;
        exportLoading.value = true;

        const baseUrl = `${import.meta.env.VITE_BASE_URL}connection-fees/download`;
        const token = authStore.token;

        const params = {
            ...buildExportParams()
        };

        try {
            const response = await axios.get(baseUrl, {
                params,
                responseType: "blob",
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });

            let fileName = null;
            const cd = response.headers["content-disposition"];

            if (cd) {
                const idx = cd.toLowerCase().indexOf("filename=");
                if (idx !== -1) {
                    fileName = cd.substring(idx + 9).trim();
                    fileName = fileName.replace(/^"(.*)"$/, "$1");
                }
            }

            const blob = new Blob([response.data], {
                type: response.headers["content-type"] || "application/octet-stream",
            });

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;

            if (fileName) {
                link.setAttribute("download", fileName);
            }

            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error("Export download failed:", err);
        } finally {
            exportLoading.value = false;
        }
    };

    const downloadFile = async (fileName) => {
        const baseUrl = `${import.meta.env.VITE_BASE_URL}connection-fees/download-ext`;

        try {
            const response = await axios.get(baseUrl, {
                params: {
                    fileName
                },
                responseType: "blob",
                headers: {
                    Authorization: `Bearer ${authStore.token}`
                }
            });

            let downloadName = fileName;
            const cd = response.headers["content-disposition"];
            if (cd) {
                const match = cd.match(/filename\*?=UTF-8''([^;]+)/);
                if (match && match[1]) {
                    downloadName = decodeURIComponent(match[1]);
                }
            }

            const blob = new Blob([response.data], {
                type: response.headers["content-type"] || "application/octet-stream"
            });

            const url = window.URL.createObjectURL(blob);
            const link = document.createElement("a");
            link.href = url;
            link.setAttribute("download", downloadName);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            console.error("File download failed:", err);
        }
    };

    /**
     * Get the user's display name.
     * @returns {string}
     */
    const getUserName = () => {
        return authStore.user?.firstName ? `${authStore.user.firstName[0]}. ${authStore.user.lastName}` : '';
    };

    /**
     * Log the user out and navigate to the login page.
     */
    const logout = async () => {
        try {
            await authStore.logout();
            await router.push('/login');
        } catch (error) {
            console.error('Logout failed:', error);
        }
    };

    return {
        showFiles,
        showTransactions,
        showUsers,
        showFileExport,
        checkCurrentRoute,
        downloadExport,
        exportLoading,
        downloadFile,
        getUserName,
        logout,
    };
}
