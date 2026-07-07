import {createRouter, createWebHistory} from "vue-router";
import routes from "./routes";
import {useAuthStore} from "/src/stores/auth.js";

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach((to, from, next) => {
    const authStore = useAuthStore();

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next({name: 'Login'});
    } else if (to.meta.requiresRoles && !to.meta.requiresRoles.includes(authStore.user?.role)) {
        next({name: 'Unauthorized'});
    } else {
        next();
    }
});

export default router;
