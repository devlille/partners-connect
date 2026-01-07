import { z } from "zod";

/**
 * Filter state for sponsor list filtering
 *
 * Boolean filters are always true or false (never null).
 * They are always sent to the API and default to true (checked).
 */
export interface FilterState {
  /** Selected pack ID, null = all packs */
  packId: string | null;

  /** Validation status filter
   * - true: show only validated sponsors
   * - false: show only not validated sponsors
   */
  validated: boolean;

  /** Payment status filter
   * - true: show only paid sponsors
   * - false: show only unpaid sponsors
   */
  paid: boolean;

  /** Agreement generation status filter
   * - true: show only sponsors with agreement generated
   * - false: show only sponsors without agreement generated
   */
  agreementGenerated: boolean;

  /** Agreement signature status filter
   * - true: show only sponsors with signed agreement
   * - false: show only sponsors without signed agreement
   */
  agreementSigned: boolean;

  /** Suggestion status filter
   * - true: show only suggestions
   * - false: show only confirmed sponsors (not suggestions)
   */
  suggestion: boolean;

  /** Organiser email filter, null = all organisers */
  organiser: string | null;
}

/**
 * Filter value option for string filters with predefined values
 */
export interface FilterValueOption {
  value: string;
  display_value: string;
}

/**
 * Filter metadata from API
 */
export interface FilterMetadata {
  name: string;
  type: "boolean" | "string";
  values?: FilterValueOption[];
}

/**
 * Metadata returned by the API for partnerships list
 */
export interface PartnershipsMetadata {
  filters: FilterMetadata[];
  sorts: string[];
}

/**
 * Zod validation schema for FilterState
 * Used for validating data restored from sessionStorage
 */
export const FilterStateSchema = z.object({
  packId: z.string().nullable(),
  validated: z.boolean(),
  paid: z.boolean(),
  agreementGenerated: z.boolean(),
  agreementSigned: z.boolean(),
  suggestion: z.boolean(),
  organiser: z.string().nullable(),
});

/**
 * Initial filter state with all boolean filters disabled (unchecked)
 */
export const initialFilterState: FilterState = {
  packId: null,
  validated: false,
  paid: false,
  agreementGenerated: false,
  agreementSigned: false,
  suggestion: false,
  organiser: null,
};
