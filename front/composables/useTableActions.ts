import { h, resolveComponent } from "vue";

/**
 * Composable pour créer une colonne "Actions" standardisée dans les tableaux
 */
export const useTableActions = () => {
  /**
   * Crée une colonne "Actions" avec un bouton de navigation
   * @param accessorKey - La clé d'accès pour la colonne (ex: 'slug', 'id')
   * @param getUrl - Fonction qui retourne l'URL de navigation à partir de la ligne
   * @param title - Titre du bouton (tooltip)
   */
  function createActionColumn(
    accessorKey: string,
    getUrl: (row: any) => string,
    title: string = "Voir",
  ) {
    return {
      header: "Actions",
      accessorKey,
      cell: (info: any) => {
        const row = info.row.original;
        return h(resolveComponent("UButton"), {
          onClick: () => {
            navigateTo(getUrl(row));
          },
          icon: "i-heroicons-arrow-right-circle",
          size: "md",
          color: "primary",
          variant: "ghost",
          square: true,
          title,
        });
      },
    };
  }

  /**
   * Crée un bouton d'action personnalisé
   * @param options - Options du bouton
   */
  function createActionButton(options: {
    onClick: () => void;
    icon?: string;
    color?: string;
    title?: string;
  }) {
    return h(resolveComponent("UButton"), {
      onClick: options.onClick,
      icon: options.icon || "i-heroicons-arrow-right-circle",
      size: "md",
      color: options.color || "primary",
      variant: "ghost",
      square: true,
      title: options.title || "",
    });
  }

  return {
    createActionColumn,
    createActionButton,
  };
};
