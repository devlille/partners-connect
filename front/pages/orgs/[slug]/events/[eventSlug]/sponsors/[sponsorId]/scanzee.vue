<template>
  <Dashboard :main-links="sponsorLinks" :footer-links="footerLinks">
    <div class="bg-white p-6 border-b border-gray-200">
      <div class="flex items-center justify-between">
        <div>
          <PageTitle>{{ companyName || 'Sponsor' }}</PageTitle>
          <p class="text-sm text-gray-600 mt-1">Scanzee — Questions & Réponses</p>
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
    </div>

    <div class="p-6 space-y-6">
      <TableSkeleton v-if="loading" :columns="2" :rows="4" />

      <div v-else-if="loadError" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
        {{ loadError }}
      </div>

      <template v-else>
        <UCard>
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
              <p v-if="maxQuestions || maxAnswers" class="text-xs text-gray-400">
                <span v-if="maxQuestions">Max {{ maxQuestions }} question(s)</span>
                <span v-if="maxQuestions && maxAnswers"> · </span>
                <span v-if="maxAnswers">Max {{ maxAnswers }} réponse(s) par question</span>
              </p>
            </div>
          </div>
        </UCard>

        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-900">
              Questions
              <span class="text-sm font-normal text-gray-500 ml-1">
                ({{ questions.length }}{{ maxQuestions ? ` / ${maxQuestions}` : '' }})
              </span>
            </h2>
            <UButton
              icon="i-heroicons-plus"
              label="Ajouter une question"
              :disabled="maxQuestions !== null && questions.length >= maxQuestions"
              @click="addQuestion"
            />
          </div>

          <div
            v-if="questions.length === 0"
            class="text-center py-12 text-gray-400"
          >
            <i class="i-heroicons-chat-bubble-left-right text-4xl mb-3 block" aria-hidden="true" />
            Aucune question créée. Cliquez sur "Ajouter une question" pour commencer !
          </div>

          <UCard
            v-for="(q, qi) in questions"
            :key="q._key"
            class="space-y-4"
          >
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
                  :disabled="maxAnswers !== null && q.answers.length >= maxAnswers"
                  @click="addAnswer(qi)"
                />
              </div>

              <div
                v-for="(a, ai) in q.answers"
                :key="ai"
                class="flex items-center gap-2"
              >
                <UCheckbox
                  v-model="a.is_correct"
                  :aria-label="`Marquer la réponse ${ai + 1} comme correcte`"
                />
                <UInput
                  v-model="a.answer"
                  placeholder="Réponse..."
                  class="flex-1"
                />
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
    </div>
  </Dashboard>
</template>

<script setup lang="ts">
import {
  getEventsPartnershipDetailed,
  listPartnershipQandaQuestions,
  createQandaQuestion,
  updateQandaQuestion,
  deleteQandaQuestion,
} from '~/utils/api';
import authMiddleware from '~/middleware/auth';

const route = useRoute();
const { footerLinks } = useDashboardLinks();
const toast = useCustomToast();

definePageMeta({
  middleware: authMiddleware,
  ssr: false,
});

const orgSlug = computed(() => {
  const params = route.params.slug;
  return Array.isArray(params) ? params[0] : (params as string);
});
const eventSlug = computed(() => {
  const params = route.params.eventSlug;
  return Array.isArray(params) ? params[1] : (params as string);
});
const sponsorId = computed(() => {
  const params = route.params.sponsorId;
  return Array.isArray(params) ? params[0] : (params as string);
});

type FormAnswer = { id?: string; answer: string; is_correct: boolean };
type FormQuestion = { _key: string; id?: string; question: string; answers: FormAnswer[] };

let _keyCounter = 0;
const makeKey = () => String(++_keyCounter);

const companyName = ref<string | null>(null);
const loading = ref(true);
const loadError = ref<string | null>(null);
const maxQuestions = ref<number | null>(null);
const maxAnswers = ref<number | null>(null);
const questions = ref<FormQuestion[]>([]);
const saving = ref(new Set<string>());
const deleting = ref(new Set<string>());
const qandaEnabled = ref(true);

const { sponsorLinks } = useSponsorLinks({
  orgSlug: orgSlug.value,
  eventSlug: eventSlug.value,
  sponsorId: sponsorId.value,
  qandaEnabled,
});

async function loadData() {
  try {
    loading.value = true;
    loadError.value = null;

    const [partnershipRes, questionsRes] = await Promise.all([
      getEventsPartnershipDetailed(eventSlug.value, sponsorId.value),
      listPartnershipQandaQuestions(eventSlug.value, sponsorId.value),
    ]);

    companyName.value = partnershipRes.data.company.name;

    const qandaConfig = partnershipRes.data.event.qanda_config;
    maxQuestions.value = qandaConfig?.max_questions ?? null;
    maxAnswers.value = qandaConfig?.max_answers ?? null;

    questions.value = questionsRes.data.map(q => ({
      _key: q.id,
      id: q.id,
      question: q.question,
      answers: q.answers.map(a => ({ id: a.id, answer: a.answer, is_correct: a.is_correct })),
    }));
  } catch {
    loadError.value = 'Impossible de charger les données';
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
      await updateQandaQuestion(eventSlug.value, sponsorId.value, q.id, payload);
    } else {
      const res = await createQandaQuestion(eventSlug.value, sponsorId.value, payload);
      questions.value[qi].id = res.data.id;
      questions.value[qi]._key = res.data.id;
    }

    toast.success('Question sauvegardée');
  } catch {
    toast.error('Impossible de sauvegarder la question');
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
    await deleteQandaQuestion(eventSlug.value, sponsorId.value, q.id);
    questions.value.splice(qi, 1);
    toast.success('Question supprimée');
  } catch {
    toast.error('Impossible de supprimer la question');
  } finally {
    const next = new Set(deleting.value);
    next.delete(q._key);
    deleting.value = next;
  }
}

onMounted(loadData);
watch([eventSlug, sponsorId], loadData);

useHead({
  title: computed(() => `Scanzee — ${companyName.value || 'Sponsor'} | DevLille`),
});
</script>
