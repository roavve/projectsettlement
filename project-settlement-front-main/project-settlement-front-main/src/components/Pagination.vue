<script setup>
import usePagination from "/src/composables/usePagination.js";

const currentPage = defineModel('current-page', {
  type: Number,
  default: 1,
  required: true
});
const pageSize = defineModel('page-size', {
  type: Number,
  default: 20,
  required: true,
});
const totalPages = defineModel('total-pages', {
  type: Number,
  default: 1,
  required: true,
});
const totalElements = defineModel('total-elements', {
  type: Number,
  default: 20,
  required: true,
});

const props = defineProps({
  absolute: {
    type: Boolean,
    default: false
  }
})

const {
  showOptions,
  startIndex,
  endIndex,
  previousPage,
  nextPage,
  onEnter,
  validatePage,
  options,
} = usePagination(currentPage, pageSize, totalPages, totalElements);
</script>

<template>
  <div class="bg-white p-4 flex justify-center items-center gap-x-10" :class="[absolute ? 'w-full' : 'fixed bottom-0 w-screen']">
    <div class="flex items-center gap-x-4 relative">
      <span><strong>{{ startIndex }}</strong> - <strong>{{ endIndex }}</strong> of <strong>{{ totalElements }}</strong></span>
      <i class="fas fa-caret-down cursor-pointer" @click="showOptions = !showOptions"/>
      <ul class="absolute flex flex-col bottom-10 bg-white shadow" v-if="showOptions">
        <li v-for="(option, index) in options" :key="index"
            @click="() => { showOptions = false; pageSize = option; currentPage = 1; }"
            class="flex items-center gap-x-2.5 whitespace-nowrap cursor-pointer hover:bg-gray-300 py-2 px-3.5 text-sm">
          {{ option }} გვერდზე <i v-if="pageSize === option" class="fa-solid fa-check text-blue-500"></i>
        </li>
      </ul>
    </div>

    <div class="flex items-center gap-x-7">
      <i class="fas fa-angle-double-left cursor-pointer" :class="{ 'text-gray-300 cursor-default': currentPage === 1 }"
         @click="() => { currentPage = 1; }"/>
      <i class="fas fa-angle-left cursor-pointer" :class="{ 'text-gray-300 cursor-default': currentPage === 1 }"
         @click="previousPage"/>
      <div class="flex items-center gap-x-1">
        <input @input="validatePage" class="w-8 text-center border no-spinner" type="number" min="1" :max="totalPages"
               :value="currentPage" @keyup.enter="onEnter"/>
        <span>of {{ totalPages }}</span>
      </div>
      <i class="fas fa-angle-right cursor-pointer"
         :class="{ 'text-gray-300 cursor-default': currentPage === totalPages }" @click="nextPage"/>
      <i class="fas fa-angle-double-right cursor-pointer"
         :class="{ 'text-gray-300 cursor-default': currentPage === totalPages }"
         @click="() => { currentPage = totalPages }"/>
    </div>
  </div>
</template>

<style scoped>
.no-spinner::-webkit-outer-spin-button,
.no-spinner::-webkit-inner-spin-button {
  appearance: none;
}

.no-spinner {
  appearance: textfield;
}
</style>
