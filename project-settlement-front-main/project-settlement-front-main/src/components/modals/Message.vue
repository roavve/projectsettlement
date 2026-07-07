<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="show" class="message fixed bottom-4 right-10 bg-white p-4 rounded-lg shadow-md max-w-sm">
        {{ message }}
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import {ref, watch} from 'vue';

const props = defineProps({
  message: {
    type: String,
    required: true
  }
});

const show = ref(false);

function hideRequest() {
  show.value = false;
}

watch(
    () => props.message,
    () => {
      show.value = true;
      setTimeout(hideRequest, 3000);
    },
    {immediate: true}
);
</script>

<style scoped>
.message {
  padding: 1rem;
  border-radius: 0.5rem;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter, .fade-leave-to /* .fade-leave-active in <2.1.8 */
{
  opacity: 0;
}
</style>
