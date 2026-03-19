import type { CommunicationItemSchema, CommunicationPlanRequestSchema } from "~/utils/api";

export const useStandaloneCommunicationModal = () => {
  const toast = useToast();

  const isOpen = ref(false);
  const editingEntry = ref<CommunicationItemSchema | null>(null);
  const form = ref<
    Pick<CommunicationPlanRequestSchema, "title" | "scheduled_date" | "support_url">
  >({
    title: "",
    scheduled_date: null,
    support_url: null,
  });
  const submitting = ref(false);
  const deleting = ref(false);

  const isEditing = computed(() => editingEntry.value !== null);

  function openCreate() {
    editingEntry.value = null;
    form.value = { title: "", scheduled_date: null, support_url: null };
    isOpen.value = true;
  }

  function openEdit(entry: CommunicationItemSchema) {
    editingEntry.value = entry;
    form.value = {
      title: entry.title,
      scheduled_date: entry.publication_date ? entry.publication_date.split("T")[0] : null,
      support_url: entry.support_url ?? null,
    };
    isOpen.value = true;
  }

  function close() {
    isOpen.value = false;
  }

  async function handleSubmit(orgSlug: string, eventSlug: string, onSuccess: () => Promise<void>) {
    const { postOrgsEventsCommunicationPlan, putOrgsEventsCommunicationPlanById } =
      await import("~/utils/api");

    try {
      submitting.value = true;

      const payload: CommunicationPlanRequestSchema = {
        title: form.value.title,
        scheduled_date: form.value.scheduled_date ? `${form.value.scheduled_date}T00:00:00` : null,
        support_url: form.value.support_url,
      };

      if (isEditing.value && editingEntry.value) {
        await putOrgsEventsCommunicationPlanById(
          orgSlug,
          eventSlug,
          editingEntry.value.id,
          payload,
        );
      } else {
        await postOrgsEventsCommunicationPlan(orgSlug, eventSlug, payload);
      }

      toast.add({
        title: "Succès",
        description: isEditing.value ? "Communication modifiée" : "Communication créée",
        color: "green",
      });

      isOpen.value = false;
      await onSuccess();
    } catch (err) {
      console.error("Failed to save communication:", err);
      toast.add({
        title: "Erreur",
        description: isEditing.value
          ? "Impossible de modifier la communication"
          : "Impossible de créer la communication",
        color: "error",
      });
    } finally {
      submitting.value = false;
    }
  }

  async function handleDelete(orgSlug: string, eventSlug: string, onSuccess: () => Promise<void>) {
    if (!editingEntry.value) return;

    const { deleteOrgsEventsCommunicationPlanById } = await import("~/utils/api");

    try {
      deleting.value = true;
      await deleteOrgsEventsCommunicationPlanById(orgSlug, eventSlug, editingEntry.value.id);

      toast.add({
        title: "Succès",
        description: "Communication supprimée",
        color: "green",
      });

      isOpen.value = false;
      await onSuccess();
    } catch (err) {
      console.error("Failed to delete communication:", err);
      toast.add({
        title: "Erreur",
        description: "Impossible de supprimer la communication",
        color: "error",
      });
    } finally {
      deleting.value = false;
    }
  }

  return {
    isOpen,
    editingEntry,
    form,
    submitting,
    deleting,
    isEditing,
    openCreate,
    openEdit,
    close,
    handleSubmit,
    handleDelete,
  };
};
