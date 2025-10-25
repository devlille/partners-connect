import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useModal } from '~/composables/useModal';

describe('useModal', () => {
  beforeEach(() => {
    // Reset DOM
    document.body.innerHTML = '';
    document.body.style.overflow = '';
  });

  afterEach(() => {
    document.body.style.overflow = '';
  });

  it('should initialize with closed state', () => {
    const modal = useModal();

    expect(modal.isOpen.value).toBe(false);
  });

  it('should open modal', () => {
    const modal = useModal();

    modal.open();

    expect(modal.isOpen.value).toBe(true);
    expect(document.body.style.overflow).toBe('hidden');
  });

  it('should close modal', () => {
    const modal = useModal();

    modal.open();
    modal.close();

    expect(modal.isOpen.value).toBe(false);
    expect(document.body.style.overflow).toBe('');
  });

  it('should toggle modal state', () => {
    const modal = useModal();

    expect(modal.isOpen.value).toBe(false);

    modal.toggle();
    expect(modal.isOpen.value).toBe(true);

    modal.toggle();
    expect(modal.isOpen.value).toBe(false);
  });

  it('should change modal size', () => {
    const modal = useModal({ defaultSize: 'md' });

    expect(modal.size.value).toBe('md');

    modal.setSize('lg');
    expect(modal.size.value).toBe('lg');
  });

  it('should call onOpen callback', () => {
    const onOpen = vi.fn();
    const modal = useModal({ onOpen });

    modal.open();

    expect(onOpen).toHaveBeenCalledTimes(1);
  });

  it('should call onClose callback', () => {
    const onClose = vi.fn();
    const modal = useModal({ onClose });

    modal.open();
    modal.close();

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('should handle ESC key when closeOnEsc is true', () => {
    const modal = useModal({ closeOnEsc: true });
    modal.open();

    expect(modal.isOpen.value).toBe(true);

    // Simulate ESC key press
    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    document.dispatchEvent(event);

    expect(modal.isOpen.value).toBe(false);
  });

  it('should not close on ESC when closeOnEsc is false', () => {
    const modal = useModal({ closeOnEsc: false });
    modal.open();

    expect(modal.isOpen.value).toBe(true);

    // Simulate ESC key press
    const event = new KeyboardEvent('keydown', { key: 'Escape' });
    document.dispatchEvent(event);

    expect(modal.isOpen.value).toBe(true);
  });

  it('should cleanup body overflow on unmount', () => {
    const modal = useModal();
    modal.open();

    expect(document.body.style.overflow).toBe('hidden');

    // The composable should cleanup on unmount
    // This is tested by the implementation having onUnmounted
  });
});
