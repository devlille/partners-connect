import type { PartnershipItemSchema, PartnershipOptionSelection } from "~/utils/api";

/**
 * Extension du type PartnershipItemSchema pour inclure les champs manquants de l'API
 * Ces champs sont disponibles dans l'API mais ne sont pas encore générés dans le schéma TypeScript
 */
export interface ExtendedPartnershipItem extends PartnershipItemSchema {
  /** Indique si le partenariat a été validé */
  validated?: boolean;
  /** Indique si le partenariat a été payé */
  paid?: boolean;
  /** Indique s'il s'agit d'une suggestion de partenariat */
  suggestion?: boolean;
  /** Indique si l'accord a été généré */
  agreement_generated?: boolean;
  /** Indique si l'accord a été signé */
  agreement_signed?: boolean;
  /** URL de la convention de partenariat */
  agreement_url?: string | null;
  /** URL de la convention signée */
  agreement_signed_url?: string | null;
  /** URL du devis */
  quote_url?: string | null;
  /** URL de la facture */
  invoice_url?: string | null;
  /** IDs des options de sponsoring choisies par le sponsor */
  option_ids?: string[];
  /** Options complètes du pack sélectionné (avec nom et description) */
  pack_options?: Array<{ id: string; name: string; description?: string | null }>;
  /** Détails des sélections d'options avec quantité et valeur sélectionnée */
  option_selections?: PartnershipOptionSelection[];
}
