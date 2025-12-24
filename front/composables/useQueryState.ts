import { ref, computed, watch, type Ref, type ComputedRef } from "vue";
import { useRoute, useRouter } from "vue-router";

/**
 * Parser interface for type-safe URL query parameter parsing
 * Inspired by nuqs (https://nuqs.47ng.com/)
 */
export interface Parser<T> {
  /** Parse a string value from the URL into the target type */
  parse: (value: string) => T;
  /** Serialize a value to a string for the URL */
  serialize: (value: T) => string;
  /** Default value when the parameter is not present */
  defaultValue: T;
}

/**
 * Built-in parsers for common types
 */
export const parsers = {
  /**
   * String parser - returns the value as-is
   */
  string: (defaultValue: string = ""): Parser<string> => ({
    parse: (value) => value,
    serialize: (value) => value,
    defaultValue,
  }),

  /**
   * Nullable string parser
   */
  stringOrNull: (): Parser<string | null> => ({
    parse: (value) => value,
    serialize: (value) => value ?? "",
    defaultValue: null,
  }),

  /**
   * Boolean parser - parses "true"/"false" strings
   */
  boolean: (defaultValue: boolean = false): Parser<boolean> => ({
    parse: (value) => value === "true",
    serialize: (value) => String(value),
    defaultValue,
  }),

  /**
   * Nullable boolean parser - for tri-state filters (true/false/null)
   */
  booleanOrNull: (): Parser<boolean | null> => ({
    parse: (value) => (value === "true" ? true : value === "false" ? false : null),
    serialize: (value) => (value === null ? "" : String(value)),
    defaultValue: null,
  }),

  /**
   * Number parser
   */
  number: (defaultValue: number = 0): Parser<number> => ({
    parse: (value) => {
      const parsed = Number(value);
      return Number.isNaN(parsed) ? defaultValue : parsed;
    },
    serialize: (value) => String(value),
    defaultValue,
  }),

  /**
   * Nullable number parser
   */
  numberOrNull: (): Parser<number | null> => ({
    parse: (value) => {
      const parsed = Number(value);
      return Number.isNaN(parsed) ? null : parsed;
    },
    serialize: (value) => (value === null ? "" : String(value)),
    defaultValue: null,
  }),

  /**
   * Enum parser - restricts values to a set of allowed strings
   */
  enum: <T extends string>(values: readonly T[], defaultValue: T): Parser<T> => ({
    parse: (value) => (values.includes(value as T) ? (value as T) : defaultValue),
    serialize: (value) => value,
    defaultValue,
  }),

  /**
   * Array parser - handles comma-separated values
   */
  array: <T>(itemParser: Parser<T>): Parser<T[]> => ({
    parse: (value) =>
      value
        .split(",")
        .filter(Boolean)
        .map((item) => itemParser.parse(item)),
    serialize: (values) => values.map((v) => itemParser.serialize(v)).join(","),
    defaultValue: [],
  }),

  /**
   * JSON parser - for complex objects
   */
  json: <T>(defaultValue: T): Parser<T> => ({
    parse: (value) => {
      try {
        return JSON.parse(value) as T;
      } catch {
        return defaultValue;
      }
    },
    serialize: (value) => JSON.stringify(value),
    defaultValue,
  }),
};

/**
 * Options for useQueryState
 */
export interface UseQueryStateOptions<T> {
  /** The query parameter key in the URL */
  key: string;
  /** Parser to convert between string and target type */
  parser: Parser<T>;
  /** Whether to use history.replaceState (default) or pushState */
  history?: "replace" | "push";
  /** Throttle URL updates in milliseconds (default: 50) */
  throttleMs?: number;
  /** Storage key for persistence (optional) */
  storageKey?: string;
}

/**
 * Return type for useQueryState
 */
export interface UseQueryStateReturn<T> {
  /** Reactive state value */
  state: Ref<T>;
  /** Reset to default value */
  reset: () => void;
  /** Check if value differs from default */
  isModified: ComputedRef<boolean>;
}

/**
 * Type-safe URL query state management composable
 * Inspired by nuqs for React
 *
 * @example
 * ```typescript
 * const { state: page } = useQueryState({
 *   key: 'page',
 *   parser: parsers.number(1)
 * })
 *
 * const { state: search } = useQueryState({
 *   key: 'q',
 *   parser: parsers.string('')
 * })
 *
 * const { state: validated } = useQueryState({
 *   key: 'validated',
 *   parser: parsers.booleanOrNull()
 * })
 * ```
 */
