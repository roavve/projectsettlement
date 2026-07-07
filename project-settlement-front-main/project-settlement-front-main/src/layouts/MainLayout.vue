<script setup>
import HeaderComponent from "/src/components/HeaderComponent.vue";
</script>

<template>
  <HeaderComponent/>
  <main class="px-5 pt-2.5 pb-14">
    <slot/>
  </main>
</template>
