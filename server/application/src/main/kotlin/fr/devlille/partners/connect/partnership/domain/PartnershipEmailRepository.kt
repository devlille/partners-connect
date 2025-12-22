package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.notifications.domain.Destination

/**
 * Repository interface for fetching partnerships with email contact information.
 *
 * This repository is responsible for data fetching only - it does NOT handle
 * email sending logic. Email sending is orchestrated in route handlers via
 * the NotificationRepository.
 */
interface PartnershipEmailRepository {
    /**
     * Fetch destinations with their associated email contacts based on filters.
     *
     * Returns partnerships that match ALL provided filter criteria (AND logic).
     * Each partnership includes:
     * - The organizer's email contact (if an organizer is assigned)
     * - All company email contacts from the company_emails table
     *
     * @param eventSlug The event slug to filter partnerships
     * @param filters Partnership filters (validated, paid, agreement-signed, pack_id, etc.)
     * @return List of destinations
     */
    suspend fun getPartnershipDestination(eventSlug: String, filters: PartnershipFilters): List<Destination>
}
