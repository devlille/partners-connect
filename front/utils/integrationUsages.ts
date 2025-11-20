import type { IntegrationSchemaUsage } from './api';

/**
 * Usage category display metadata
 */
export interface UsageMetadata {
  /** Usage type identifier */
  type: IntegrationSchemaUsage;

  /** Human-readable usage name (French) */
  name: string;

  /** Description (French) */
  description: string;
}

/**
 * Usage category metadata registry
 * Maps each integration usage type to its display metadata
 */
export const USAGES: Record<IntegrationSchemaUsage, UsageMetadata> = {
  billing: {
    type: 'billing',
    name: 'Facturation',
    description: 'Gestion des paiements et facturation'
  },
  notification: {
    type: 'notification',
    name: 'Notifications',
    description: 'Envoi de notifications par email ou messagerie'
  },
  ticketing: {
    type: 'ticketing',
    name: 'Billetterie',
    description: 'Gestion des billets et inscriptions'
  },
  agenda: {
    type: 'agenda',
    name: 'Agenda',
    description: 'Gestion du programme et des sessions'
  },
  webhook: {
    type: 'webhook',
    name: 'Webhook',
    description: 'Intégration personnalisée via webhook'
  }
};

/**
 * Get all available usage types
 */
export const ALL_USAGES = Object.keys(USAGES) as IntegrationSchemaUsage[];
