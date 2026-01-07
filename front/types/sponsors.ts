import { z } from "zod";

/**
 * Filter state for sponsor list filtering
 *
 * Boolean filters can be true, false, or null (no filter applied).
 * - null: no filter (show all)
 * - true: show only items matching the condition
 * - false: show only items NOT matching the condition
 */
export interface FilterState {
  /** Selected pack ID, null = all packs */
  packId: string | null;

  /** Validation status filter
   * - null: show all sponsors
   * - true: show only validated sponsors
   * - false: show only not validated sponsors
   */
  validated: boolean | null;

  /** Payment status filter
   * - null: show all sponsors
   * - true: show only paid sponsors
   * - false: show only unpaid sponsors
   */
  paid: boolean | null;

  /** Agreement generation status filter
   * - null: show all sponsors
   * - true: show only sponsors with agreement generated
   * - false: show only sponsors without agreement generated
   */
  agreementGenerated: boolean | null;

  /** Agreement signature status filter
   * - null: show all sponsors
   * - true: show only sponsors with signed agreement
   * - false: show only sponsors without signed agreement
   */
  agreementSigned: boolean | null;

  /** Suggestion status filter
   * - null: show all sponsors
   * - true: show only suggestions
   * - false: show only confirmed sponsors (not suggestions)
   */
  suggestion: boolean | null;

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
  validated: z.boolean().nullable(),
  paid: z.boolean().nullable(),
  agreementGenerated: z.boolean().nullable(),
  agreementSigned: z.boolean().nullable(),
  suggestion: z.boolean().nullable(),
  organiser: z.string().nullable(),
});

/**
 * Initial filter state with all filters set to null (no filter applied)
 */
export const initialFilterState: FilterState = {
  packId: null,
  validated: null,
  paid: null,
  agreementGenerated: null,
  agreementSigned: null,
  suggestion: null,
  organiser: null,
};
