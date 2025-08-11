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
            <a href="#" aria-current="page">
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

    <main class="auto-scroll">
      <h2>Liste de Sponsors</h2>
      <details>
        <summary>
          <h3>
            <svg role="img" width="24" height="24" aria-hidden="true">
              <use href="/img/sprite.svg#ic-filter"></use>
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
                      type="checkbox"
                      id="in-gold"
                      name="pack"
                      value="in-gold"
                    />
                    <label for="in-gold">Gold</label>
                  </li>
                  <li>
                    <input
                      type="checkbox"
                      id="in-silver"
                      name="pack"
                      value="in-silver"
                    />
                    <label for="in-silver">Silver</label>
                  </li>
                  <li>
                    <input
                      type="checkbox"
                      id="in-bronze"
                      name="pack"
                      value="in-bronze"
                    />
                    <label for="in-bronze">Bronze</label>
                  </li>
                  <li>
                    <input
                      type="checkbox"
                      id="in-free"
                      name="pack"
                      value="in-free"
                    />
                    <label for="in-free">Freelance</label>
                  </li>
                  <li>
                    <input
                      type="checkbox"
                      id="in-community"
                      name="pack"
                      value="in-community"
                    />
                    <label for="in-community">Communauté</label>
                  </li>
                  <li>
                    <input
                      type="checkbox"
                      id="in-media"
                      name="pack"
                      value="in-media"
                    />
                    <label for="in-media">Média</label>
                  </li>
                </ul>
              </fieldset>
              <fieldset class="choices">
                <legend>Type:</legend>
                <ul>
                  <li>
                    <input
                      type="checkbox"
                      id="in-esn"
                      name="type"
                      value="in-esn"
                    />
                    <label for="in-esn">ESN</label>
                  </li>
                  <li>
                    <input
                      type="checkbox"
                      id="in-notesn"
                      name="type"
                      value="in-notesn"
                    />
                    <label for="in-notesn">Pas ESN</label>
                  </li>
                </ul>
              </fieldset>
            </div>
            <p class="buttons-bar">
              <input type="submit" value="Appliquer" />
              <input type="reset" value="Annuler" />
            </p>
          </form>
        </div>
      </details>
      <!-- <div class="actions-bar">
                <p>
                    <button class="with-icon ic-only">
                        <svg role="img" width="24" height="24" aria-hidden="true">
                            <use href="/img/sprite.svg#ic-filter"></use>
                        </svg>
                        Filtre
                    </button>
                </p>

                <fieldset class="switch display-mode">
                    <legend>Type d'affichage</legend>
                        <ul>
                            <li>
                                <input 
                                    type="radio" 
                                    name="affichage" 
                                    id="rb-aff-carte"
                                    class="card-view" 
                                    checked>
                                <label for="rb-aff-carte">Affichage par carte</label>
                            </li>
                            <li>
                                <input 
                                    type="radio" 
                                    name="affichage"
                                    id="rb-aff-tab" 
                                    class="table-view">
                                <label for="rb-aff-tab">Affichage par tableau</label>
                            </li>
                        </ul>
                    </fieldset>
            </div> -->

      <div class="results-list grid">
        <SponsorTile  :partner="{ name: 'Zenika', billingDate: '01/02/2025', pack: 'Gold', status: 'Activé' }"/>
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
