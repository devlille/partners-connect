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
        {{ companyName }}
      </h2>

      <div class="tabs-block">
        <ul class="tabs">
          <li>
            <button type="button" @click="activeTab = 'information'" :class="{ active: activeTab === 'information' }">Information</button>
          </li>
          <li>
            <button type="button" @click="activeTab = 'suggestions'" :class="{ active: activeTab === 'suggestions' }">Suggestions</button>
          </li>
          <li>
            <button type="button" @click="activeTab = 'facturation'" :class="{ active: activeTab === 'facturation' }">Facturation</button>
          </li>
          <li role="presentation">
            <button type="button" @click="activeTab = 'billeterie'" :class="{ active: activeTab === 'billeterie' }">Billeterie</button>
          </li>
          <li>
            <button type="button" @click="activeTab = 'stand'" :class="{ active: activeTab === 'stand' }">Stand</button>
          </li>
        </ul>
        
        <!-- Information Tab -->
        <div v-show="activeTab === 'information'" class="tab-content auto-scroll">
          <p>Information content goes here...</p>
        </div>

        <!-- Suggestions Tab -->
        <div v-show="activeTab === 'suggestions'" class="tab-content auto-scroll">
          <SuggestionManager 
            :eventId="eventId" 
            :companyId="companyId"
            @refresh="loadPartnership"
          />
        </div>

        <!-- Facturation Tab -->
        <div v-show="activeTab === 'facturation'" class="tab-content auto-scroll">
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

        <!-- Billeterie Tab -->
        <div v-show="activeTab === 'billeterie'" class="tab-content auto-scroll">
          <p>Billeterie content...</p>
        </div>

        <!-- Stand Tab -->
        <div v-show="activeTab === 'stand'" class="tab-content auto-scroll">
          <p>Stand content...</p>
        </div>
      </div>
    </main>
  </div>

  <footer>
    <p>&copy; DevLille, 2025 -</p>
  </footer>
</template>

<script setup>
// Mock data - in a real app, these would come from route params or authentication
const eventId = ref('550e8400-e29b-41d4-a716-446655440000') // Example UUID
const companyId = ref('660e8400-e29b-41d4-a716-446655440001') // Example UUID
const companyName = ref('Zenika')
const activeTab = ref('suggestions') // Default to suggestions tab

const partnership = ref(null)
const loading = ref(false)
const error = ref(null)

const loadPartnership = async () => {
  loading.value = true
  error.value = null
  
  try {
    const response = await $fetch(`/api/events/${eventId.value}/companies/${companyId.value}/partnership`)
    partnership.value = response
  } catch (err) {
    error.value = err.message || 'Failed to load partnership'
    console.error('Error loading partnership:', err)
  } finally {
    loading.value = false
  }
}

// Load partnership data on component mount
onMounted(() => {
  loadPartnership()
})
</script>

<style scoped>
.tabs button.active {
  background-color: #f0f0f0;
  border-bottom: 2px solid #007bff;
}

.tab-content {
  padding: 20px;
  min-height: 400px;
}
</style>
