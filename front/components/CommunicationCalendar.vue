<template>
  <div class="bg-white rounded-lg border border-gray-200 p-6">
    <!-- Header du calendrier -->
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-lg font-semibold text-gray-900">
        {{ currentMonthLabel }}
      </h2>
      <div class="flex gap-2">
        <UButton
          icon="i-heroicons-chevron-left"
          variant="ghost"
          color="neutral"
          size="sm"
          @click="previousMonth"
        />
        <UButton
          variant="ghost"
          color="neutral"
          size="sm"
          @click="goToToday"
        >
          Aujourd'hui
        </UButton>
        <UButton
          icon="i-heroicons-chevron-right"
          variant="ghost"
          color="neutral"
          size="sm"
          @click="nextMonth"
        />
      </div>
    </div>

    <!-- Grille du calendrier -->
    <div class="grid grid-cols-7 gap-px bg-gray-200 border border-gray-200 rounded-lg overflow-hidden">
      <!-- En-têtes des jours -->
      <div
        v-for="day in weekDays"
        :key="day"
        class="bg-gray-50 p-2 text-center text-xs font-semibold text-gray-700"
      >
        {{ day }}
      </div>

      <!-- Cellules du calendrier -->
      <div
        v-for="(day, index) in calendarDays"
        :key="index"
        :class="[
          'bg-white p-2 min-h-24',
          day.isCurrentMonth ? 'text-gray-900' : 'text-gray-400 bg-gray-50',
          day.isToday ? 'ring-2 ring-primary-500' : ''
        ]"
      >
        <div class="text-sm font-medium mb-1">
          {{ day.date }}
        </div>

        <!-- Communications pour ce jour -->
        <div class="space-y-1">
          <div
            v-for="comm in day.communications"
            :key="comm.partnership_id"
            :class="[
              'text-xs p-1 rounded cursor-pointer truncate',
              getStatusClass(comm)
            ]"
            :title="comm.company_name"
            @click="$emit('selectCommunication', comm)"
          >
            {{ comm.company_name }}
          </div>
        </div>
      </div>
    </div>

    <!-- Légende -->
    <div class="mt-4 flex items-center gap-4 text-xs">
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-blue-100 border border-blue-300" />
        <span class="text-gray-600">Planifiée</span>
      </div>
      <div class="flex items-center gap-2">
        <div class="w-4 h-4 rounded bg-green-100 border border-green-300" />
        <span class="text-gray-600">Terminée</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CommunicationItemSchema } from '~/utils/api';

interface Props {
  communications: CommunicationItemSchema[];
}

const props = defineProps<Props>();

defineEmits<{
  selectCommunication: [comm: CommunicationItemSchema];
}>();

interface CalendarDay {
  date: number;
  fullDate: Date;
  isCurrentMonth: boolean;
  isToday: boolean;
  communications: CommunicationItemSchema[];
}

const currentDate = ref(new Date());

const weekDays = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

const currentMonthLabel = computed(() => {
  return new Intl.DateTimeFormat('fr-FR', {
    month: 'long',
    year: 'numeric'
  }).format(currentDate.value);
});

const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear();
  const month = currentDate.value.getMonth();

  // Premier jour du mois
  const firstDay = new Date(year, month, 1);
  // Dernier jour du mois
  const lastDay = new Date(year, month + 1, 0);

  // Jour de la semaine du premier jour (0 = dimanche, on veut 0 = lundi)
  let firstDayOfWeek = firstDay.getDay() - 1;
  if (firstDayOfWeek < 0) firstDayOfWeek = 6;

  // Calculer le nombre de jours du mois précédent à afficher
  const prevMonthLastDay = new Date(year, month, 0).getDate();
  const daysFromPrevMonth = firstDayOfWeek;

  // Créer le tableau des jours
  const days: CalendarDay[] = [];

  // Jours du mois précédent
  for (let i = daysFromPrevMonth - 1; i >= 0; i--) {
    const date = prevMonthLastDay - i;
    const fullDate = new Date(year, month - 1, date);
    days.push({
      date,
      fullDate,
      isCurrentMonth: false,
      isToday: false,
      communications: getCommunicationsForDate(fullDate)
    });
  }

  // Jours du mois actuel
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  for (let i = 1; i <= lastDay.getDate(); i++) {
    const fullDate = new Date(year, month, i);
    const isToday = fullDate.getTime() === today.getTime();

    days.push({
      date: i,
      fullDate,
      isCurrentMonth: true,
      isToday,
      communications: getCommunicationsForDate(fullDate)
    });
  }

  // Jours du mois suivant pour compléter la grille
  const remainingDays = 42 - days.length; // 6 semaines * 7 jours
  for (let i = 1; i <= remainingDays; i++) {
    const fullDate = new Date(year, month + 1, i);
    days.push({
      date: i,
      fullDate,
      isCurrentMonth: false,
      isToday: false,
      communications: getCommunicationsForDate(fullDate)
    });
  }

  return days;
});

function getCommunicationsForDate(date: Date): CommunicationItemSchema[] {
  const dateString = date.toISOString().split('T')[0];
  return props.communications.filter(comm => {
    if (!comm.publication_date) return false;
    const commDate = comm.publication_date.split('T')[0];
    return commDate === dateString;
  });
}

function getStatusClass(comm: CommunicationItemSchema): string {
  // Déterminer le statut en fonction de la date
  if (!comm.publication_date) return 'bg-gray-100 text-gray-800 border border-gray-300';

  const pubDate = new Date(comm.publication_date);
  pubDate.setHours(0, 0, 0, 0);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (pubDate < today) {
    // Terminée
    return 'bg-green-100 text-green-800 border border-green-300 hover:bg-green-200';
  } else {
    // Planifiée
    return 'bg-blue-100 text-blue-800 border border-blue-300 hover:bg-blue-200';
  }
}

function previousMonth() {
  currentDate.value = new Date(
    currentDate.value.getFullYear(),
    currentDate.value.getMonth() - 1,
    1
  );
}

function nextMonth() {
  currentDate.value = new Date(
    currentDate.value.getFullYear(),
    currentDate.value.getMonth() + 1,
    1
  );
}

function goToToday() {
  currentDate.value = new Date();
}
</script>
