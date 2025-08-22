<template>
  <div>
    <header>
      <MainTitle />
      <button id="toggle-nav" aria-controls="main-nav" aria-expanded="false">
        Menu
      </button>
    </header>
    <div id="container">
      <EventMenu />

      <main class="auto-scroll">
        <slot />
      </main>
    </div>

    <Footer />
  </div>
</template>

<script lang="ts" setup></script>

<style></style>
