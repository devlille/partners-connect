import { describe, it, expect, vi, beforeEach } from "vitest";
import { useStandaloneCommunicationModal } from "~/composables/useStandaloneCommunicationModal";

vi.mock("~/utils/api", () => ({
  postOrgsEventsCommunicationPlan: vi.fn().mockResolvedValue({}),
  putOrgsEventsCommunicationPlanById: vi.fn().mockResolvedValue({}),
  deleteOrgsEventsCommunicationPlanById: vi.fn().mockResolvedValue({}),
}));

vi.mock("#imports", async (importOriginal) => {
  const actual = await importOriginal<typeof import("#imports")>();
  return {
    ...actual,
    useToast: () => ({ add: vi.fn() }),
  };
});

const standaloneEntry = {
  id: "entry-1",
  partnership_id: null,
  title: "Post LinkedIn DevLille 2026",
  publication_date: "2026-06-15T00:00:00",
  support_url: null,
};

describe("useStandaloneCommunicationModal", () => {
  describe("initial state", () => {
    it("starts closed with no editing entry", () => {
      const modal = useStandaloneCommunicationModal();
      expect(modal.isOpen.value).toBe(false);
      expect(modal.editingEntry.value).toBeNull();
      expect(modal.isEditing.value).toBe(false);
    });
  });

  describe("openCreate", () => {
    it("opens the modal and resets form", () => {
      const modal = useStandaloneCommunicationModal();
      modal.openCreate();
      expect(modal.isOpen.value).toBe(true);
      expect(modal.editingEntry.value).toBeNull();
      expect(modal.isEditing.value).toBe(false);
      expect(modal.form.value.title).toBe("");
    });
  });

  describe("openEdit", () => {
    it("opens the modal with entry data pre-filled", () => {
      const modal = useStandaloneCommunicationModal();
      modal.openEdit(standaloneEntry);
      expect(modal.isOpen.value).toBe(true);
      expect(modal.editingEntry.value).toEqual(standaloneEntry);
      expect(modal.isEditing.value).toBe(true);
      expect(modal.form.value.title).toBe("Post LinkedIn DevLille 2026");
      expect(modal.form.value.scheduled_date).toBe("2026-06-15");
    });

    it("sets scheduled_date to null when entry has no publication_date", () => {
      const modal = useStandaloneCommunicationModal();
      modal.openEdit({ ...standaloneEntry, publication_date: null });
      expect(modal.form.value.scheduled_date).toBeNull();
    });
  });

  describe("close", () => {
    it("closes the modal", () => {
      const modal = useStandaloneCommunicationModal();
      modal.openCreate();
      modal.close();
      expect(modal.isOpen.value).toBe(false);
    });
  });

  describe("handleSubmit", () => {
    beforeEach(() => {
      vi.clearAllMocks();
    });

    it("calls postOrgsEventsCommunicationPlan when creating", async () => {
      const { postOrgsEventsCommunicationPlan } = await import("~/utils/api");
      const modal = useStandaloneCommunicationModal();
      const onSuccess = vi.fn().mockResolvedValue(undefined);

      modal.openCreate();
      modal.form.value.title = "Nouveau post";

      await modal.handleSubmit("devlille", "devlille-2026", onSuccess);

      expect(postOrgsEventsCommunicationPlan).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        expect.objectContaining({ title: "Nouveau post", scheduled_date: null }),
      );
      expect(onSuccess).toHaveBeenCalled();
    });

    it("calls putOrgsEventsCommunicationPlanById when editing", async () => {
      const { putOrgsEventsCommunicationPlanById } = await import("~/utils/api");
      const modal = useStandaloneCommunicationModal();
      const onSuccess = vi.fn().mockResolvedValue(undefined);

      modal.openEdit(standaloneEntry);

      await modal.handleSubmit("devlille", "devlille-2026", onSuccess);

      expect(putOrgsEventsCommunicationPlanById).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        "entry-1",
        expect.objectContaining({ title: "Post LinkedIn DevLille 2026" }),
      );
      expect(onSuccess).toHaveBeenCalled();
    });

    it("formats scheduled_date with time suffix", async () => {
      const { postOrgsEventsCommunicationPlan } = await import("~/utils/api");
      const modal = useStandaloneCommunicationModal();
      const onSuccess = vi.fn().mockResolvedValue(undefined);

      modal.openCreate();
      modal.form.value.title = "Test";
      modal.form.value.scheduled_date = "2026-09-01";

      await modal.handleSubmit("devlille", "devlille-2026", onSuccess);

      expect(postOrgsEventsCommunicationPlan).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        expect.objectContaining({ scheduled_date: "2026-09-01T00:00:00" }),
      );
    });
  });

  describe("handleDelete", () => {
    beforeEach(() => {
      vi.clearAllMocks();
    });

    it("calls deleteOrgsEventsCommunicationPlanById", async () => {
      const { deleteOrgsEventsCommunicationPlanById } = await import("~/utils/api");
      const modal = useStandaloneCommunicationModal();
      const onSuccess = vi.fn().mockResolvedValue(undefined);

      modal.openEdit(standaloneEntry);
      await modal.handleDelete("devlille", "devlille-2026", onSuccess);

      expect(deleteOrgsEventsCommunicationPlanById).toHaveBeenCalledWith(
        "devlille",
        "devlille-2026",
        "entry-1",
      );
      expect(onSuccess).toHaveBeenCalled();
    });

    it("does nothing when no editing entry", async () => {
      const { deleteOrgsEventsCommunicationPlanById } = await import("~/utils/api");
      const modal = useStandaloneCommunicationModal();
      const onSuccess = vi.fn().mockResolvedValue(undefined);

      await modal.handleDelete("devlille", "devlille-2026", onSuccess);

      expect(deleteOrgsEventsCommunicationPlanById).not.toHaveBeenCalled();
      expect(onSuccess).not.toHaveBeenCalled();
    });
  });
});
