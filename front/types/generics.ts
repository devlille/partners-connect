export type Nullable<T> = T | null;

export interface EntityState<T> {
  items: T[];
  loading: boolean;
  error: Nullable<string>;
}
