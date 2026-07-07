<script setup>
import {useUsers} from '/src/composables/useUsers';
import Confirm from "/src/components/modals/Confirm.vue";
import {computed, ref} from "vue";
import useUploads from "/src/composables/useUploads.js";

const {
  users,
  showModal,
  selectedUser,
  isEditing,
  roles,
  hasErrors,
  openCreateModal,
  editUser,
  saveUser,
  deleteUser
} = useUsers();

const deleteId = ref()

const {
  formatDate,
} = useUploads();

const showPassword = ref(false);
const passwordFieldType = computed(() => (showPassword.value ? 'text' : 'password'));
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
};
</script>

<template>
  <div class="container mx-auto p-4">
    <h1 class="text-xl font-bold mb-4">მომხმარებელთა სამართავი პანელი</h1>

    <!-- Create User Button -->
    <button @click="openCreateModal"
            class="btn btn-success text-white mb-4 btn-sm">
      მომხმარებლის დამატება
    </button>

    <table class="min-w-full table-auto">
      <thead>
      <tr class="bg-gray-200">
        <th class="px-4 py-2">ID</th>
        <th class="px-4 py-2">სახელი</th>
        <th class="px-4 py-2">გვარი</th>
        <th class="px-4 py-2">ემაილი</th>
        <th class="px-4 py-2">როლი</th>
        <th class="px-4 py-2">შექმნის თარიღი</th>
        <th class="px-4 py-2">განახლების თარიღი</th>
        <th class="px-4 py-2">ქმედებები</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="user in users" :key="user.id" class="text-center">
        <td class="border px-4 py-2">{{ user.id }}</td>
        <td class="border px-4 py-2">{{ user.firstName }}</td>
        <td class="border px-4 py-2">{{ user.lastName }}</td>
        <td class="border px-4 py-2">{{ user.email }}</td>
        <td class="border px-4 py-2">{{ user.role }}</td>
        <td class="border px-4 py-2">{{ formatDate(user.createdAt) }}</td>
        <td class="border px-4 py-2">{{ formatDate(user.updatedAt).toLocaleString() }}</td>
        <td class="border px-4 py-2">
          <button @click="editUser(user)" class="btn btn-sm btn-info text-white">
            შეცვლა
          </button>
          <button
              onclick="cnfrm_0.showModal()"
              @click="deleteId = user.id"
              class="btn btn-error text-white btn-sm ml-2.5">წაშლა
          </button>
        </td>
      </tr>
      </tbody>
    </table>

    <confirm id="cnfrm_0" @accept="deleteUser(deleteId)"/>

    <!-- Edit/Create Modal -->
    <div v-if="showModal" class="fixed inset-0 flex items-center justify-center bg-gray-900 bg-opacity-50">
      <div class="bg-white p-6 rounded-lg w-1/3">
        <h2 class="text-xl font-bold mb-4">{{ isEditing ? 'მომხმარებლის ცვლილება' : 'მომხმარებლის შექმნა' }}</h2>
        <div class="mb-4">
          <label class="block mb-1">სახელი</label>
          <input v-model="selectedUser.firstName" type="text" class="w-full border px-3 py-2 rounded-lg"/>
        </div>
        <div class="mb-4">
          <label class="block mb-1">გვარი</label>
          <input v-model="selectedUser.lastName" type="text" class="w-full border px-3 py-2 rounded-lg"/>
        </div>
        <div class="mb-4">
          <label class="block mb-1">ემაილი</label>
          <input v-model="selectedUser.email" type="email" class="w-full border px-3 py-2 rounded-lg"/>
        </div>
        <div class="mb-4">
          <label class="block mb-1">პაროლი</label>
          <div class="relative">
            <input v-model="selectedUser.password" :type="passwordFieldType"
                   class="w-full border px-3 py-2 rounded-lg"/>
            <!--TODO - Items must be downloaded-->
            <i @click="togglePasswordVisibility"
               :class="['absolute right-3 top-1/2 transform -translate-y-1/2 cursor-pointer', showPassword ? 'fa fa-eye-slash' : 'fa fa-eye']"></i>
          </div>
        </div>
        <div class="mb-4">
          <label class="block mb-1">როლი</label>
          <select class="w-full border px-3 py-2 rounded-lg"
                  v-model="selectedUser.role">
            <option disabled selected>აირჩიეთ როლი</option>
            <option :value="role.key" v-for="(role, index) in roles" v-text="role.text" :key="index"/>
          </select>
        </div>
        <div class="flex justify-end gap-x-5">
          <button @click="saveUser" class="btn btn-success text-white" :disabled="hasErrors">
            {{ isEditing ? 'შენახვა' : 'შექმნა' }}
          </button>
          <button @click="showModal = false"
                  class="btn">გაუქმება
          </button>
        </div>
      </div>
    </div>
  </div>

</template>