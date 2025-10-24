import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useCustomToast, ToastType } from '~/composables/useCustomToast';

describe('useCustomToast', () => {
  beforeEach(() => {
    const { clearAll } = useCustomToast();
    clearAll();
  });

  describe('addToast', () => {
    it('should add a toast notification', () => {
      const { addToast, toasts } = useCustomToast();

      addToast(ToastType.SUCCESS, 'Test message');

      expect(toasts.value).toHaveLength(1);
      expect(toasts.value[0].type).toBe(ToastType.SUCCESS);
      expect(toasts.value[0].message).toBe('Test message');
    });

    it('should generate unique IDs for toasts', () => {
      const { addToast, toasts } = useCustomToast();

      const id1 = addToast(ToastType.SUCCESS, 'Message 1');
      const id2 = addToast(ToastType.SUCCESS, 'Message 2');

      expect(id1).not.toBe(id2);
      expect(toasts.value).toHaveLength(2);
    });

    it('should auto-remove toast after duration', async () => {
      vi.useFakeTimers();
      const { addToast, toasts } = useCustomToast();

      addToast(ToastType.SUCCESS, 'Test message', 1000);
      expect(toasts.value).toHaveLength(1);

      vi.advanceTimersByTime(1000);
      await vi.runAllTimersAsync();

      expect(toasts.value).toHaveLength(0);
      vi.useRealTimers();
    });
  });

  describe('removeToast', () => {
    it('should remove a specific toast', () => {
      const { addToast, removeToast, toasts } = useCustomToast();

      const id = addToast(ToastType.SUCCESS, 'Test message', 0);
      expect(toasts.value).toHaveLength(1);

      removeToast(id);
      expect(toasts.value).toHaveLength(0);
    });
  });

  describe('success', () => {
    it('should create a success toast', () => {
      const { success, toasts } = useCustomToast();

      success('Success message');

      expect(toasts.value[0].type).toBe(ToastType.SUCCESS);
      expect(toasts.value[0].message).toBe('Success message');
    });
  });

  describe('error', () => {
    it('should create an error toast', () => {
      const { error, toasts } = useCustomToast();

      error('Error message');

      expect(toasts.value[0].type).toBe(ToastType.ERROR);
      expect(toasts.value[0].message).toBe('Error message');
    });
  });

  describe('warning', () => {
    it('should create a warning toast', () => {
      const { warning, toasts } = useCustomToast();

      warning('Warning message');

      expect(toasts.value[0].type).toBe(ToastType.WARNING);
      expect(toasts.value[0].message).toBe('Warning message');
    });
  });

  describe('info', () => {
    it('should create an info toast', () => {
      const { info, toasts } = useCustomToast();

      info('Info message');

      expect(toasts.value[0].type).toBe(ToastType.INFO);
      expect(toasts.value[0].message).toBe('Info message');
    });
  });

  describe('clearAll', () => {
    it('should remove all toasts', () => {
      const { addToast, clearAll, toasts } = useCustomToast();

      addToast(ToastType.SUCCESS, 'Message 1', 0);
      addToast(ToastType.ERROR, 'Message 2', 0);
      addToast(ToastType.WARNING, 'Message 3', 0);

      expect(toasts.value).toHaveLength(3);

      clearAll();
      expect(toasts.value).toHaveLength(0);
    });
  });
});