export function useQueryState<T>(options: UseQueryStateOptions<T>): UseQueryStateReturn<T> {
  const { key, parser, history = "replace", throttleMs = 50, storageKey } = options;

  const route = useRoute();
  const router = useRouter();

  // Initialize from URL, storage, or default
  function getInitialValue(): T {
    if (typeof window === "undefined") return parser.defaultValue;

    // Check URL first
    const urlValue = route.query[key];
    if (urlValue !== undefined && urlValue !== null) {
      const stringValue = Array.isArray(urlValue) ? urlValue[0] : urlValue;
      if (stringValue) {
        return parser.parse(stringValue);
      }
    }

    // Check storage if configured
    if (storageKey) {
      const stored = sessionStorage.getItem(storageKey);
      if (stored) {
        try {
          return parser.parse(stored);
        } catch {
          // Ignore parse errors
        }
      }
    }

    return parser.defaultValue;
  }

  const state = ref<T>(getInitialValue()) as Ref<T>;

  // Throttle timer
  let throttleTimer: ReturnType<typeof setTimeout> | null = null;

  // Sync state to URL
  watch(
    state,
    (newValue) => {
      if (typeof window === "undefined") return;

      // Clear existing timer
      if (throttleTimer) {
        clearTimeout(throttleTimer);
      }

      throttleTimer = setTimeout(() => {
        const serialized = parser.serialize(newValue);
        const currentQuery = { ...route.query };

        // Remove key if value is empty or equals default
        if (
          serialized === "" ||
          serialized === parser.serialize(parser.defaultValue)
        ) {
          delete currentQuery[key];
        } else {
          currentQuery[key] = serialized;
        }

        // Update URL
        if (history === "replace") {
          router.replace({ query: currentQuery });
        } else {
          router.push({ query: currentQuery });
        }

        // Update storage if configured
        if (storageKey) {
          if (serialized === "" || serialized === parser.serialize(parser.defaultValue)) {
            sessionStorage.removeItem(storageKey);
          } else {
            sessionStorage.setItem(storageKey, serialized);
          }
        }
      }, throttleMs);
    },
    { deep: true },
  );

  // Watch for external URL changes (back/forward navigation)
  watch(
    () => route.query[key],
    (newValue) => {
      if (typeof window === "undefined") return;

      const stringValue = Array.isArray(newValue) ? newValue[0] : newValue;
      const parsedValue = stringValue ? parser.parse(stringValue) : parser.defaultValue;

      // Only update if different (avoid loops)
      if (JSON.stringify(parsedValue) !== JSON.stringify(state.value)) {
        state.value = parsedValue;
      }
    },
  );

  const isModified = computed(() => {
    return JSON.stringify(state.value) !== JSON.stringify(parser.defaultValue);
  });

  function reset() {
    state.value = parser.defaultValue;
  }

  return {
    state,
    reset,
    isModified,
  };
}

/**
 * Schema definition for useQueryStates
 */
export type QueryStateSchema = Record<string, Parser<unknown>>;

/**
 * Infer the state type from a schema
 */
export type InferQueryState<T extends QueryStateSchema> = {
  [K in keyof T]: T[K] extends Parser<infer U> ? U : never;
};

/**
 * Options for useQueryStates
 */
export interface UseQueryStatesOptions<T extends QueryStateSchema> {
  /** Schema defining all query parameters */
  schema: T;
  /** Whether to use history.replaceState (default) or pushState */
  history?: "replace" | "push";
  /** Throttle URL updates in milliseconds (default: 50) */
  throttleMs?: number;
  /** Storage key prefix for persistence (optional) */
  storageKeyPrefix?: string;
}

/**
 * Return type for useQueryStates
 */
export interface UseQueryStatesReturn<T extends QueryStateSchema> {
  /** Reactive state object */
  state: Ref<InferQueryState<T>>;
  /** Reset all values to defaults */
  resetAll: () => void;
  /** Reset a specific key to its default */
  reset: (key: keyof T) => void;
  /** Number of modified (non-default) values */
  modifiedCount: ComputedRef<number>;
  /** Check if any value is modified */
  isModified: ComputedRef<boolean>;
}

/**
 * Manage multiple query parameters with a single composable
 * Inspired by nuqs useQueryStates
 *
 * @example
 * ```typescript
 * const { state, resetAll } = useQueryStates({
 *   schema: {
 *     page: parsers.number(1),
 *     search: parsers.string(''),
 *     validated: parsers.booleanOrNull(),
 *     packId: parsers.stringOrNull(),
 *   }
 * })
 *
 * // Access values
 * console.log(state.value.page) // number
 * console.log(state.value.search) // string
 *
 * // Update values
 * state.value.page = 2
 * state.value.validated = true
 *
 * // Reset all
 * resetAll()
 * ```
 */
