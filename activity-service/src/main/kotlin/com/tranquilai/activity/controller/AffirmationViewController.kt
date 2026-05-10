package com.tranquilai.activity.controller

import com.tranquilai.activity.dto.request.LogAffirmationViewRequest
import com.tranquilai.activity.dto.response.AffirmationViewResponse
import com.tranquilai.activity.security.GatewayUser
import com.tranquilai.activity.service.AffirmationViewService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/affirmation-views")
class AffirmationViewController(private val affirmationViewService: AffirmationViewService) {

    /** POST /api/affirmation-views — record that the user viewed an affirmation */
    @PostMapping
    fun log(
        @AuthenticationPrincipal user: GatewayUser,
        @Valid @RequestBody request: LogAffirmationViewRequest,
    ): ResponseEntity<AffirmationViewResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(affirmationViewService.log(user.id, request))
}
