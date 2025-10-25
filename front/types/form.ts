/**
 * Types pour les formulaires
 */

export interface ValidationError {
  field: string;
  message: string;
}

export interface FormState<T = any> {
  /** Données du formulaire */
  data: T;
  /** Erreurs de validation */
  errors: ValidationError[];
  /** Formulaire en cours de soumission */
  isSubmitting: boolean;
  /** Formulaire a été modifié */
  isDirty: boolean;
  /** Formulaire est valide */
  isValid: boolean;
  /** Formulaire a été touché */
  isTouched: boolean;
}

export interface FormField<T = any> {
  /** Nom du champ */
  name: keyof T;
  /** Valeur du champ */
  value: any;
  /** Label du champ */
  label?: string;
  /** Placeholder */
  placeholder?: string;
  /** Type du champ */
  type?: 'text' | 'email' | 'password' | 'number' | 'date' | 'url' | 'tel' | 'textarea' | 'select';
  /** Champ requis */
  required?: boolean;
  /** Règles de validation */
  rules?: ValidationRule[];
  /** Champ désactivé */
  disabled?: boolean;
}

export type ValidationRule =
  | 'required'
  | 'email'
  | 'url'
  | 'number'
  | 'date'
  | { min: number }
  | { max: number }
  | { minLength: number }
  | { maxLength: number }
  | { pattern: RegExp }
  | { custom: (value: any) => boolean | string };

export interface UseFormOptions<T> {
  /** Valeurs initiales */
  initialValues: T;
  /** Fonction de validation */
  validate?: (values: T) => ValidationError[] | Promise<ValidationError[]>;
  /** Fonction de soumission */
  onSubmit?: (values: T) => void | Promise<void>;
  /** Auto-save */
  autoSave?: boolean;
  /** Délai d'auto-save (ms) */
  autoSaveDelay?: number;
  /** Validation sur blur */
  validateOnBlur?: boolean;
  /** Validation sur change */
  validateOnChange?: boolean;
}

export interface UseFormReturn<T> {
  /** État du formulaire */
  formState: Ref<FormState<T>>;
  /** Valeurs du formulaire */
  values: Ref<T>;
  /** Erreurs du formulaire */
  errors: Ref<ValidationError[]>;
  /** Définir la valeur d'un champ */
  setValue: (field: keyof T, value: any) => void;
  /** Définir plusieurs valeurs */
  setValues: (values: Partial<T>) => void;
  /** Définir une erreur */
  setError: (field: keyof T, message: string) => void;
  /** Clear une erreur */
  clearError: (field: keyof T) => void;
  /** Clear toutes les erreurs */
  clearErrors: () => void;
  /** Valider le formulaire */
  validate: () => Promise<boolean>;
  /** Soumettre le formulaire */
  submit: () => Promise<void>;
  /** Réinitialiser le formulaire */
  reset: (values?: Partial<T>) => void;
  /** Marquer comme touché */
  touch: () => void;
  /** Obtenir l'erreur d'un champ */
  getError: (field: keyof T) => string | undefined;
  /** Vérifier si un champ a une erreur */
  hasError: (field: keyof T) => boolean;
}

export interface AutoSaveOptions {
  /** Activer l'auto-save */
  enabled: boolean;
  /** Délai avant save (ms) */
  delay: number;
  /** Clé de stockage */
  storageKey: string;
  /** Callback appelé lors du save */
  onSave?: (data: any) => void;
  /** Callback appelé lors de la restauration */
  onRestore?: (data: any) => void;
}