export function useQueryStates<T extends QueryStateSchema>(
  options: UseQueryStatesOptions<T>,
): UseQueryStatesReturn<T> {
  const { schema, history = "replace", throttleMs = 50, storageKeyPrefix } = options;

  const route = useRoute();
  const router = useRouter();

  // Initialize state from URL, storage, or defaults
  function getInitialState(): InferQueryState<T> {
    const result = {} as InferQueryState<T>;

    for (const [key, parser] of Object.entries(schema)) {
      if (typeof window === "undefined") {
        (result as Record<string, unknown>)[key] = parser.defaultValue;
        continue;
      }

      // Check URL first
      const urlValue = route.query[key];
      if (urlValue !== undefined && urlValue !== null) {
        const stringValue = Array.isArray(urlValue) ? urlValue[0] : urlValue;
        if (stringValue) {
          (result as Record<string, unknown>)[key] = parser.parse(stringValue);
          continue;
        }
      }

      // Check storage if configured
      if (storageKeyPrefix) {
        const stored = sessionStorage.getItem(`${storageKeyPrefix}:${key}`);
        if (stored) {
          try {
            (result as Record<string, unknown>)[key] = parser.parse(stored);
            continue;
          } catch {
            // Ignore parse errors
          }
        }
      }

      (result as Record<string, unknown>)[key] = parser.defaultValue;
    }

    return result;
  }

  const state = ref<InferQueryState<T>>(getInitialState()) as Ref<InferQueryState<T>>;

  // Throttle timer
  let throttleTimer: ReturnType<typeof setTimeout> | null = null;

  // Sync state to URL
  watch(
    state,
    (newState) => {
      if (typeof window === "undefined") return;

      // Clear existing timer
      if (throttleTimer) {
        clearTimeout(throttleTimer);
      }

      throttleTimer = setTimeout(() => {
        const currentQuery = { ...route.query };

        for (const [key, parser] of Object.entries(schema)) {
          const value = (newState as Record<string, unknown>)[key];
          const serialized = parser.serialize(value);

          // Remove key if value is empty or equals default
          if (serialized === "" || serialized === parser.serialize(parser.defaultValue)) {
            delete currentQuery[key];
          } else {
            currentQuery[key] = serialized;
          }

          // Update storage if configured
          if (storageKeyPrefix) {
            const storageKey = `${storageKeyPrefix}:${key}`;
            if (serialized === "" || serialized === parser.serialize(parser.defaultValue)) {
              sessionStorage.removeItem(storageKey);
            } else {
              sessionStorage.setItem(storageKey, serialized);
            }
          }
        }

        // Update URL
        if (history === "replace") {
          router.replace({ query: currentQuery });
        } else {
          router.push({ query: currentQuery });
        }
      }, throttleMs);
    },
    { deep: true },
  );

  // Watch for external URL changes (back/forward navigation)
  watch(
    () => route.query,
    (newQuery) => {
      if (typeof window === "undefined") return;

      const newState = { ...state.value } as Record<string, unknown>;
      let hasChanges = false;

      for (const [key, parser] of Object.entries(schema)) {
        const urlValue = newQuery[key];
        const stringValue = Array.isArray(urlValue) ? urlValue[0] : urlValue;
        const parsedValue = stringValue ? parser.parse(stringValue) : parser.defaultValue;

        if (JSON.stringify(parsedValue) !== JSON.stringify(newState[key])) {
          newState[key] = parsedValue;
          hasChanges = true;
        }
      }

      if (hasChanges) {
        state.value = newState as InferQueryState<T>;
      }
    },
    { deep: true },
  );

  const modifiedCount = computed(() => {
    let count = 0;
    for (const [key, parser] of Object.entries(schema)) {
      const value = (state.value as Record<string, unknown>)[key];
      if (JSON.stringify(value) !== JSON.stringify(parser.defaultValue)) {
        count++;
      }
    }
    return count;
  });

  const isModified = computed(() => modifiedCount.value > 0);

  function resetAll() {
    const defaults = {} as InferQueryState<T>;
    for (const [key, parser] of Object.entries(schema)) {
      (defaults as Record<string, unknown>)[key] = parser.defaultValue;
    }
    state.value = defaults;
  }

  function reset(key: keyof T) {
    const parser = schema[key];
    (state.value as Record<string, unknown>)[key as string] = parser.defaultValue;
  }

  return {
    state,
    resetAll,
    reset,
    modifiedCount,
    isModified,
  };
}
