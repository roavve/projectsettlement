import {ref, onMounted, computed} from 'vue';
import axios from '/src/interceptors/axios/index.js';

export function useUsers() {
    const users = ref([]);

    const showModal = ref(false);

    const selectedUser = ref({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'აირჩიეთ როლი'
    });

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    const errors = computed(() => ({
        firstName: !selectedUser.value.firstName,
        lastName: !selectedUser.value.lastName,
        email: !emailRegex.test(selectedUser.value.email),
        password: !isEditing && !selectedUser.value.password,
        role: selectedUser.value.role === 'აირჩიეთ როლი',
    }));

    const hasErrors = computed(() => Object.values(errors.value).some(error => error));

    const isEditing = ref(false);

    const roles = [
        {key: 'ROLE_ADMIN', text: 'ადმინი'},
        {key: 'ROLE_OPERATOR', text: 'ოპერატორი'},
        {key: 'ROLE_MANAGER', text: 'მენეჯერი'},
        {key: 'ROLE_GUEST', text: 'სტუმარი'}
    ];

    const getUsers = async () => {
        const {data} = await axios.get('user', {requiresAuth: true});
        users.value = data;
    };

    const openCreateModal = () => {
        selectedUser.value = {
            firstName: '',
            lastName: '',
            email: '',
            password: '',
            role: 'აირჩიეთ როლი'
        };
        isEditing.value = false;
        showModal.value = true;
    };

    const editUser = (user) => {
        selectedUser.value = {...user};
        isEditing.value = true;
        showModal.value = true;
    };

    const saveUser = async () => {
        if (hasErrors.value) return;

        try {
            if (isEditing.value) {
                await axios.put(`user/${selectedUser.value.id}`, selectedUser.value, {requiresAuth: true});
            } else {
                await axios.post('auth/signup', selectedUser.value, {requiresAuth: true});
            }
            showModal.value = false;
            await getUsers();
        } catch (error) {
            console.error('Error saving user:', error);
        }
    };

    const deleteUser = async (userId) => {
        try {
            await axios.delete(`user/${userId}`, {requiresAuth: true});
            await getUsers();
        } catch (error) {
            console.error('Error deleting user:', error);
        }
    };

    onMounted(getUsers);

    return {
        users,
        showModal,
        selectedUser,
        isEditing,
        roles,
        hasErrors,
        getUsers,
        openCreateModal,
        editUser,
        saveUser,
        deleteUser,
    };
}
