import {createApp} from "vue";
import "./style.css";
import '@fortawesome/fontawesome-free/css/all.css';
import App from "./App.vue";
import {createPinia} from 'pinia';
import {useAuthStore} from "./stores/auth.js";
import router from "./router";

const initializeApp = async () => {
    const app = createApp(App);
    const pinia = createPinia();
    app.use(pinia);

    const authStore = useAuthStore();
    try {
        await authStore.restoreSession();
    } catch (error) {
        console.error("Error restoring session:", error);
    }

    app.use(router);
    app.mount('#app');
};

initializeApp();
