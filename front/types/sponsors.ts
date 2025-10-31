import { z } from 'zod'

/**
 * Filter state for sponsor list filtering
 *
 * All fields are nullable to support "no filter" state.
 * For boolean fields, null means "show both" (not the same as false).
 */
export interface FilterState {
  /** Selected pack ID, null = all packs */
  packId: string | null

  /** Validation status filter
   * - null: show both validated and not validated
   * - true: show only validated sponsors
   * - false: show only not validated sponsors
   */
  validated: boolean | null

  /** Payment status filter
   * - null: show both paid and unpaid
   * - true: show only paid sponsors
   * - false: show only unpaid sponsors
   */
  paid: boolean | null

  /** Agreement generation status filter
   * - null: show both generated and not generated
   * - true: show only sponsors with agreement generated
   * - false: show only sponsors without agreement generated
   */
  agreementGenerated: boolean | null

  /** Agreement signature status filter
   * - null: show both signed and not signed
   * - true: show only sponsors with signed agreement
   * - false: show only sponsors without signed agreement
   */
  agreementSigned: boolean | null

  /** Suggestion status filter
   * - null: show both suggestions and confirmed sponsors
   * - true: show only suggestions
   * - false: show only confirmed sponsors (not suggestions)
   */
  suggestion: boolean | null
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
  suggestion: z.boolean().nullable()
})

/**
 * Initial filter state with all filters disabled
 */
export const initialFilterState: FilterState = {
  packId: null,
  validated: null,
  paid: null,
  agreementGenerated: null,
  agreementSigned: null,
  suggestion: null
}
