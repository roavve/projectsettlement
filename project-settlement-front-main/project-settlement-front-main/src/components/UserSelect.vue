<script setup>
import { ref, onMounted } from 'vue';
import axios from '/src/interceptors/axios';

const props = defineProps({
  modelValue: {
    type: [Number, String, undefined],
    default: undefined
  },
  placeholder: {
    type: String,
    default: 'აირჩიეთ მომხმარებელი'
  }
});

const emit = defineEmits(['update:modelValue']);

const users = ref([]);

const fetchUsers = async () => {
  try {
    const { data } = await axios.get(`user`, {
      requiresAuth: true
    });
    users.value = data;
  } catch (error) {
    console.error('Error fetching users:', error);
  }
};

onMounted(fetchUsers);

const handleSelect = (event) => {
  const value = event.target.value;
  emit('update:modelValue', value === "" ? undefined : Number(value));
};

const clearSelection = () => {
  emit('update:modelValue', undefined);
};

</script>

<template>
  <div class="relative select-container">
    <select
      :value="modelValue === undefined ? '' : modelValue"
      @change="handleSelect"
      class="filter-select w-full"
    >
      <option value="">{{ placeholder }}</option>
      <option
        v-for="user in users"
        :key="user.id"
        :value="user.id"
      >
        {{ user.firstName }} {{ user.lastName }}
      </option>
    </select>
    <button class="filter-clear-btn" @click="clearSelection">✕</button>
  </div>
</template>

<style scoped>
.select-container {
  position: relative;
  display: flex;
  align-items: center;
}
</style>
