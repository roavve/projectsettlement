<script setup>
import {onMounted, ref} from "vue";
import Message from "../modals/Message.vue";

const fileInput = ref(null);
const selectedFileName = ref('ფაილი არჩეული არ არის'); // Default message
const showUploadButton = ref(false);
const message = ref()

const triggerFileInput = () => {
  fileInput.value.click();
};

const emit = defineEmits(['createFile']);

const model = defineModel()

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    model.value = file;
    selectedFileName.value = file.name;
    showUploadButton.value = true;
  }
  event.target.value = '';
};

const upload = () => {
  try {
    emit('createFile');
    selectedFileName.value = '';
    showUploadButton.value = false;
    message.value = 'ფაილი აიტვირთა წარმატებით';
  } catch (e) {
    message.value = 'შეცდომა ფაილის დამუშავებისას'
  }
}
</script>

<template>
  <div class="flex items-center gap-x-5 mb-3.5">
    <div class="flex items-center gap-x-2.5">
      <input
          ref="fileInput"
          type="file"
          @change="handleFileChange"
          class="hidden"
      />
      <button
          @click="triggerFileInput"
          class="btn btn-sm"
      >
        აირჩიეთ ფაილი
      </button>
      <p class="text-sm">{{ selectedFileName }}</p>
    </div>
    <button
        v-if="showUploadButton"
        class="btn btn-sm"
        @click="upload"
    >
      ატვირთვა
    </button>
  </div>
  <message v-if="message" :message="message"/>
</template>
