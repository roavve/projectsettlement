<script setup>
import {useAuthStore} from "/src/stores/auth.js";
import useNavigation from "/src/composables/useNavigation.js";

const authStore = useAuthStore();
const {
  showFiles,
  showTransactions,
  showUsers,
  showFileExport,
  checkCurrentRoute,
  downloadExport,
  exportLoading,
  getUserName,
  logout
} = useNavigation();
</script>

<template>
  <nav class="flex gap-x-5 items-center justify-between">
    <div class="flex items-center gap-x-5">
      <!-- Files Link -->
      <router-link v-if="showFiles()" class="btn" :class="{'btn-neutral': checkCurrentRoute('/files')}" :to="{name: 'Files'}">
        ფაილები
      </router-link>

      <!-- Transactions Link -->
      <router-link v-if="showTransactions()" class="btn" :class="{'btn-neutral': checkCurrentRoute('/')}"
                   :to="{name: 'Transactions'}">
        ჩარიცხვები
      </router-link>
    </div>

    <div v-if="authStore.isAuthenticated" class="flex items-center gap-x-5">
      <!-- File Export Link -->
      <button v-if="showFileExport()" class="text-white btn btn-success" type="button" :disabled="exportLoading" @click="downloadExport">
        <span v-if="!exportLoading">ექსპორტი</span>
        <span v-else>ფაილი იგზავნება...</span>
      </button>

      <!-- Users Link -->
      <router-link v-if="showUsers()" class="btn" :class="{'btn-neutral': checkCurrentRoute('/users')}" to="/users">
        მომხმარებლები
      </router-link>

      <!-- User Profile and Logout -->
      <div class="cursor-pointer items-center avatar placeholder gap-x-2.5">
        <button class="cursor-auto uppercase btn no-animation hover:bg-base-200 hover:border-base-200"
                v-text="getUserName()"></button>
        <button class="text-white btn btn-error" @click="logout">გასვლა</button>
      </div>
    </div>
  </nav>
</template>
