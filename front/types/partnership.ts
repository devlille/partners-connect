import type { PartnershipItemSchema } from '~/utils/api';

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
  /** IDs des options de sponsoring choisies par le sponsor */
  option_ids?: string[];
}
