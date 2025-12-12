import { VALIDATION_MESSAGES } from "~/constants/validation";

/**
 * Composable pour gérer la validation des formulaires d'intégration
 * Centralise la logique de validation répétée dans tous les formulaires d'intégration
 */
export const useIntegrationFormValidation = <T extends Record<string, any>>(
  props: { modelValue: T },
  emit: {
    (event: "update:modelValue", value: T): void;
    (event: "update:valid", value: boolean): void;
  },
  requiredFields: (keyof T)[],
  customValidators?: Partial<Record<keyof T, (value: any) => string | null>>,
) => {
  const localValue = reactive<T>({ ...props.modelValue });
  const errors = reactive<Partial<Record<keyof T, string>>>({});

  /**
   * Valide un champ spécifique
   */
  function validateField(field: keyof T, showError = true): boolean {
    const value =
      typeof localValue[field] === "string" ? localValue[field]?.trim() : localValue[field];

    // Vérifier si le champ est requis
    if (requiredFields.includes(field)) {
      if (!value || (typeof value === "string" && value === "")) {
        if (showError) errors[field] = VALIDATION_MESSAGES.REQUIRED;
        return false;
      }
    }

    // Appliquer les validateurs personnalisés
    if (customValidators?.[field] && value) {
      const customError = customValidators[field]!(value);
      if (customError) {
        if (showError) errors[field] = customError;
        return false;
      }
    }

    if (showError) errors[field] = "";
    return true;
  }

  /**
   * Valide tous les champs
   */
  function validateAll(showErrors = false): boolean {
    let allValid = true;

    for (const field of requiredFields) {
      const isValid = validateField(field, showErrors);
      if (!isValid) allValid = false;
    }

    return allValid;
  }

  /**
   * Gère les changements d'input
   */
  function handleInput() {
    emit("update:modelValue", { ...localValue } as T);
    emit("update:valid", validateAll());
  }

  /**
   * Réinitialise les erreurs
   */
  function clearErrors() {
    Object.keys(errors).forEach((key) => {
      errors[key as keyof T] = "";
    });
  }

  // Watch pour les changements externes
  watch(
    () => props.modelValue,
    (newVal) => {
      Object.assign(localValue, newVal);
    },
    { deep: true },
  );

  // Validation initiale
  watchEffect(() => {
    emit("update:valid", validateAll());
  });

  return {
    localValue,
    errors,
    validateField,
    validateAll,
    handleInput,
    clearErrors,
  };
};
