<template>
  <div class="flex flex-col items-center justify-center mt-72">
    <!-- Error Code -->
    <h1 class="text-6xl font-bold text-red-600 mb-4">403</h1>
    <!-- Error Message -->
    <p class="text-lg text-gray-600 mb-8">თქვენ არ გაქვთ ამ გვერდზე წვდომის უფლება.</p>
    <!-- Link to Home Page -->
    <router-link
        :to="{name: 'Transactions'}"
        class="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 transition">
      მთავარ გვერდზე დაბრუნება
    </router-link>
  </div>
</template>
