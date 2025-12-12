<template>
  <div id="main-nav">
    <p class="k-skip">
      <a href="#page-body">Sauter la navigation</a>
    </p>
    <nav aria-label="Navigation principale">
      <ul>
        <li v-for="item in menuItems" :key="item.key">
          <NuxtLink :to="item.to">
            <svg role="img" width="24" height="24" aria-hidden="true">
              <use :href="`/img/sprite.svg#${item.icon}`" />
            </svg>
            {{ item.label }}
          </NuxtLink>
        </li>

        <li v-if="isAuthenticated">
          <button @click="handleLogout">
            <svg role="img" width="24" height="24" aria-hidden="true">
              <use href="/img/sprite.svg#ic-parameters" />
            </svg>
            Déconnexion
          </button>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script lang="ts" setup>
interface MenuItem {
  key: string
  to: string
  icon: string
  label: string
  condition?: boolean
}

interface Props {
  items?: MenuItem[]
  type?: 'general' | 'organisation' | 'event'
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  type: 'general'
})

const route = useRoute()
const { isAuthenticated, logout, initializeAuth } = useAuth()

onMounted(() => {
  initializeAuth()
})

const handleLogout = async () => {
  await logout()
}

const menuItems = computed(() => {
  if (props.items.length > 0) {
    return props.items.filter(item => item.condition !== false)
  }

  switch (props.type) {
    case 'general':
      return [
        {
          key: 'orgs',
          to: '/orgs',
          icon: 'ic-sponsor',
          label: 'Organisations',
          condition: isAuthenticated.value
        },
        {
          key: 'settings',
          to: '/settings',
          icon: 'ic-parameters',
          label: 'Paramètres',
          condition: isAuthenticated.value
        }
      ].filter(item => item.condition !== false)

    case 'organisation':
      return [
        {
          key: 'events',
          to: `/orgs/${route.params.slug}/events`,
          icon: 'ic-sponsor',
          label: 'Evènements'
        },
        {
          key: 'settings',
          to: `/orgs/${route.params.slug}/settings`,
          icon: 'ic-parameters',
          label: 'Paramètres'
        }
      ]

    case 'event':
      return [
        {
          key: 'partners',
          to: `/orgs/${route.params.slug}/events/${route.params.id}/partners`,
          icon: 'ic-sponsor',
          label: 'Partenaires'
        },
        {
          key: 'settings',
          to: `/orgs/${route.params.slug}/events/${route.params.id}/settings`,
          icon: 'ic-parameters',
          label: 'Paramètres'
        }
      ]

    default:
      return []
  }
})
</script>

<style></style>
