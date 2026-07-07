<script setup>
import {computed} from "vue";

defineProps({
  label: {
    type: String,
    required: true,
  }
});

const startDate = defineModel('startDate', {
  default: null,
  required: true,
});
const endDate = defineModel('endDate', {
  default: null,
  required: true,
});

const emit = defineEmits(['clear']);

const handleClick = () => {
  emit('clear');
};

const showClear = computed(() => startDate.value || endDate.value);
</script>

<template>
  <div class="flex flex-col gap-y-2.5">
    <div class="flex items-center gap-x-1.5 h-7">
      <p class="filter-label pb-1" v-text="label"></p>

      <button class="btn btn-sm btn-circle btn-ghost" v-if="showClear" @click="handleClick">✕</button>
    </div>

    <div class="flex gap-x-2.5">
      <input type="date" class="text-sm" v-model="startDate"/>

      <input type="date" class="text-sm" v-model="endDate"/>
    </div>
  </div>
</template>
