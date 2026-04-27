<template>
  <NuxtLayout
    name="minimal-sidebar"
    :sidebar-title="partnership?.company_name || 'Partenariat'"
    :sidebar-links="sidebarLinks"
  >
    <div class="min-h-screen bg-gray-50">
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8" role="main">
        <header class="bg-white rounded-lg shadow p-6 mb-6">
          <div class="flex items-center justify-between">
            <div>
              <PageTitle>Scanzee — Questions & Réponses</PageTitle>
              <p class="text-sm text-gray-600 mt-1" role="doc-subtitle">
                Gérez vos questions pour les visiteurs de votre stand
              </p>
            </div>
            <UButton
              href="https://www.scanzee.app"
              target="_blank"
              color="neutral"
              variant="outline"
              icon="i-heroicons-arrow-top-right-on-square"
              label="scanzee.app"
            />
          </div>
        </header>

        <TableSkeleton v-if="loading" :columns="2" :rows="4" />

        <div
          v-else-if="error"
          role="alert"
          class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded"
        >
          {{ error }}
        </div>

        <template v-else>
          <UCard class="mb-6">
            <div class="flex items-start gap-4">
              <i class="i-heroicons-qr-code text-4xl text-primary-500 shrink-0" aria-hidden="true" />
              <div class="space-y-2">
                <p class="text-sm font-semibold text-gray-900">Jeu de questions / réponses avec Scanzee</p>
                <p class="text-sm text-gray-600">
                  Cette fonctionnalité est réservée aux sponsors ayant un stand. Créez vos questions pour
                  inviter les visiteurs à scanner votre QR code, jouer et venir sur votre stand pendant l'événement.
                  Les questions seront synchronisées avec
                  <a href="https://www.scanzee.app" target="_blank" rel="noopener" class="text-primary-600 underline">Scanzee</a>.
                </p>
                <p v-if="qandaMaxQuestions || qandaMaxAnswers" class="text-xs text-gray-400">
                  <span v-if="qandaMaxQuestions">Max {{ qandaMaxQuestions }} question(s)</span>
                  <span v-if="qandaMaxQuestions && qandaMaxAnswers"> · </span>
                  <span v-if="qandaMaxAnswers">Max {{ qandaMaxAnswers }} réponse(s) par question</span>
                </p>
              </div>
            </div>
          </UCard>

          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <h2 class="text-lg font-semibold text-gray-900">
                Questions
                <span class="text-sm font-normal text-gray-500 ml-1">
                  ({{ questions.length }}{{ qandaMaxQuestions ? ` / ${qandaMaxQuestions}` : '' }})
                </span>
              </h2>
              <UButton
                icon="i-heroicons-plus"
                label="Ajouter une question"
                :disabled="qandaMaxQuestions !== null && questions.length >= qandaMaxQuestions"
                @click="addQuestion"
              />
            </div>

            <div v-if="questions.length === 0" class="text-center py-12 text-gray-400">
              <i class="i-heroicons-chat-bubble-left-right text-4xl mb-3 block" aria-hidden="true" />
              Aucune question créée. Cliquez sur "Ajouter une question" pour commencer !
            </div>

            <UCard v-for="(q, qi) in questions" :key="q._key" class="space-y-4">
              <div class="flex items-start gap-3">
                <div class="flex-1">
                  <label :for="`question-${q._key}`" class="block text-sm font-medium text-gray-700 mb-1">
                    Question {{ qi + 1 }}
                  </label>
                  <UInput
                    :id="`question-${q._key}`"
                    v-model="q.question"
                    placeholder="Entrez votre question..."
                    class="w-full"
                  />
                </div>
                <UButton
                  color="error"
                  variant="ghost"
                  icon="i-heroicons-trash"
                  class="mt-6"
                  :loading="deleting.has(q._key)"
                  :aria-label="`Supprimer la question ${qi + 1}`"
                  @click="deleteQuestion(qi)"
                />
              </div>

              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <label class="text-sm font-medium text-gray-600">
                    Réponses
                    <span class="text-xs text-gray-400 ml-1">(au moins 2 — cocher la bonne réponse)</span>
                  </label>
                  <UButton
                    size="xs"
                    variant="ghost"
                    icon="i-heroicons-plus"
                    label="Ajouter"
                    :disabled="qandaMaxAnswers !== null && q.answers.length >= qandaMaxAnswers"
                    @click="addAnswer(qi)"
                  />
                </div>

                <div v-for="(a, ai) in q.answers" :key="ai" class="flex items-center gap-2">
                  <UCheckbox
                    v-model="a.is_correct"
                    :aria-label="`Marquer la réponse ${ai + 1} comme correcte`"
                  />
                  <UInput v-model="a.answer" placeholder="Réponse..." class="flex-1" />
                  <UButton
                    v-if="q.answers.length > 2"
                    color="neutral"
                    variant="ghost"
                    icon="i-heroicons-x-mark"
                    size="xs"
                    :aria-label="`Supprimer la réponse ${ai + 1}`"
                    @click="removeAnswer(qi, ai)"
                  />
                </div>
              </div>

              <div class="flex justify-end mt-4">
                <UButton
                  :loading="saving.has(q._key)"
                  :disabled="!q.question.trim() || q.answers.length < 2 || q.answers.some(a => !a.answer.trim())"
                  icon="i-heroicons-check"
                  label="Sauvegarder"
                  @click="saveQuestion(qi)"
                />
              </div>
            </UCard>
          </div>
        </template>
      </main>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import {
  listPartnershipQandaQuestions,
  createQandaQuestion,
  updateQandaQuestion,
  deleteQandaQuestion,
} from '~/utils/api';

