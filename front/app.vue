<template>
  <UApp>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>

<script setup>
import "./app.css";

const colorMode = useColorMode();
colorMode.preference = "light";
</script>
