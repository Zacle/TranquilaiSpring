package com.tranquilai.content.repository

import com.tranquilai.content.entity.AmbientSoundAudio
import org.springframework.data.jpa.repository.JpaRepository

interface AmbientSoundAudioRepository : JpaRepository<AmbientSoundAudio, String>
