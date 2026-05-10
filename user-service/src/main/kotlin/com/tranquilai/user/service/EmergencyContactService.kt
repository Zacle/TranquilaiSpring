package com.tranquilai.user.service

import com.tranquilai.user.dto.request.CreateEmergencyContactRequest
import com.tranquilai.user.dto.request.UpdateEmergencyContactRequest
import com.tranquilai.user.dto.response.EmergencyContactResponse
import com.tranquilai.user.entity.EmergencyContact
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.EmergencyContactRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class EmergencyContactService(
    private val repo: EmergencyContactRepository,
) {

    fun create(userId: UUID, request: CreateEmergencyContactRequest): EmergencyContactResponse {
        if (request.isPrimary) {
            repo.clearPrimaryForUser(userId)
        }
        val contact = repo.save(
            EmergencyContact(
                userId = userId,
                name = request.name,
                phoneNumber = request.phoneNumber,
                email = request.email,
                relationship = request.relationship,
                isPrimary = request.isPrimary,
            )
        )
        return contact.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID): List<EmergencyContactResponse> =
        repo.findByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun get(userId: UUID, id: UUID): EmergencyContactResponse {
        val contact = findByIdAndUser(userId, id)
        return contact.toResponse()
    }

    @Transactional(readOnly = true)
    fun getPrimary(userId: UUID): EmergencyContactResponse? =
        repo.findByUserIdAndIsPrimaryTrue(userId)?.toResponse()

    fun update(userId: UUID, id: UUID, request: UpdateEmergencyContactRequest): EmergencyContactResponse {
        val contact = findByIdAndUser(userId, id)
        request.name?.let { contact.name = it }
        request.phoneNumber?.let { contact.phoneNumber = it }
        request.email?.let { contact.email = it }
        request.relationship?.let { contact.relationship = it }
        contact.updatedAt = System.currentTimeMillis()
        return repo.save(contact).toResponse()
    }

    fun setPrimary(userId: UUID, id: UUID): EmergencyContactResponse {
        val contact = findByIdAndUser(userId, id)
        repo.clearPrimaryForUser(userId)
        contact.isPrimary = true
        contact.updatedAt = System.currentTimeMillis()
        return repo.save(contact).toResponse()
    }

    fun delete(userId: UUID, id: UUID) {
        val contact = findByIdAndUser(userId, id)
        repo.delete(contact)
    }

    fun deleteAll(userId: UUID) {
        repo.deleteByUserId(userId)
    }

    private fun findByIdAndUser(userId: UUID, id: UUID): EmergencyContact =
        repo.findByIdAndUserId(id, userId)
            ?: throw UserNotFoundException("Emergency contact $id not found")
}

private fun EmergencyContact.toResponse() = EmergencyContactResponse(
    id = id,
    userId = userId,
    name = name,
    phoneNumber = phoneNumber,
    email = email,
    relationship = relationship,
    isPrimary = isPrimary,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
