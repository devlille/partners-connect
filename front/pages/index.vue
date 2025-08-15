<template>
  <header>
    <MainTitle> Demande de Partenariat </MainTitle>
  </header>

  <div id="container">
    <main>
      <form>
        <p>
          <label for="ip-name">Nom</label>
          <input type="text" id="ip-name" name="ip-name" autocomplete="name" />
        </p>
        <p>
          <label for="sel-pack">Pack de sponsoring</label>
          <select id="sel-pack" name="sel-pack">
            <option>Gold</option>
            <option>Silver</option>
            <option>Bronze</option>
            <option>Média</option>
          </select>
        </p>
        <p>
          <label for="ip-email">Email</label>
          <input
            type="emailtext"
            id="ip-email"
            name="ip-email"
            autocomplete="email"
          />
        </p>
        <p>
          <label for="ip-phone">Tél.</label>
          <input type="tel" id="ip-phone" name="ip-phone" autocomplete="tel" />
        </p>

        <p class="buttons-bar">
          <input type="submit" value="Valider" />
        </p>
      </form>
    </main>
  </div>
</template>

<script setup>
//document.body.id = "partners";

definePageMeta({
  layout: "minimal",
});

useHead({
  title: "Demande de Partenariat | DevLille",
  bodyAttrs: {
    id: "partners",
  },
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
});
</script>
