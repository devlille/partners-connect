/**
 * Composable for form validation with Zod schema integration
 * Provides type-safe validation with automatic TypeScript inference
 */

import { ref, computed, type Ref } from 'vue';
import { z, type ZodSchema } from 'zod';

/**
 * Validation result interface
 */
export interface ValidationResult<T> {
  success: boolean;
  data?: T;
  errors?: Partial<Record<keyof T, string>>;
}

/**
 * Form validation composable options
 */
export interface UseFormValidationOptions {
  /**
   * Validate on change (debounced)
   */
  validateOnChange?: boolean;
  /**
   * Debounce delay in ms for validateOnChange
   */
  debounceMs?: number;
  /**
   * Transform Zod error messages to i18n keys
   */
  transformErrorMessage?: (message: string) => string;
}

/**
 * Form validation composable with Zod schema integration
 *
 * @template T - Zod schema type
 * @param schema - Zod validation schema
 * @param options - Validation options
 *
 * @example
 * ```typescript
 * const schema = z.object({
 *   email: z.string().email(),
 *   name: z.string().min(2)
 * });
 *
 * const { errors, validate, validateField, hasError, getError, clearErrors } =
 *   useFormValidation(schema);
 *
 * const result = await validate({ email: 'test@example.com', name: 'John' });
 * if (result.success) {
 *   // result.data is typed as { email: string; name: string }
 * }
 * ```
 */
export function useFormValidation<T extends ZodSchema>(
  schema: T,
  options: UseFormValidationOptions = {}
) {
  type SchemaType = z.infer<T>;

  const errors = ref<Partial<Record<keyof SchemaType, string>>>({}) as Ref<
    Partial<Record<keyof SchemaType, string>>
  >;

  const touched = ref<Partial<Record<keyof SchemaType, boolean>>>({}) as Ref<
    Partial<Record<keyof SchemaType, boolean>>
  >;

  const isValidating = ref(false);

  /**
   * Check if form has any errors
   */
  const hasErrors = computed(() => Object.keys(errors.value).length > 0);

  /**
   * Check if a specific field has an error
   */
  const hasError = (field: keyof SchemaType): boolean => {
    return !!errors.value[field];
  };

  /**
   * Get error message for a specific field
   */
  const getError = (field: keyof SchemaType): string | undefined => {
    return errors.value[field];
  };

  /**
   * Check if a field has been touched
   */
  const isTouched = (field: keyof SchemaType): boolean => {
    return !!touched.value[field];
  };

  /**
   * Mark a field as touched
   */
  const touch = (field: keyof SchemaType) => {
    touched.value[field] = true;
  };

  /**
   * Mark all fields as touched
   */
  const touchAll = (data: Record<string, any>) => {
    Object.keys(data).forEach((key) => {
      touched.value[key as keyof SchemaType] = true;
    });
  };

  /**
   * Clear all validation errors
   */
  const clearErrors = () => {
    errors.value = {};
  };

  /**
   * Clear error for a specific field
   */
  const clearError = (field: keyof SchemaType) => {
    delete errors.value[field];
  };

  /**
   * Transform Zod error to field errors
   */
  const transformZodError = (zodError: z.ZodError): Partial<Record<keyof SchemaType, string>> => {
    const fieldErrors: Partial<Record<keyof SchemaType, string>> = {};

    zodError.issues.forEach((error: z.ZodIssue) => {
      const field = error.path[0] as keyof SchemaType;
      if (field) {
        const message = options.transformErrorMessage
          ? options.transformErrorMessage(error.message)
          : error.message;
        fieldErrors[field] = message;
      }
    });

    return fieldErrors;
  };

  /**
   * Validate the entire form data
   */
  const validate = async (data: unknown): Promise<ValidationResult<SchemaType>> => {
    isValidating.value = true;

    try {
      const result = await schema.parseAsync(data);
      errors.value = {};
      isValidating.value = false;

      return {
        success: true,
        data: result,
      };
    } catch (error) {
      if (error instanceof z.ZodError) {
        errors.value = transformZodError(error);
      } else {
        console.error('Unexpected validation error:', error);
      }

      isValidating.value = false;

      return {
        success: false,
        errors: errors.value,
      };
    }
  };

  /**
   * Validate a single field
   */
  const validateField = async (
    field: keyof SchemaType,
    value: unknown
  ): Promise<boolean> => {
    try {
      // Extract the schema for this specific field
      const schemaObject = schema as any;
      const fieldSchema = schemaObject.shape?.[field as string];

      if (fieldSchema) {
        await fieldSchema.parseAsync(value);
        clearError(field);
        return true;
      }

      return false;
    } catch (error) {
      if (error instanceof z.ZodError) {
        const message = error.issues[0]?.message || 'Validation error';
        const transformedMessage = options.transformErrorMessage
          ? options.transformErrorMessage(message)
          : message;
        errors.value[field] = transformedMessage;
      }
      return false;
    }
  };

  /**
   * Safe parse - validate without throwing errors
   */
  const safeParse = (data: unknown): ValidationResult<SchemaType> => {
    const result = schema.safeParse(data);

    if (result.success) {
      errors.value = {};
      return {
        success: true,
        data: result.data,
      };
    }

    errors.value = transformZodError(result.error);

    return {
      success: false,
      errors: errors.value,
    };
  };

  /**
   * Reset validation state
   */
  const reset = () => {
    errors.value = {};
    touched.value = {};
    isValidating.value = false;
  };

  return {
    // State
    errors,
    touched,
    isValidating,
    hasErrors,

    // Methods
    validate,
    validateField,
    safeParse,
    hasError,
    getError,
    isTouched,
    touch,
    touchAll,
    clearErrors,
    clearError,
    reset,
  };
}

/**
 * Reusable validator for non-empty records
 * Common pattern in the codebase for translations and other keyed objects
 *
 * @example
 * ```typescript
 * const schema = z.object({
 *   translations: nonEmptyRecord(translationSchema)
 * });
 * ```
 */
export function nonEmptyRecord<T extends z.ZodTypeAny>(schema: T) {
  return z.record(z.string(), schema).refine(
    (record) => Object.keys(record).length > 0,
    { message: 'errors.validation.emptyRecord' }
  );
}

/**
 * Create a mutually exclusive validator
 * Ensures that only one of the specified fields is set
 *
 * @example
 * ```typescript
 * const schema = z.object({
 *   price: z.number().optional(),
 *   is_free: z.boolean().optional(),
 * }).refine(
 *   mutuallyExclusive('price', 'is_free'),
 *   { message: 'Either price or is_free, not both' }
 * );
 * ```
 */
export function mutuallyExclusive<T extends Record<string, any>>(
  field1: keyof T,
  field2: keyof T
) {
  return (data: T) => {
    const hasField1 = data[field1] !== undefined && data[field1] !== null;
    const hasField2 = data[field2] !== undefined && data[field2] !== null;
    return !(hasField1 && hasField2);
  };
}

/**
 * Create a conditional required validator
 * Makes a field required when a condition is met
 *
 * @example
 * ```typescript
 * const schema = z.object({
 *   type: z.enum(['paid', 'free']),
 *   price: z.number().optional(),
 * }).refine(
 *   conditionalRequired('price', (data) => data.type === 'paid'),
 *   { message: 'Price is required for paid items' }
 * );
 * ```
 */
export function conditionalRequired<T extends Record<string, any>>(
  field: keyof T,
  condition: (data: T) => boolean
) {
  return (data: T) => {
    if (condition(data)) {
      return data[field] !== undefined && data[field] !== null && data[field] !== '';
    }
    return true;
  };
}
