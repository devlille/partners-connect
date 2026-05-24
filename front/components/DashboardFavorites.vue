<template>
  <div v-if="show && top5.length > 0" class="px-4 pb-4">
    <div class="border-t border-gray-200 my-4" />
    <h3 class="px-3 mb-2 text-xs font-semibold uppercase tracking-wide text-gray-500">
      Mes favoris
    </h3>
    <nav class="space-y-1">
      <NuxtLink
        v-for="event in top5"
        :key="event.slug"
        :to="`/orgs/${event.org_slug}/events/${event.slug}`"
        class="flex flex-col px-3 py-2 text-sm text-gray-700 rounded-md hover:bg-gray-100 transition-colors"
      >
        <span class="font-medium truncate">{{ event.name }}</span>
        <span class="text-xs text-gray-500">{{ formatDate(event.start_time) }}</span>
      </NuxtLink>
    </nav>
  </div>
</template>

<script setup lang="ts">
const route = useRoute();
const { top5 } = useFavoriteEvents();
const { formatDate } = useDateFormatter();

// Show on /orgs and any /orgs/* path that is NOT inside a specific event.
// Eligible: /orgs, /orgs/favorites, /orgs/create, /orgs/foo, /orgs/foo/users.
// NOT eligible: /orgs/foo/events, /orgs/foo/events/bar/anything.
// Specifically gate on segments[2] (the "events" segment after /orgs/{slug})
// rather than `.includes("events")` so a hypothetical org slug literally named
// "events" doesn't accidentally hide the widget.
const show = computed(() => {
  const segments = route.path.split("/").filter(Boolean);
  return segments[0] === "orgs" && (segments.length < 3 || segments[2] !== "events");
});
</script>
