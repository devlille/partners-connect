package fr.devlille.partners.connect.companies.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request model for creating a new job offer.
 * Contains all required and optional fields for job offer creation.
 *
 * @property url Direct link to the detailed job posting (required, must be valid URI)
 * @property title Position name or job title (required, max 255 characters)
 * @property location Work location (required, can be remote/hybrid/on-site)
 * @property publicationDate When the job was posted (required, ISO format YYYY-MM-DDTHH:MM:SS, cannot be future)
 * @property endDate Application deadline (optional, ISO format, must be after publication date)
 * @property experienceYears Minimum experience required in years (optional, range 0-20)
 * @property salary Salary information as free text (optional, max 100 characters)
 */
@Serializable
data class CreateJobOffer(
    val url: String,
    val title: String,
    val location: String,
    @SerialName("publication_date")
    val publicationDate: LocalDateTime,
    @SerialName("end_date")
    val endDate: LocalDateTime? = null,
    @SerialName("experience_years")
    val experienceYears: Int? = null,
    val salary: String? = null,
)

/**
 * Request model for updating an existing job offer.
 * All fields are optional to support partial updates. At least one field must be provided.
 *
 * @property url New URL for the job posting (optional, must be valid URI if provided)
 * @property title New position title (optional, max 255 characters if provided)
 * @property location New work location (optional)
 * @property publicationDate New publication date (optional, ISO format, cannot be future)
 * @property endDate New application deadline (optional, ISO format, must be after publication date)
 * @property experienceYears New experience requirement (optional, range 0-20)
 * @property salary New salary information (optional, max 100 characters)
 */
@Serializable
data class UpdateJobOffer(
    val url: String? = null,
    val title: String? = null,
    val location: String? = null,
    @SerialName("publication_date")
    val publicationDate: LocalDateTime? = null,
    @SerialName("end_date")
    val endDate: LocalDateTime? = null,
    @SerialName("experience_years")
    val experienceYears: Int? = null,
    val salary: String? = null,
)

/**
 * Response model for job offer data.
 * Contains all job offer information returned by the API.
 *
 * @property id Unique identifier of the job offer
 * @property companyId UUID of the company that owns this job offer
 * @property url Direct link to the detailed job posting
 * @property title Position name or job title
 * @property location Work location description
 * @property publicationDate When the job was posted (ISO format)
 * @property endDate Application deadline (ISO format, nullable)
 * @property experienceYears Minimum experience required in years (nullable)
 * @property salary Salary information as free text (nullable)
 * @property createdAt Timestamp when the record was created (ISO format)
 * @property updatedAt Timestamp when the record was last modified (ISO format)
 */
@Serializable
data class JobOfferResponse(
    val id: String,
    @SerialName("company_id")
    val companyId: String,
    val url: String,
    val title: String,
    val location: String,
    @SerialName("publication_date")
    val publicationDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("experience_years")
    val experienceYears: Int? = null,
    val salary: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)
