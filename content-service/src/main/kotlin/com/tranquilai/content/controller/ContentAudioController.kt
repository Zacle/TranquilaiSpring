package com.tranquilai.content.controller

import com.tranquilai.content.service.AudioService
import com.tranquilai.content.service.AudioUrlsResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/content")
class ContentAudioController(private val audioService: AudioService) {

    @GetMapping("/audio")
    fun getAudioUrls(): ResponseEntity<AudioUrlsResponse> =
        ResponseEntity.ok(audioService.getAllAudioUrls())
}
