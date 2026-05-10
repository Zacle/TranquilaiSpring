package com.tranquilai.user.service

import com.tranquilai.user.dto.request.CreateEmergencyContactRequest
import com.tranquilai.user.dto.request.UpdateEmergencyContactRequest
import com.tranquilai.user.entity.EmergencyContact
import com.tranquilai.user.exception.UserNotFoundException
import com.tranquilai.user.repository.EmergencyContactRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

class EmergencyContactServiceTest {

    private val repo: EmergencyContactRepository = mock(EmergencyContactRepository::class.java)
    private val service = EmergencyContactService(repo)

    @Test
    fun `create clears existing primary when new contact is primary`() {
        val userId = UUID.randomUUID()
        `when`(repo.save(anyContact())).thenAnswer { it.getArgument<EmergencyContact>(0) }

        val response = service.create(
            userId,
            CreateEmergencyContactRequest(
                name = "Mom",
                phoneNumber = "+15550001111",
                relationship = "Parent",
                isPrimary = true,
            ),
        )

        assertEquals("Mom", response.name)
        assertTrue(response.isPrimary)
        verify(repo).clearPrimaryForUser(userId)
        verify(repo).save(anyContact())
    }

    @Test
    fun `list returns contacts for user`() {
        val userId = UUID.randomUUID()
        `when`(repo.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(
            listOf(contact(userId = userId, name = "Friend"), contact(userId = userId, name = "Sibling")),
        )

        val response = service.list(userId)

        assertEquals(listOf("Friend", "Sibling"), response.map { it.name })
    }

    @Test
    fun `update applies provided fields only`() {
        val userId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val contact = contact(id = id, userId = userId, name = "Old", relationship = "Friend")
        `when`(repo.findByIdAndUserId(id, userId)).thenReturn(contact)
        `when`(repo.save(contact)).thenReturn(contact)

        val response = service.update(
            userId,
            id,
            UpdateEmergencyContactRequest(name = "New", email = "new@example.com"),
        )

        assertEquals("New", response.name)
        assertEquals("new@example.com", response.email)
        assertEquals("Friend", response.relationship)
        verify(repo).save(contact)
    }

    @Test
    fun `setPrimary clears existing primary and saves selected contact`() {
        val userId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val contact = contact(id = id, userId = userId, isPrimary = false)
        `when`(repo.findByIdAndUserId(id, userId)).thenReturn(contact)
        `when`(repo.save(contact)).thenReturn(contact)

        val response = service.setPrimary(userId, id)

        assertTrue(response.isPrimary)
        verify(repo).clearPrimaryForUser(userId)
        verify(repo).save(contact)
    }

    @Test
    fun `getPrimary returns nullable primary contact`() {
        val userId = UUID.randomUUID()
        `when`(repo.findByUserIdAndIsPrimaryTrue(userId)).thenReturn(contact(userId = userId, isPrimary = true))

        val response = service.getPrimary(userId)

        assertEquals(userId, response?.userId)
        assertTrue(response?.isPrimary == true)
    }

    @Test
    fun `delete removes matching contact`() {
        val userId = UUID.randomUUID()
        val id = UUID.randomUUID()
        val contact = contact(id = id, userId = userId)
        `when`(repo.findByIdAndUserId(id, userId)).thenReturn(contact)

        service.delete(userId, id)

        verify(repo).delete(contact)
    }

    @Test
    fun `get throws when contact is missing for user`() {
        val userId = UUID.randomUUID()
        val id = UUID.randomUUID()
        `when`(repo.findByIdAndUserId(id, userId)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            service.get(userId, id)
        }
    }

    @Test
    fun `deleteAll delegates by user id`() {
        val userId = UUID.randomUUID()

        service.deleteAll(userId)

        verify(repo).deleteByUserId(userId)
    }

    private fun contact(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        name: String = "Contact",
        relationship: String = "Friend",
        isPrimary: Boolean = false,
    ) = EmergencyContact(
        id = id,
        userId = userId,
        name = name,
        phoneNumber = "+15550001111",
        email = null,
        relationship = relationship,
        isPrimary = isPrimary,
    )

    @Suppress("UNCHECKED_CAST")
    private fun anyContact(): EmergencyContact {
        any(EmergencyContact::class.java)
        return uninitialized()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T
}