definePageMeta({
  auth: false,
  ssr: false,
  validate: async (route) => {
    const eventSlug = Array.isArray(route.params.eventSlug) ? route.params.eventSlug[0] : route.params.eventSlug;
    const partnershipId = Array.isArray(route.params.partnershipId) ? route.params.partnershipId[0] : route.params.partnershipId;
    const isValidFormat = /^[a-zA-Z0-9-_]+$/;
    return isValidFormat.test(eventSlug) && isValidFormat.test(partnershipId);
  },
});

const toast = useToast();

const {
  eventSlug,
  partnershipId,
  partnership,
  qandaMaxQuestions,
  qandaMaxAnswers,
  loadPartnership,
} = usePublicPartnership();

const { sidebarLinks } = usePublicPartnershipLinks();

type FormAnswer = { id?: string; answer: string; is_correct: boolean };
type FormQuestion = { _key: string; id?: string; question: string; answers: FormAnswer[] };

let _keyCounter = 0;
const makeKey = () => String(++_keyCounter);

const loading = ref(true);
const error = ref<string | null>(null);
const questions = ref<FormQuestion[]>([]);
const saving = ref(new Set<string>());
const deleting = ref(new Set<string>());

async function loadQuestions() {
  try {
    loading.value = true;
    error.value = null;
    const res = await listPartnershipQandaQuestions(eventSlug.value, partnershipId.value);
    questions.value = res.data.map(q => ({
      _key: q.id,
      id: q.id,
      question: q.question,
      answers: q.answers.map(a => ({ id: a.id, answer: a.answer, is_correct: a.is_correct })),
    }));
  } catch {
    error.value = 'Impossible de charger les questions';
  } finally {
    loading.value = false;
  }
}

function addQuestion() {
  questions.value.push({
    _key: makeKey(),
    question: '',
    answers: [
      { answer: '', is_correct: false },
      { answer: '', is_correct: false },
    ],
  });
}

function addAnswer(qi: number) {
  questions.value[qi].answers.push({ answer: '', is_correct: false });
}

function removeAnswer(qi: number, ai: number) {
  questions.value[qi].answers.splice(ai, 1);
}

async function saveQuestion(qi: number) {
  const q = questions.value[qi];
  saving.value = new Set([...saving.value, q._key]);

  try {
    const payload = {
      question: q.question.trim(),
      answers: q.answers.map(a => ({ answer: a.answer.trim(), is_correct: a.is_correct })),
    };

    if (q.id) {
      await updateQandaQuestion(eventSlug.value, partnershipId.value, q.id, payload);
    } else {
      const res = await createQandaQuestion(eventSlug.value, partnershipId.value, payload);
      questions.value[qi].id = res.data.id;
      questions.value[qi]._key = res.data.id;
    }

    toast.add({ title: 'Question sauvegardée', color: 'success' });
  } catch (err: any) {
    const description = err?.response?.data?.message ?? undefined;
    toast.add({ title: 'Impossible de sauvegarder la question', description, color: 'error' });
  } finally {
    const next = new Set(saving.value);
    next.delete(q._key);
    saving.value = next;
  }
}

async function deleteQuestion(qi: number) {
  const q = questions.value[qi];

  if (!q.id) {
    questions.value.splice(qi, 1);
    return;
  }

  deleting.value = new Set([...deleting.value, q._key]);

  try {
    await deleteQandaQuestion(eventSlug.value, partnershipId.value, q.id);
    questions.value.splice(qi, 1);
    toast.add({ title: 'Question supprimée', color: 'success' });
  } catch {
    toast.add({ title: 'Impossible de supprimer la question', color: 'error' });
  } finally {
    const next = new Set(deleting.value);
    next.delete(q._key);
    deleting.value = next;
  }
}

onMounted(async () => {
  await loadPartnership();
  await loadQuestions();
});

watch([eventSlug, partnershipId], async () => {
  await loadPartnership();
  await loadQuestions();
});

useHead({
  title: computed(() => `Scanzee - ${partnership.value?.company_name || 'Partenariat'} | DevLille`),
});
</script>
