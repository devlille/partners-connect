<template>
  <div id="main-nav">
      <p class="k-skip">
        <a href="#page-body">Sauter la navigation</a>
      </p>
      <nav aria-label="Navigation principale">
        <ul>
          <li>
            <NuxtLink to="/sponsor">
              <svg role="img" width="24" height="24" aria-hidden="true">
                <use href="/img/sprite.svg#ic-sponsor"></use>
              </svg>
              Sponsors</NuxtLink
            >
            </li>
          <li>
            <NuxtLink to="/hall">
              <svg role="img" width="24" height="24" aria-hidden="true">
                <use href="/img/sprite.svg#ic-hall"></use>
              </svg>
              Hall
            </NuxtLink>
          </li>
          <li>
            <NuxtLink to="/settings">
              <svg role="img" width="24" height="24" aria-hidden="true">
                <use href="/img/sprite.svg#ic-parameters"></use>
              </svg>
              Paramètres
            </NuxtLink>
          </li>
        </ul>
      </nav>
    </div>

</template>

<script lang="ts" setup>

</script>

<style>

</style>