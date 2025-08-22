<template>
  <h2>Liste de Sponsors</h2>
  <details>
    <summary>
      <h3>
        <svg role="img" width="24" height="24" aria-hidden="true">
          <use href="/img/sprite.svg#ic-filter"/>
        </svg>
        Filtres
      </h3>
    </summary>
    <div class="content">
      <form class="filters">
        <div class="grid">
          <fieldset class="choices">
            <legend>Pack:</legend>
            <ul>
              <li>
                <input
                  id="in-gold"
                  type="checkbox"
                  name="pack"
                  value="in-gold"
                >
                <label for="in-gold">Gold</label>
              </li>
              <li>
                <input
                  id="in-silver"
                  type="checkbox"
                  name="pack"
                  value="in-silver"
                >
                <label for="in-silver">Silver</label>
              </li>
              <li>
                <input
                  id="in-bronze"
                  type="checkbox"
                  name="pack"
                  value="in-bronze"
                >
                <label for="in-bronze">Bronze</label>
              </li>
              <li>
                <input
                  id="in-free"
                  type="checkbox"
                  name="pack"
                  value="in-free"
                >
                <label for="in-free">Freelance</label>
              </li>
              <li>
                <input
                  id="in-community"
                  type="checkbox"
                  name="pack"
                  value="in-community"
                >
                <label for="in-community">Communauté</label>
              </li>
              <li>
                <input
                  id="in-media"
                  type="checkbox"
                  name="pack"
                  value="in-media"
                >
                <label for="in-media">Média</label>
              </li>
            </ul>
          </fieldset>
          <fieldset class="choices">
            <legend>Type:</legend>
            <ul>
              <li>
                <input id="in-esn" type="checkbox" name="type" value="in-esn" >
                <label for="in-esn">ESN</label>
              </li>
              <li>
                <input
                  id="in-notesn"
                  type="checkbox"
                  name="type"
                  value="in-notesn"
                >
                <label for="in-notesn">Pas ESN</label>
              </li>
            </ul>
          </fieldset>
        </div>
        <p class="buttons-bar">
          <input type="submit" value="Appliquer" >
          <input type="reset" value="Annuler" >
        </p>
      </form>
    </div>
  </details>

  <div class="results-list grid">
    <SponsorTile
      :partner="{
        id: 1,
        name: 'Zenika',
        billingDate: '01/02/2025',
        pack: 'Gold',
        status: 'Activé',
      }"
    />
  </div>
</template>

<script setup>
useHead({
  title: "Liste de Sponsors | DevLille"
});
</script>
