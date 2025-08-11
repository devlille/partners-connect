<template>
  <header>
    <h1>
      <img src="/img/logodfl.svg" alt="DevLille" />
    </h1>
    <button id="toggle-nav" aria-controls="main-nav" aria-expanded="false">
      Menu
    </button>
  </header>

  <div id="container">
    <div id="main-nav">
      <p class="k-skip">
        <a href="#page-body">Sauter la navigation</a>
      </p>
      <nav aria-label="Navigation principale">
        <ul>
          <li>
            <a href="index.html" aria-current="page">
              <svg role="img" width="24" height="24" aria-hidden="true">
                <use href="/img/sprite.svg#ic-sponsor"></use>
              </svg>
              Sponsors</a
            >
          </li>
          <li>
            <a href="#">
              <svg role="img" width="24" height="24" aria-hidden="true">
                <use href="/img/sprite.svg#ic-hall"></use>
              </svg>
              Hall
            </a>
          </li>
          <li>
            <a href="#">
              <svg role="img" width="24" height="24" aria-hidden="true">
                <use href="/img/sprite.svg#ic-parameters"></use>
              </svg>
              Paramètres
            </a>
          </li>
        </ul>
      </nav>
    </div>

    <main>
      <h2>
        <span>Sponsor</span>
        Zenika
      </h2>

      <div class="tabs-block">
        <ul class="tabs">
          <li>
            <button type="button">Information</button>
          </li>
          <li>
            <button type="button">Facturation</button>
          </li>
          <li role="presentation">
            <button type="button">Billeterie</button>
          </li>
          <li>
            <button type="button">Stand</button>
          </li>
        </ul>
        <div class="tab-content auto-scroll"></div>
        <div class="tab-content auto-scroll">
          <form class="m-b-double">
            <fieldset>
              <legend>Données de facturation</legend>
              <p>
                <label for="ip-siret">SIRET</label>
                <input type="text" id="ip-siret" name="ip-siret" />
              </p>
              <p>
                <label for="ip-devis">N&deg; devis</label>
                <input type="text" id="ip-devis" name="ip-devis" />
              </p>

              <p class="buttons-bar">
                <button>Générer</button>
              </p>
            </fieldset>
          </form>

          <h3>Liste de documents</h3>

          <ul class="docs-list">
            <li><a href="#">Facture 2024</a></li>
            <li><a href="#">Contrat 2025</a></li>
            <li><a href="#">Facture 2025</a></li>
          </ul>
        </div>
        <div class="tab-content auto-scroll">
          dklsfjs qkfsmdlkjf mslkdjfmlskjdf
        </div>
      </div>
    </main>
  </div>
  <Footer />
</template>

<script>
export default {
  head() {
    return {
      script: [
        {
          src: "/js/main-nav.js",
          type: "text/javascript",
          defer: true,
        },
        {
          src: "/js/autoScroll.js",
          type: "text/javascript",
          defer: true,
        },
        {
          src: "/js/tabs.js",
          type: "text/javascript",
          defer: true,
        },
      ],
    };
  },
};
</script>
