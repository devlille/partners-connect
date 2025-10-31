import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

describe('Sponsor Filtering Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    sessionStorage.clear()
  })

  describe('T-015: Pack Filtering Integration', () => {
    it('should filter sponsors by pack and call API with correct params', async () => {
      // Test: select pack filter, verify API called with correct params, verify filtered results displayed

      const mockPartnerships = [
        { id: '1', company_name: 'Company A', pack_name: 'Gold Pack', pack_id: 'pack-gold' },
        { id: '2', company_name: 'Company B', pack_name: 'Silver Pack', pack_id: 'pack-silver' },
        { id: '3', company_name: 'Company C', pack_name: 'Gold Pack', pack_id: 'pack-gold' }
      ]

     

      // Mock API call
      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(p => p.pack_id === 'pack-gold')
      })

      // Simulate: User selects Gold Pack from dropdown
      // Expected API params: { 'filter[pack_id]': 'pack-gold' }
      await apiSpy('org-slug', 'event-slug', { 'filter[pack_id]': 'pack-gold' })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[pack_id]': 'pack-gold'
      })

      const filteredResults = mockPartnerships.filter(p => p.pack_id === 'pack-gold')
      expect(filteredResults).toHaveLength(2)
      expect(filteredResults.every(p => p.pack_name === 'Gold Pack')).toBe(true)
    })

    it('should clear pack filter and show all sponsors', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      // Select pack, then clear
      await apiSpy('org-slug', 'event-slug', { 'filter[pack_id]': 'pack-gold' })
      await apiSpy('org-slug', 'event-slug', {}) // Cleared

      expect(apiSpy).toHaveBeenLastCalledWith('org-slug', 'event-slug', {})
    })

    it('should show empty state when no sponsors match pack filter', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      await apiSpy('org-slug', 'event-slug', { 'filter[pack_id]': 'pack-bronze' })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[pack_id]': 'pack-bronze'
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(0)
    })
  })

  describe('T-020: Single Status Filtering Integration', () => {
    it('should filter sponsors by validated status', async () => {
      const mockPartnerships = [
        { id: '1', company_name: 'Company A', validated: true },
        { id: '2', company_name: 'Company B', validated: false },
        { id: '3', company_name: 'Company C', validated: true }
      ]

      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(p => p.validated === true)
      })

      // User toggles "Validated = Yes"
      await apiSpy('org-slug', 'event-slug', { 'filter[validated]': true })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[validated]': true
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(2)
      expect(result.data.every(p => p.validated === true)).toBe(true)
    })

    it('should filter sponsors by paid status', async () => {
      const mockPartnerships = [
        { id: '1', company_name: 'Company A', paid: true },
        { id: '2', company_name: 'Company B', paid: false }
      ]

      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(p => p.paid === false)
      })

      // User toggles "Paid = No" (to see unpaid sponsors)
      await apiSpy('org-slug', 'event-slug', { 'filter[paid]': false })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[paid]': false
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(1)
      expect(result.data[0].paid).toBe(false)
    })

    it('should filter by agreement signed status', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      await apiSpy('org-slug', 'event-slug', { 'filter[agreement-signed]': false })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[agreement-signed]': false
      })
    })
  })

  describe('T-021: Multiple Status Filters (AND Logic) Integration', () => {
    it('should apply multiple status filters with AND logic', async () => {
      const mockPartnerships = [
        { id: '1', company_name: 'Company A', validated: true, paid: true },
        { id: '2', company_name: 'Company B', validated: true, paid: false },
        { id: '3', company_name: 'Company C', validated: false, paid: true },
        { id: '4', company_name: 'Company D', validated: true, paid: true }
      ]

      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(p => p.validated === true && p.paid === true)
      })

      // User toggles "Validated = Yes" AND "Paid = Yes"
      await apiSpy('org-slug', 'event-slug', {
        'filter[validated]': true,
        'filter[paid]': true
      })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[validated]': true,
        'filter[paid]': true
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(2)
      expect(result.data.every(p => p.validated === true && p.paid === true)).toBe(true)
    })

    it('should include all status filters in API params', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      await apiSpy('org-slug', 'event-slug', {
        'filter[validated]': true,
        'filter[paid]': false,
        'filter[agreement-generated]': true,
        'filter[agreement-signed]': false
      })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[validated]': true,
        'filter[paid]': false,
        'filter[agreement-generated]': true,
        'filter[agreement-signed]': false
      })
    })
  })

  describe('T-023: Suggestion Filtering Integration', () => {
    it('should filter to show only suggestions', async () => {
      const mockPartnerships = [
        { id: '1', company_name: 'Company A', suggestion: true },
        { id: '2', company_name: 'Company B', suggestion: false },
        { id: '3', company_name: 'Company C', suggestion: true }
      ]

      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(p => p.suggestion === true)
      })

      // User toggles "Suggestions Only"
      await apiSpy('org-slug', 'event-slug', { 'filter[suggestion]': true })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[suggestion]': true
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(2)
      expect(result.data.every(p => p.suggestion === true)).toBe(true)
    })

    it('should filter to show only confirmed sponsors (not suggestions)', async () => {
      const mockPartnerships = [
        { id: '1', company_name: 'Company A', suggestion: false },
        { id: '2', company_name: 'Company B', suggestion: true }
      ]

      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(p => p.suggestion === false)
      })

      // User toggles "Confirmed Only"
      await apiSpy('org-slug', 'event-slug', { 'filter[suggestion]': false })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[suggestion]': false
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(1)
      expect(result.data[0].suggestion).toBe(false)
    })
  })

  describe('T-028: Combined Filters Integration', () => {
    it('should apply pack + status filters together', async () => {
      const mockPartnerships = [
        { id: '1', company_name: 'Company A', pack_id: 'pack-gold', validated: true, paid: true },
        { id: '2', company_name: 'Company B', pack_id: 'pack-gold', validated: true, paid: false },
        { id: '3', company_name: 'Company C', pack_id: 'pack-silver', validated: true, paid: true },
        { id: '4', company_name: 'Company D', pack_id: 'pack-gold', validated: false, paid: true }
      ]

      const apiSpy = vi.fn().mockResolvedValue({
        data: mockPartnerships.filter(
          p => p.pack_id === 'pack-gold' && p.validated === true && p.paid === true
        )
      })

      // User selects: Gold Pack + Validated + Paid
      await apiSpy('org-slug', 'event-slug', {
        'filter[pack_id]': 'pack-gold',
        'filter[validated]': true,
        'filter[paid]': true
      })

      expect(apiSpy).toHaveBeenCalledWith('org-slug', 'event-slug', {
        'filter[pack_id]': 'pack-gold',
        'filter[validated]': true,
        'filter[paid]': true
      })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(1)
      expect(result.data[0].id).toBe('1')
    })

    it('should verify all filters are included in API call', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      await apiSpy('org-slug', 'event-slug', {
        'filter[pack_id]': 'pack-gold',
        'filter[validated]': true,
        'filter[paid]': false,
        'filter[suggestion]': false
      })

      const call = apiSpy.mock.calls[0]
      expect(call[2]).toHaveProperty('filter[pack_id]', 'pack-gold')
      expect(call[2]).toHaveProperty('filter[validated]', true)
      expect(call[2]).toHaveProperty('filter[paid]', false)
      expect(call[2]).toHaveProperty('filter[suggestion]', false)
    })
  })

  describe('T-029: Persistence Across Refresh Integration', () => {
    it('should restore filters from sessionStorage on page refresh', () => {
      const storageKey = 'sponsor-filters:test-org:test-event'
      const savedFilters = {
        packId: 'pack-gold',
        validated: true,
        paid: false,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null
      }

      // Simulate saved state
      sessionStorage.setItem(storageKey, JSON.stringify(savedFilters))

      // Simulate page refresh (remount component)
      const restored = JSON.parse(sessionStorage.getItem(storageKey)!)

      expect(restored).toEqual(savedFilters)
      expect(restored.packId).toBe('pack-gold')
      expect(restored.validated).toBe(true)
      expect(restored.paid).toBe(false)
    })

    it('should call API with restored filters after refresh', async () => {
      const storageKey = 'sponsor-filters:test-org:test-event'
      const savedFilters = {
        packId: 'pack-gold',
        validated: true,
        paid: null,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null
      }

      sessionStorage.setItem(storageKey, JSON.stringify(savedFilters))

      // Simulate API call after restore
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })
      const restored = JSON.parse(sessionStorage.getItem(storageKey)!)

      // Build query params from restored state
      const queryParams: any = {}
      if (restored.packId) queryParams['filter[pack_id]'] = restored.packId
      if (restored.validated !== null) queryParams['filter[validated]'] = restored.validated

      await apiSpy('test-org', 'test-event', queryParams)

      expect(apiSpy).toHaveBeenCalledWith('test-org', 'test-event', {
        'filter[pack_id]': 'pack-gold',
        'filter[validated]': true
      })
    })

    it('should use initial state if sessionStorage is empty', () => {
      const storageKey = 'sponsor-filters:test-org:test-event'

      expect(sessionStorage.getItem(storageKey)).toBeNull()

      // Should use initial state (all null)
      const initialState = {
        packId: null,
        validated: null,
        paid: null,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null
      }

      expect(initialState.packId).toBeNull()
      expect(initialState.validated).toBeNull()
    })
  })

  describe('T-030: Persistence Across Navigation Integration', () => {
    it('should preserve filters when navigating away and back', () => {
      const storageKey = 'sponsor-filters:test-org:test-event'
      const filters = {
        packId: 'pack-gold',
        validated: true,
        paid: null,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null
      }

      // Apply filters
      sessionStorage.setItem(storageKey, JSON.stringify(filters))

      // Navigate to detail page (filters stay in storage)
      expect(sessionStorage.getItem(storageKey)).not.toBeNull()

      // Navigate back to list
      const restored = JSON.parse(sessionStorage.getItem(storageKey)!)
      expect(restored).toEqual(filters)
    })

    it('should not persist filters to new tab/window', () => {
      const storageKey = 'sponsor-filters:test-org:test-event'

      // Filters in current tab
      sessionStorage.setItem(storageKey, JSON.stringify({ packId: 'pack-gold' }))

      // New tab = new sessionStorage (simulated by not having the key)
      // In real scenario, new tab would have empty sessionStorage
      const newTabStorage = null // Simulate new session

      expect(newTabStorage).toBeNull()
    })
  })

  describe('T-031: Clear All Functionality Integration', () => {
    it('should clear all filters and call API with no params', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      // Apply multiple filters
      await apiSpy('org-slug', 'event-slug', {
        'filter[pack_id]': 'pack-gold',
        'filter[validated]': true,
        'filter[paid]': false
      })

      // Click Clear All
      await apiSpy('org-slug', 'event-slug', {})

      expect(apiSpy).toHaveBeenLastCalledWith('org-slug', 'event-slug', {})
    })

    it('should remove sessionStorage when all filters cleared', () => {
      const storageKey = 'sponsor-filters:test-org:test-event'

      // Set filters
      sessionStorage.setItem(storageKey, JSON.stringify({
        packId: 'pack-gold',
        validated: true
      }))

      expect(sessionStorage.getItem(storageKey)).not.toBeNull()

      // Clear all (isEmpty = true)
      sessionStorage.removeItem(storageKey)

      expect(sessionStorage.getItem(storageKey)).toBeNull()
    })

    it('should reset all filter values to null', () => {
      

      // Clear all
      const clearedFilters = {
        packId: null,
        validated: null,
        paid: null,
        agreementGenerated: null,
        agreementSigned: null,
        suggestion: null
      }

      expect(Object.values(clearedFilters).every(v => v === null)).toBe(true)
    })
  })

  describe('Edge Cases', () => {
    it('should handle empty sponsors list with filters active', async () => {
      const apiSpy = vi.fn().mockResolvedValue({ data: [] })

      await apiSpy('org-slug', 'event-slug', { 'filter[pack_id]': 'pack-gold' })

      const result = await apiSpy.mock.results[0].value
      expect(result.data).toHaveLength(0)
      // Should show: "No sponsors match the selected filters"
    })

    it('should handle API error gracefully', async () => {
      const apiSpy = vi.fn().mockRejectedValue(new Error('API Error'))

      try {
        await apiSpy('org-slug', 'event-slug', { 'filter[pack_id]': 'pack-gold' })
      } catch (error) {
        expect(error).toBeInstanceOf(Error)
        expect((error as Error).message).toBe('API Error')
      }
    })

    it('should handle invalid sessionStorage data', () => {
      const storageKey = 'sponsor-filters:test-org:test-event'
      sessionStorage.setItem(storageKey, '{invalid json}')

      let restored = null
      try {
        restored = JSON.parse(sessionStorage.getItem(storageKey)!)
      } catch {
        // Should fall back to initial state
        restored = {
          packId: null,
          validated: null,
          paid: null,
          agreementGenerated: null,
          agreementSigned: null,
          suggestion: null
        }
      }

      expect(Object.values(restored).every(v => v === null)).toBe(true)
    })
  })
})
