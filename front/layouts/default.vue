<template>
  <div>
    <header>
      <MainTitle />
      <button id="toggle-nav" aria-controls="main-nav" aria-expanded="false">
        Menu
      </button>
    </header>
    <div id="container">
      <UApp>
      <NavigationMenu type="general" />

      <main class="auto-scroll">
        <slot />
      </main>
      </UApp>
    </div>

    <Footer />
  </div>
</template>

<script lang="ts" setup></script>

<style></style>
