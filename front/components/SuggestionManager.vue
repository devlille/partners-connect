<template>
  <div class="suggestion-manager">
    <h3>Gestion des Suggestions</h3>
    
    <!-- Loading state -->
    <div v-if="loading" class="loading">
      <p>Chargement en cours...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="error">
      <p>Erreur: {{ error }}</p>
      <button @click="$emit('refresh')" class="retry-btn">Réessayer</button>
    </div>

    <!-- No partnership found -->
    <div v-else-if="!partnership" class="no-partnership">
      <p>Aucun partenariat trouvé pour cette entreprise.</p>
    </div>

    <!-- Partnership found -->
    <div v-else class="partnership-content">
      <!-- Current Selected Pack -->
      <div class="current-pack" v-if="partnership.selected_pack">
        <h4>Pack Actuel</h4>
        <div class="pack-info">
          <strong>{{ partnership.selected_pack.name }}</strong>
          <p>{{ partnership.selected_pack.description || 'Aucune description disponible' }}</p>
          <p class="price">Prix: {{ partnership.selected_pack.base_price }}€</p>
        </div>
      </div>

      <!-- Suggestion Section -->
      <div class="suggestion-section">
        <h4>Suggestion de l'Organisateur</h4>
        
        <!-- No suggestion -->
        <div v-if="!partnership.suggestion_pack" class="no-suggestion">
          <p>Aucune suggestion en attente.</p>
        </div>

        <!-- Pending suggestion -->
        <div v-else-if="!partnership.suggestion_approved_at && !partnership.suggestion_declined_at" class="pending-suggestion">
          <div class="suggested-pack">
            <h5>Pack Suggéré</h5>
            <div class="pack-info">
              <strong>{{ partnership.suggestion_pack.name }}</strong>
              <p>{{ partnership.suggestion_pack.description || 'Aucune description disponible' }}</p>
              <p class="price">Prix: {{ partnership.suggestion_pack.base_price }}€</p>
            </div>
          </div>
          
          <div class="suggestion-actions">
            <p><strong>Que souhaitez-vous faire avec cette suggestion ?</strong></p>
            <div class="buttons-bar">
              <button 
                @click="approveSuggestion" 
                :disabled="processing"
                class="approve-btn"
              >
                {{ processing === 'approve' ? 'Approbation...' : 'Accepter la suggestion' }}
              </button>
              <button 
                @click="declineSuggestion" 
                :disabled="processing"
                class="decline-btn"
              >
                {{ processing === 'decline' ? 'Refus...' : 'Refuser la suggestion' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Approved suggestion -->
        <div v-else-if="partnership.suggestion_approved_at" class="approved-suggestion">
          <div class="status-message success">
            <p>✅ Suggestion acceptée le {{ formatDate(partnership.suggestion_approved_at) }}</p>
          </div>
          <div class="suggested-pack">
            <h5>Pack Approuvé</h5>
            <div class="pack-info">
              <strong>{{ partnership.suggestion_pack.name }}</strong>
              <p>{{ partnership.suggestion_pack.description || 'Aucune description disponible' }}</p>
              <p class="price">Prix: {{ partnership.suggestion_pack.base_price }}€</p>
            </div>
          </div>
        </div>

        <!-- Declined suggestion -->
        <div v-else-if="partnership.suggestion_declined_at" class="declined-suggestion">
          <div class="status-message error">
            <p>❌ Suggestion refusée le {{ formatDate(partnership.suggestion_declined_at) }}</p>
          </div>
          <div class="suggested-pack">
            <h5>Pack Refusé</h5>
            <div class="pack-info">
              <strong>{{ partnership.suggestion_pack.name }}</strong>
              <p>{{ partnership.suggestion_pack.description || 'Aucune description disponible' }}</p>
              <p class="price">Prix: {{ partnership.suggestion_pack.base_price }}€</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  eventId: {
    type: String,
    required: true
  },
  companyId: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['refresh'])

const partnership = ref(null)
const loading = ref(false)
const error = ref(null)
const processing = ref(null) // 'approve', 'decline', or null

const loadPartnership = async () => {
  loading.value = true
  error.value = null
  
  try {
    // Mock a delay to simulate API call
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // For demo purposes, use mock data with a pending suggestion
    partnership.value = {
      id: "123e4567-e89b-12d3-a456-426614174000",
      selected_pack: {
        name: "Silver",
        description: "Pack Argent avec stand et logo",
        base_price: 1500
      },
      suggestion_pack: {
        name: "Gold",
        description: "Pack Or avec stand premium, logo et conférence",
        base_price: 3000
      },
      suggestion_sent_at: "2025-01-15T10:30:00Z",
      suggestion_approved_at: null,
      suggestion_declined_at: null
    }
  } catch (err) {
    error.value = err.message || 'Failed to load partnership'
    console.error('Error loading partnership:', err)
  } finally {
    loading.value = false
  }
}

const approveSuggestion = async () => {
  processing.value = 'approve'
  
  try {
    // Mock a delay to simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // Update local state to show approval
    partnership.value.suggestion_approved_at = new Date().toISOString()
    partnership.value.suggestion_declined_at = null
    
    // In a real app, this would call the backend API:
    // const response = await $fetch(`/api/events/${props.eventId}/companies/${props.companyId}/partnership/${partnership.value.id}/suggestion-approve`, {
    //   method: 'POST'
    // })
    
    emit('refresh')
  } catch (err) {
    error.value = err.message || 'Failed to approve suggestion'
    console.error('Error approving suggestion:', err)
  } finally {
    processing.value = null
  }
}

const declineSuggestion = async () => {
  processing.value = 'decline'
  
  try {
    // Mock a delay to simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // Update local state to show decline
    partnership.value.suggestion_declined_at = new Date().toISOString()
    partnership.value.suggestion_approved_at = null
    
    // In a real app, this would call the backend API:
    // const response = await $fetch(`/api/events/${props.eventId}/companies/${props.companyId}/partnership/${partnership.value.id}/suggestion-decline`, {
    //   method: 'POST'
    // })
    
    emit('refresh')
  } catch (err) {
    error.value = err.message || 'Failed to decline suggestion'
    console.error('Error declining suggestion:', err)
  } finally {
    processing.value = null
  }
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Load partnership data when component mounts
onMounted(() => {
  loadPartnership()
})

// Watch for prop changes and reload data
watch([() => props.eventId, () => props.companyId], () => {
  loadPartnership()
})
</script>

<style scoped>
.suggestion-manager {
  padding: 20px;
}

.loading, .error, .no-partnership {
  text-align: center;
  padding: 40px 20px;
}

.error {
  color: #dc3545;
}

.retry-btn {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 10px;
}

.retry-btn:hover {
  background-color: #0056b3;
}

.current-pack, .suggestion-section {
  margin-bottom: 30px;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.pack-info {
  margin-top: 10px;
  padding: 15px;
  background-color: white;
  border-radius: 4px;
  border-left: 4px solid #007bff;
}

.pack-info strong {
  font-size: 1.2em;
  color: #333;
}

.price {
  font-weight: bold;
  color: #28a745;
  margin: 5px 0;
}

.suggestion-actions {
  margin-top: 20px;
  padding: 20px;
  background-color: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 4px;
}

.buttons-bar {
  display: flex;
  gap: 15px;
  margin-top: 15px;
}

.approve-btn {
  background-color: #28a745;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.approve-btn:hover:not(:disabled) {
  background-color: #218838;
}

.decline-btn {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.decline-btn:hover:not(:disabled) {
  background-color: #c82333;
}

.approve-btn:disabled, .decline-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.status-message {
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
  font-weight: bold;
}

.status-message.success {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-message.error {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.no-suggestion {
  text-align: center;
  padding: 30px;
  color: #6c757d;
  font-style: italic;
}

.pending-suggestion {
  border-left: 4px solid #ffc107;
}

.approved-suggestion {
  border-left: 4px solid #28a745;
}

.declined-suggestion {
  border-left: 4px solid #dc3545;
}
</style>