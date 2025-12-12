/**
 * Labels ARIA et textes d'accessibilité
 * Centralise tous les labels pour garantir la cohérence et faciliter l'i18n
 */

export const ARIA_LABELS = {
  // Navigation
  NAV_MAIN: "Navigation principale",
  NAV_SECONDARY: "Navigation secondaire",
  NAV_BREADCRUMB: "Fil d'Ariane",
  NAV_FOOTER: "Navigation du pied de page",

  // Landmarks
  MAIN_CONTENT: "Contenu principal",
  SIDEBAR: "Menu latéral",
  SEARCH: "Recherche",
  COMPLEMENTARY: "Informations complémentaires",

  // Buttons & Controls
  CLOSE: "Fermer",
  CANCEL: "Annuler",
  CONFIRM: "Confirmer",
  SAVE: "Enregistrer",
  DELETE: "Supprimer",
  EDIT: "Modifier",
  ADD: "Ajouter",
  REMOVE: "Retirer",
  SEARCH_BUTTON: "Rechercher",
  FILTER: "Filtrer",
  SORT: "Trier",
  REFRESH: "Actualiser",
  SETTINGS: "Paramètres",
  MENU: "Menu",
  TOGGLE_MENU: "Ouvrir/Fermer le menu",
  TOGGLE_THEME: "Basculer le thème clair/sombre",
  BACK: "Retour",
  NEXT: "Suivant",
  PREVIOUS: "Précédent",
  SUBMIT: "Soumettre",

  // Tables
  TABLE: "Tableau de données",
  SORT_ASCENDING: (column: string) => `Trier ${column} par ordre croissant`,
  SORT_DESCENDING: (column: string) => `Trier ${column} par ordre décroissant`,
  ROW_SELECTED: "Ligne sélectionnée",
  SELECT_ROW: "Sélectionner cette ligne",
  SELECT_ALL_ROWS: "Sélectionner toutes les lignes",

  // Forms
  REQUIRED_FIELD: "Champ obligatoire",
  OPTIONAL_FIELD: "Champ optionnel",
  FORM_ERROR: "Erreur de formulaire",
  FORM_SUCCESS: "Formulaire envoyé avec succès",
  FIELD_ERROR: (field: string) => `Erreur dans le champ ${field}`,
  PASSWORD_SHOW: "Afficher le mot de passe",
  PASSWORD_HIDE: "Masquer le mot de passe",

  // Modals
  MODAL: "Boîte de dialogue",
  MODAL_CLOSE: "Fermer la boîte de dialogue",
  DIALOG_TITLE: (title: string) => `Dialogue: ${title}`,

  // Status & Notifications
  LOADING: "Chargement en cours",
  LOADING_MORE: "Chargement de plus d'éléments",
  SUCCESS_MESSAGE: "Message de succès",
  ERROR_MESSAGE: "Message d'erreur",
  WARNING_MESSAGE: "Message d'avertissement",
  INFO_MESSAGE: "Message d'information",
  NOTIFICATION_REGION: "Zone de notifications",

  // Pagination
  PAGINATION: "Navigation par pages",
  CURRENT_PAGE: (page: number) => `Page ${page}`,
  GO_TO_PAGE: (page: number) => `Aller à la page ${page}`,
  NEXT_PAGE: "Page suivante",
  PREVIOUS_PAGE: "Page précédente",
  FIRST_PAGE: "Première page",
  LAST_PAGE: "Dernière page",

  // File Upload
  FILE_UPLOAD: "Téléverser un fichier",
  FILE_REMOVE: "Retirer le fichier",
  FILE_SELECTED: (name: string) => `Fichier sélectionné: ${name}`,

  // Dates
  DATE_PICKER: "Sélecteur de date",
  TIME_PICKER: "Sélecteur d'heure",
  CALENDAR: "Calendrier",

  // Actions
  VIEW_DETAILS: (item: string) => `Voir les détails de ${item}`,
  EDIT_ITEM: (item: string) => `Modifier ${item}`,
  DELETE_ITEM: (item: string) => `Supprimer ${item}`,
  DUPLICATE_ITEM: (item: string) => `Dupliquer ${item}`,
  EXPORT: "Exporter les données",
  IMPORT: "Importer des données",

  // Resources
  SPONSORS_LIST: "Liste des sponsors",
  PACKS_LIST: "Liste des packs",
  OPTIONS_LIST: "Liste des options",
  EVENTS_LIST: "Liste des événements",
  PROVIDERS_LIST: "Liste des prestataires",
  COMPANIES_LIST: "Liste des entreprises",
  JOB_OFFERS_LIST: "Liste des offres d'emploi",

  // Empty States
  NO_RESULTS: "Aucun résultat trouvé",
  EMPTY_LIST: "La liste est vide",

  // Keyboard Shortcuts
  KEYBOARD_SHORTCUTS: "Raccourcis clavier",
  SHORTCUT_HELP: "Aide sur les raccourcis clavier",
} as const;

/**
 * Live regions pour les annonces dynamiques
 */
export const LIVE_REGIONS = {
  POLITE: "polite" as const,
  ASSERTIVE: "assertive" as const,
  OFF: "off" as const,
};

/**
 * ARIA roles
 */
export const ARIA_ROLES = {
  ALERT: "alert",
  ALERTDIALOG: "alertdialog",
  BUTTON: "button",
  CHECKBOX: "checkbox",
  DIALOG: "dialog",
  LISTBOX: "listbox",
  MENU: "menu",
  MENUITEM: "menuitem",
  NAVIGATION: "navigation",
  PROGRESSBAR: "progressbar",
  RADIO: "radio",
  RADIOGROUP: "radiogroup",
  SEARCH: "search",
  STATUS: "status",
  TAB: "tab",
  TABLIST: "tablist",
  TABPANEL: "tabpanel",
  TEXTBOX: "textbox",
  TOOLTIP: "tooltip",
} as const;

/**
 * States ARIA
 */
export const ARIA_STATES = {
  EXPANDED: "aria-expanded",
  SELECTED: "aria-selected",
  CHECKED: "aria-checked",
  DISABLED: "aria-disabled",
  HIDDEN: "aria-hidden",
  INVALID: "aria-invalid",
  REQUIRED: "aria-required",
  BUSY: "aria-busy",
  CURRENT: "aria-current",
  PRESSED: "aria-pressed",
} as const;
