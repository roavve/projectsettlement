<script setup>
defineProps({
  id: {
    type: String,
    required: true
  },
  question: {
    type: String,
    default: "ნამდვილად გსურთ წაშლა?"
  },
  acceptText: {
    type: String,
    default: "დიახ"
  },
  declineText: {
    type: String,
    default: "არა"
  }
})

const emits = defineEmits(['accept'])
</script>

<template>
  <dialog :id="id" class="modal">
    <div class="modal-box">
      <form method="dialog">
        <button class="btn btn-sm btn-circle btn-ghost absolute right-2 top-2 focus:outline-none">✕</button>
      </form>
      <h3 class="text-lg font-bold" v-text="question"/>
      <div class="modal-action !mt-16">
        <form method="dialog" class="flex items-center justify-center w-full gap-x-5">
          <button class="btn btn-neutral" v-text="acceptText" @click="emits('accept')"/>
          <button class="btn" v-text="declineText"/>
        </form>
      </div>
    </div>
  </dialog>
</template>