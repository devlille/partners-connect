<template>
  <div>
    <header>
      <MainTitle />
      <button id="toggle-nav" aria-controls="main-nav" aria-expanded="false">Menu</button>
    </header>
    <div id="container">
      <NavigationMenu type="event" />
      <UApp>
        <main class="auto-scroll">
          <slot />
        </main>
      </UApp>
    </div>

    <Footer />
  </div>
</template>

<script lang="ts" setup></script>

<style src="~/assets/css/backoffice.css"></style>
