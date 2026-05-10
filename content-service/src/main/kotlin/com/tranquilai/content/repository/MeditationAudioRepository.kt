package com.tranquilai.content.repository

import com.tranquilai.content.entity.MeditationAudio
import org.springframework.data.jpa.repository.JpaRepository

interface MeditationAudioRepository : JpaRepository<MeditationAudio, String>
