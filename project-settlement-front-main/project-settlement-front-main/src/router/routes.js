const routes = [
    {
        path: "/",
        name: "Transactions",
        meta: {requiresAuth: true, requiresRoles: ["ROLE_GUEST", "ROLE_OPERATOR", "ROLE_MANAGER", "ROLE_ADMIN"]},
        component: () => import("/src/views/Transactions.vue"),
    },
    {
        path: "/files",
        name: "Files",
        meta: {requiresAuth: true, requiresRoles: ["ROLE_MANAGER", "ROLE_ADMIN"]},
        component: () => import("/src/views/Files.vue"),
    },
    {
        path: "/login",
        name: "Login",
        component: () => import("/src/views/Login.vue"),
    },
    {
        path: "/users",
        name: "Users",
        meta: {requiresAuth: true, requiresRoles: ["ROLE_ADMIN"]},
        component: () => import("/src/views/Users.vue"),
    },
    {
        path: "/unauthorized",
        name: "Unauthorized",
        component: () => import("/src/views/Unauthorized.vue"),
    },
    {
        path: "/:pathMatch(.*)*",
        name: "NotFound",
        component: () => import("/src/views/NotFound.vue"),
    },
];

export default routes;
