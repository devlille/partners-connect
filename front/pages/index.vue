<template>
  <header>
    <MainTitle> Demande de Partenariat </MainTitle>
  </header>

  <div id="container">
    <main>
      <form>
        <p>
          <label for="ip-name">Nom</label>
          <input id="ip-name" type="text" name="ip-name" autocomplete="name" />
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
            id="ip-email"
            type="emailtext"
            name="ip-email"
            autocomplete="email"
          />
        </p>
        <p>
          <label for="ip-phone">Tél.</label>
          <input id="ip-phone" type="tel" name="ip-phone" autocomplete="tel" />
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
  }
});
</script>
