<template>
  <div class="login-page">
    <header>
      <MainTitle>Connexion</MainTitle>
    </header>
    
    <div id="container">
      <main>
        <div class="login-content">
          <p>Connectez-vous avec votre compte Google pour accéder à l'espace partenaires.</p>
          
          <button @click="handleLogin" class="google-login-btn">
            Se connecter avec Google
          </button>
          
          <p>
            <NuxtLink to="/">Retour à l'accueil</NuxtLink>
          </p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
const { login } = useAuth()

const handleLogin = () => {
  login()
}

definePageMeta({
  layout: 'default'
})

useHead({
  title: 'Connexion | DevLille'
})
</script>

<style scoped>
.login-content {
  text-align: center;
  max-width: 400px;
  margin: 0 auto;
  padding: 2rem;
}

.google-login-btn {
  background-color: #4285f4;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  margin: 2rem 0;
  transition: background-color 0.3s;
}

.google-login-btn:hover {
  background-color: #3367d6;
}
</style>