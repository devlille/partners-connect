export default defineNuxtPlugin((nuxtApp) => {
  const { $i18n } = nuxtApp;

  if (!$i18n) {
    console.error("i18n plugin not found");
    return;
  }

  // French translations
  $i18n.setLocaleMessage("fr", {
    common: {
      save: "Enregistrer",
      cancel: "Annuler",
      delete: "Supprimer",
      edit: "Modifier",
      create: "Créer",
      add: "Ajouter",
      back: "Retour",
      loading: "Chargement...",
      error: "Erreur",
      success: "Succès",
      logout: "Déconnexion",
      confirm: "Confirmer",
      close: "Fermer",
      actions: "Actions",
      name: "Nom",
      price: "Prix",
      yes: "Oui",
      no: "Non",
      all: "Tous",
      clear: "Effacer",
      activeFilters: "Filtres actifs",
    },
    sponsors: {
      title: "Sponsors",
      list: "Liste des sponsors",
      create: "Créer un sponsor",
      companyName: "Nom de l'entreprise",
      pack: "Pack",
      noSponsors: "Aucun sponsor pour le moment",
      suggested: "suggéré",
      filters: {
        pack: "Pack de sponsoring",
        validated: "Validé",
        paid: "Payé",
        agreementGenerated: "Accord généré",
        agreementSigned: "Accord signé",
        suggestion: "Suggestion",
        clearAll: "Effacer tous les filtres",
        showingResults: "Affichage de {count} sponsor(s)",
        noResults: "Aucun sponsor ne correspond aux filtres sélectionnés",
        allPacks: "Tous les packs",
        statusFilters: "Filtres de statut",
        filterPanel: "Filtrer les sponsors",
      },
    },
  });

  // English translations
  $i18n.setLocaleMessage("en", {
    common: {
      save: "Save",
      cancel: "Cancel",
      delete: "Delete",
      edit: "Edit",
      create: "Create",
      back: "Back",
      loading: "Loading...",
      error: "Error",
      success: "Success",
      logout: "Logout",
      clear: "Clear",
      activeFilters: "Active Filters",
      yes: "Yes",
      no: "No",
      all: "All",
    },
    sponsors: {
      title: "Sponsors",
      list: "Sponsors list",
      create: "Create sponsor",
      companyName: "Company name",
      pack: "Pack",
      noSponsors: "No sponsors yet",
      suggested: "suggested",
      filters: {
        pack: "Sponsoring Pack",
        validated: "Validated",
        paid: "Paid",
        agreementGenerated: "Agreement Generated",
        agreementSigned: "Agreement Signed",
        suggestion: "Suggestion",
        clearAll: "Clear All Filters",
        showingResults: "Showing {count} sponsor(s)",
        noResults: "No sponsors match the selected filters",
        allPacks: "All Packs",
        statusFilters: "Status Filters",
        filterPanel: "Filter Sponsors",
      },
    },
  });

  // Spanish translations
  $i18n.setLocaleMessage("es", {
    common: {
      save: "Guardar",
      cancel: "Cancelar",
      delete: "Eliminar",
      edit: "Editar",
      create: "Crear",
      back: "Volver",
      loading: "Cargando...",
      error: "Error",
      success: "Éxito",
      logout: "Cerrar sesión",
      clear: "Limpiar",
      activeFilters: "Filtros Activos",
      yes: "Sí",
      no: "No",
      all: "Todos",
    },
    sponsors: {
      title: "Patrocinadores",
      list: "Lista de patrocinadores",
      create: "Crear patrocinador",
      companyName: "Nombre de la empresa",
      pack: "Paquete",
      noSponsors: "Aún no hay patrocinadores",
      suggested: "sugerido",
      filters: {
        pack: "Paquete de Patrocinio",
        validated: "Validado",
        paid: "Pagado",
        agreementGenerated: "Acuerdo Generado",
        agreementSigned: "Acuerdo Firmado",
        suggestion: "Sugerencia",
        clearAll: "Limpiar Todos los Filtros",
        showingResults: "Mostrando {count} patrocinador(es)",
        noResults: "Ningún patrocinador coincide con los filtros seleccionados",
        allPacks: "Todos los Paquetes",
        statusFilters: "Filtros de Estado",
        filterPanel: "Filtrar Patrocinadores",
      },
    },
  });

  console.log("✓ i18n translations loaded manually via plugin");
  console.log("Current locale:", $i18n.locale.value);
  console.log("Test translation:", $i18n.t("sponsors.filters.validated"));
});
