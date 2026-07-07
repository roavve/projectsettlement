<script setup>
import {ref} from 'vue';
import {useAuthStore} from '/src/stores/auth';
import {useRouter} from "vue-router";

const router = useRouter()

const email = ref('');
const password = ref('');
const authError = ref('');

const authStore = useAuthStore();

const handleLogin = async () => {
  try {
    authError.value = ''
    await authStore.login(email.value, password.value);
    await router.push({name: 'Transactions'});
  } catch (error) {
    authError.value = error.response.data.error;
  }
};
</script>

<template>
  <div class="flex justify-center items-center">
    <div class="w-full max-w-md p-8 space-y-6 shadow-lg rounded-lg mt-64">
      <h2 class="text-2xl font-bold text-center text-gray-900">ავტორიზაცია</h2>
      <form @submit.prevent="handleLogin">
        <div class="space-y-4">
          <div>
            <label for="email" class="block text-sm font-medium text-gray-700">ემაილი</label>
            <input v-model="email" id="email" type="email"
                   class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-success">
          </div>
          <div>
            <label for="password" class="block text-sm font-medium text-gray-700">პაროლი</label>
            <input v-model="password" id="password" type="password"
                   class="mt-1 w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:border-success">
          </div>
        </div>

        <p v-if="authError" class="text-sm text-error mt-4" v-text="authError"></p>

        <button type="submit"
                :class="{'!mt-4': authError}"
                class="mt-7 w-full py-2 px-4 text-white font-semibold rounded-md hover:bg-success glass bg-neutral">
          შესვლა
        </button>
      </form>
    </div>
  </div>
</template>
