import type { BoothActivityResponseSchema, BoothActivityRequestSchema } from '~/utils/api';

export interface ActivitiesState {
  activities: BoothActivityResponseSchema[];
  loading: boolean;
  error: string | null;
  isModalOpen: boolean;
  editingActivity: BoothActivityResponseSchema | null;
  isSubmitting: boolean;
  deletingId: string | null;
  form: BoothActivityRequestSchema;
}

export type ActivitiesAction =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; payload: BoothActivityResponseSchema[] }
  | { type: 'FETCH_ERROR'; payload: string }
  | { type: 'OPEN_CREATE_MODAL' }
  | { type: 'OPEN_EDIT_MODAL'; payload: BoothActivityResponseSchema }
  | { type: 'CLOSE_MODAL' }
  | { type: 'SET_SUBMITTING'; payload: boolean }
  | { type: 'SET_DELETING_ID'; payload: string | null }
  | { type: 'UPDATE_FORM'; payload: Partial<BoothActivityRequestSchema> };

const emptyForm: BoothActivityRequestSchema = {
  title: '',
  description: '',
  start_time: null,
  end_time: null,
};

export const initialActivitiesState: ActivitiesState = {
  activities: [],
  loading: false,
  error: null,
  isModalOpen: false,
  editingActivity: null,
  isSubmitting: false,
  deletingId: null,
  form: { ...emptyForm },
};

export function activitiesReducer(
  state: ActivitiesState,
  action: ActivitiesAction,
): ActivitiesState {
  switch (action.type) {
    case 'FETCH_START':
      return { ...state, loading: true, error: null };
    case 'FETCH_SUCCESS':
      return { ...state, loading: false, activities: action.payload };
    case 'FETCH_ERROR':
      return { ...state, loading: false, error: action.payload };
    case 'OPEN_CREATE_MODAL':
      return { ...state, isModalOpen: true, editingActivity: null, form: { ...emptyForm } };
    case 'OPEN_EDIT_MODAL':
      return {
        ...state,
        isModalOpen: true,
        editingActivity: action.payload,
        form: {
          title: action.payload.title,
          description: action.payload.description,
          start_time: action.payload.start_time ?? null,
          end_time: action.payload.end_time ?? null,
        },
      };
    case 'CLOSE_MODAL':
      return { ...state, isModalOpen: false, editingActivity: null, form: { ...emptyForm } };
    case 'SET_SUBMITTING':
      return { ...state, isSubmitting: action.payload };
    case 'SET_DELETING_ID':
      return { ...state, deletingId: action.payload };
    case 'UPDATE_FORM':
      return { ...state, form: { ...state.form, ...action.payload } };
    default:
      return state;
  }
}
