/**
 * Discriminated union types for Partnership entities
 * Provides type-safe handling of partnership states with automatic type narrowing
 */

import type { Nullable } from './generics';

/**
 * Partnership status types
 */
export type PartnershipStatus = 'draft' | 'pending' | 'validated' | 'signed' | 'rejected';

/**
 * Base partnership properties shared across all states
 */
interface BasePartnership {
  id: string;
  company_id: string;
  company_name: string;
  pack_id: string;
  pack_name: Nullable<string>;
  contact_name: string;
  contact_role: string;
  language: string;
  created_at: string;
  updated_at: string;
}

/**
 * Draft partnership - initial creation state
 * No agreement yet, not yet submitted for validation
 */
export interface DraftPartnership extends BasePartnership {
  status: 'draft';
  agreement_url: null;
  agreement_signed: false;
  signed_date: null;
  validated_at: null;
}

/**
 * Pending partnership - awaiting validation
 * Agreement may have been generated but not signed yet
 */
export interface PendingPartnership extends BasePartnership {
  status: 'pending';
  agreement_url: Nullable<string>;
  agreement_signed: false;
  signed_date: null;
  validated_at: null;
}

/**
 * Validated partnership - approved but not signed
 * Agreement is available for signature
 */
export interface ValidatedPartnership extends BasePartnership {
  status: 'validated';
  agreement_url: string; // Must have agreement URL
  agreement_signed: false;
  signed_date: null;
  validated_at: string; // Timestamp of validation
}

/**
 * Signed partnership - fully executed
 * Agreement has been signed by the partner
 */
export interface SignedPartnership extends BasePartnership {
  status: 'signed';
  agreement_url: string; // Must have agreement URL
  agreement_signed: true;
  signed_date: string; // Must have signature date
  validated_at: string; // Must have been validated first
}

/**
 * Rejected partnership - validation was denied
 */
export interface RejectedPartnership extends BasePartnership {
  status: 'rejected';
  agreement_url: Nullable<string>;
  agreement_signed: false;
  signed_date: null;
  validated_at: null;
  rejection_reason?: string;
  rejected_at: string;
}

/**
 * Discriminated union of all partnership states
 * TypeScript will automatically narrow the type based on the status property
 *
 * @example
 * ```typescript
 * function handlePartnership(partnership: Partnership) {
 *   if (partnership.status === 'signed') {
 *     // TypeScript knows this is SignedPartnership
 *     console.log(partnership.signed_date); // ✓ Available
 *     console.log(partnership.agreement_url); // ✓ Available (not nullable)
 *   } else if (partnership.status === 'draft') {
 *     // TypeScript knows this is DraftPartnership
 *     console.log(partnership.agreement_url); // null
 *   }
 * }
 * ```
 */
export type Partnership =
  | DraftPartnership
  | PendingPartnership
  | ValidatedPartnership
  | SignedPartnership
  | RejectedPartnership;

/**
 * Type guard to check if partnership is signed
 */
export function isSignedPartnership(partnership: Partnership): partnership is SignedPartnership {
  return partnership.status === 'signed';
}

/**
 * Type guard to check if partnership has agreement
 */
export function hasAgreement(
  partnership: Partnership
): partnership is ValidatedPartnership | SignedPartnership {
  return partnership.status === 'validated' || partnership.status === 'signed';
}

/**
 * Type guard to check if partnership is actionable (can be modified)
 */
export function isActionablePartnership(
  partnership: Partnership
): partnership is DraftPartnership | PendingPartnership {
  return partnership.status === 'draft' || partnership.status === 'pending';
}

/**
 * Partnership input for creation
 */
export interface CreatePartnershipInput {
  company_id: string;
  pack_id: string;
  option_ids?: string[];
  contact_name: string;
  contact_role: string;
  language: string;
  phone?: string;
  emails?: string[];
}

/**
 * Partnership input for update
 */
export interface UpdatePartnershipInput {
  contact_name?: string;
  contact_role?: string;
  language?: string;
  phone?: string;
  emails?: string[];
  option_ids?: string[];
}

/**
 * Partnership statistics
 */
export interface PartnershipStats {
  total: number;
  draft: number;
  pending: number;
  validated: number;
  signed: number;
  rejected: number;
}
