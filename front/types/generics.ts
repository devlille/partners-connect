/**
 * Generic utility types for reducing code duplication
 * Following DRY principles with Pick, Omit, Partial, and custom utilities
 */

/**
 * Nullable type utility - eliminates 44+ duplicate nullable type definitions
 * Usage: Nullable<string> instead of string | null
 */
export type Nullable<T> = T | null;

/**
 * Generic entity state for stores
 * Eliminates duplication in sponsors.ts, packs.ts, and other stores
 *
 * @template T - The entity type (Partnership, SponsoringPack, etc.)
 *
 * @example
 * ```typescript
 * interface SponsorsState extends EntityState<Partnership> {}
 * ```
 */
export interface EntityState<T> {
  items: T[];
  loading: boolean;
  error: Nullable<string>;
}

/**
 * Page loading state - common pattern across 20+ page components
 * Eliminates repeated ref definitions in page setup
 *
 * @example
 * ```typescript
 * const state = reactive<PageLoadState>({
 *   loading: true,
 *   error: null,
 *   success: false
 * });
 * ```
 */
export interface PageLoadState {
  loading: boolean;
  error: Nullable<string>;
  success: boolean;
}

/**
 * Extended page load state with data
 * Generic wrapper for pages that load specific data
 *
 * @template T - The data type being loaded
 */
export interface PageDataState<T> extends PageLoadState {
  data: Nullable<T>;
}

/**
 * Form state generic for managing form data with validation
 *
 * @template T - The form data type
 */
export interface FormState<T> {
  data: T;
  errors: Partial<Record<keyof T, string>>;
  touched: Partial<Record<keyof T, boolean>>;
  isDirty: boolean;
  isSubmitting: boolean;
}

/**
 * Async operation state - for tracking async operations (API calls, file uploads, etc.)
 */
export interface AsyncOperationState {
  pending: boolean;
  error: Nullable<Error>;
  completed: boolean;
}

/**
 * Pagination state - common pattern for paginated lists
 */
export interface PaginationState {
  page: number;
  pageSize: number;
  total: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Paginated entity state - combines EntityState with pagination
 *
 * @template T - The entity type
 */
export interface PaginatedEntityState<T> extends EntityState<T> {
  pagination: PaginationState;
}

/**
 * Base input props - shared across all form input components
 * Use with Pick, Omit for specific input types
 *
 * @example
 * ```typescript
 * // In TextInput.vue
 * defineProps<Pick<BaseInputProps, 'id' | 'label' | 'name'> & {
 *   type?: string;
 * }>();
 * ```
 */
export interface BaseInputProps {
  id: string;
  label: string;
  name: string;
  modelValue?: string | number;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  readonly?: boolean;
  autocomplete?: string;
}

/**
 * Base modal props - shared across modal components
 * Extract common properties to avoid duplication
 */
export interface BaseModalProps {
  isOpen: boolean;
  title: string;
  loading?: boolean;
}

/**
 * API response wrapper - standard structure for API responses
 *
 * @template T - The response data type
 */
export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

/**
 * API error response
 */
export interface ApiErrorResponse {
  message: string;
  errors?: Record<string, string[]>;
  status: number;
}

/**
 * Selection state - for managing selected items in lists/tables
 *
 * @template T - The item type (must have an id field)
 */
export interface SelectionState<T extends { id: string }> {
  selected: T[];
  isAllSelected: boolean;
}

/**
 * Utility type to make specific keys required
 * Opposite of Partial
 *
 * @example
 * ```typescript
 * type User = { id?: string; name?: string; email?: string };
 * type UserWithId = RequireKeys<User, 'id'>; // { id: string; name?: string; email?: string }
 * ```
 */
export type RequireKeys<T, K extends keyof T> = T & Required<Pick<T, K>>;

/**
 * Utility type to make specific keys optional
 * More precise than Partial
 *
 * @example
 * ```typescript
 * type User = { id: string; name: string; email: string };
 * type UserWithOptionalEmail = OptionalKeys<User, 'email'>; // { id: string; name: string; email?: string }
 * ```
 */
export type OptionalKeys<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;

/**
 * Deep partial - makes all nested properties optional
 */
export type DeepPartial<T> = {
  [P in keyof T]?: T[P] extends object ? DeepPartial<T[P]> : T[P];
};

/**
 * Deep required - makes all nested properties required
 */
export type DeepRequired<T> = {
  [P in keyof T]-?: T[P] extends object ? DeepRequired<T[P]> : T[P];
};

/**
 * Extract keys from T that have values of type V
 *
 * @example
 * ```typescript
 * type User = { id: string; age: number; name: string };
 * type StringKeys = KeysOfType<User, string>; // 'id' | 'name'
 * ```
 */
export type KeysOfType<T, V> = {
  [K in keyof T]: T[K] extends V ? K : never;
}[keyof T];

/**
 * Readonly deep - makes all nested properties readonly
 */
export type DeepReadonly<T> = {
  readonly [P in keyof T]: T[P] extends object ? DeepReadonly<T[P]> : T[P];
};
