<script setup>

import {useAuthStore} from "../stores/auth.js";
import useUploads from "../composables/useUploads.js";

const {formatDate} = useUploads()

defineProps({
  records: {
    type: Array,
    required: true
  },
  level: {
    type: Number,
    default: 0
  }
});

const emit = defineEmits(['handleEditClick', 'handleDivideClick'])

const handleEditClick = (extraction) => {
  if (extraction.status !== "CANCELED" && extraction.status !== "REMINDER") {
    emit("handleEditClick", extraction);
  }
}

const authStore = useAuthStore();
</script>

<template>
  <template v-for="extraction in records" :key="extraction.id">
    <tr class="cursor-pointer"
        :class="{'bg-yellow-100': extraction.status === 'REMINDER', 'bg-gray-300': extraction.children.length > 0, 'hover:bg-gray-100': extraction.children.length <= 0, 'hover:bg-yellow-100': extraction.status === 'REMINDER' }">
      <td title="გაყოფა" v-if="authStore.user">
        <button @click="emit('handleDivideClick', [extraction.id, extraction.remainder])">
          <img src="/src/assets/divide.svg" v-if="!extraction.queueNumber && extraction.remainder" alt="divide icon" class="max-w-8"/></button>
      </td>

      <td>
        <div :style="{ marginLeft: `${level * 10 + (level !== 0 && extraction.children.length === 0 ? 20 : 0)}px` }"
             class="flex items-center gap-x-1.5"><img
            v-if="extraction.children && extraction.children.length > 0"
            src="/src/assets/caret-right-thin.svg"
            alt="divide icon" :class="{'rotate-90': extraction.show}"
            class="cursor-pointer max-w-4 rounded-full border"
            @click="extraction.show = !extraction.show"/>
          <div v-else-if="level !== 0" class="w-1 aspect-square rounded-full bg-black"/>
          <p class="whitespace-nowrap" v-text="extraction.queueNumber ?? extraction.id"/></div>
      </td>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.orderN"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.region"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.serviceCenter"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.projectID"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.withdrawType"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.totalAmount"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.purpose"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.description"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.tax"/>

      <td @dblclick="handleEditClick(extraction)"
          v-text="extraction.extractionDate ? formatDate(extraction.extractionDate) : ''"
          :title="`${extraction.transferPerson?.firstName} ${extraction.transferPerson?.lastName}`"/>

      <td @dblclick="handleEditClick(extraction)"
          v-text="extraction.clarificationDate ? formatDate(extraction.clarificationDate) : ''"/>

      <td @dblclick="handleEditClick(extraction)"
          v-text="extraction.paymentOrderSentDate ? formatDate(extraction.paymentOrderSentDate) : ''"/>

      <td @dblclick="handleEditClick(extraction)"
          v-text="extraction.treasuryRefundDate ? formatDate(extraction.treasuryRefundDate) : ''"/>

      <td @dblclick="handleEditClick(extraction)" v-text="extraction.historyId"/>

      <td @dblclick="handleEditClick(extraction)"
          v-text="extraction.changePerson ? `${extraction.changePerson.firstName} ${extraction.changePerson.lastName}` : ''"/>
    </tr>
    <RecursiveRow @handleDivideClick="emit('handleDivideClick', $event)"
                  @handleEditClick="emit('handleEditClick', $event)"
                  v-if="extraction.children && extraction.children.length > 0 && extraction.show"
                  :records="extraction.children" :level="level + 1"/>
  </template>
</template>
