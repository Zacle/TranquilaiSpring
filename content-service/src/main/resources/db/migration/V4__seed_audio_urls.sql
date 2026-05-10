-- Seed meditation audio URLs.
-- Replace placeholder URLs with real CDN URLs after uploading files to Firebase Storage / S3.
-- topic_id values must match the id field in the mobile's MeditationContent.kt.

INSERT INTO meditation_audio (topic_id, audio_url, updated_at) VALUES
-- MINDFULNESS (7)
('mindfulness_calm_focus',           'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_calm_focus.mp3',           0),
('mindfulness_body_awareness',       'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_body_awareness.mp3',       0),
('mindfulness_breath_awareness',     'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_breath_awareness.mp3',     0),
('mindfulness_loving_kindness',      'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_loving_kindness.mp3',      0),
('mindfulness_mountain',             'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_mountain.mp3',             0),
('mindfulness_present_moment',       'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_present_moment.mp3',       0),
('mindfulness_open_awareness',       'https://storage.googleapis.com/tranquilai-media/meditation/mindfulness_open_awareness.mp3',       0),
-- STRESS REDUCTION (6)
('stress_body_scan',                 'https://storage.googleapis.com/tranquilai-media/meditation/stress_body_scan.mp3',                 0),
('stress_progressive_muscle',        'https://storage.googleapis.com/tranquilai-media/meditation/stress_progressive_muscle.mp3',        0),
('stress_release',                   'https://storage.googleapis.com/tranquilai-media/meditation/stress_release.mp3',                   0),
('stress_quick_reset',               'https://storage.googleapis.com/tranquilai-media/meditation/stress_quick_reset.mp3',               0),
('stress_nature_visualization',      'https://storage.googleapis.com/tranquilai-media/meditation/stress_nature_visualization.mp3',      0),
('stress_breath_counting',           'https://storage.googleapis.com/tranquilai-media/meditation/stress_breath_counting.mp3',           0),
-- ANXIETY RELIEF (6)
('anxiety_grounding',                'https://storage.googleapis.com/tranquilai-media/meditation/anxiety_grounding.mp3',                0),
('anxiety_safe_place',               'https://storage.googleapis.com/tranquilai-media/meditation/anxiety_safe_place.mp3',               0),
('anxiety_thought_observation',      'https://storage.googleapis.com/tranquilai-media/meditation/anxiety_thought_observation.mp3',      0),
('anxiety_breath_anchor',            'https://storage.googleapis.com/tranquilai-media/meditation/anxiety_breath_anchor.mp3',            0),
('anxiety_letting_go',               'https://storage.googleapis.com/tranquilai-media/meditation/anxiety_letting_go.mp3',               0),
('anxiety_calm_mind',                'https://storage.googleapis.com/tranquilai-media/meditation/anxiety_calm_mind.mp3',                0),
-- SLEEP (6)
('sleep_preparation',                'https://storage.googleapis.com/tranquilai-media/meditation/sleep_preparation.mp3',                0),
('sleep_body_relaxation',            'https://storage.googleapis.com/tranquilai-media/meditation/sleep_body_relaxation.mp3',            0),
('sleep_peaceful_night',             'https://storage.googleapis.com/tranquilai-media/meditation/sleep_peaceful_night.mp3',             0),
('sleep_deep_rest',                  'https://storage.googleapis.com/tranquilai-media/meditation/sleep_deep_rest.mp3',                  0),
('sleep_gratitude',                  'https://storage.googleapis.com/tranquilai-media/meditation/sleep_gratitude.mp3',                  0),
('sleep_counting_breaths',           'https://storage.googleapis.com/tranquilai-media/meditation/sleep_counting_breaths.mp3',           0),
-- EMOTIONAL BALANCE (6)
('emotional_self_compassion',        'https://storage.googleapis.com/tranquilai-media/meditation/emotional_self_compassion.mp3',        0),
('emotional_heart_opening',          'https://storage.googleapis.com/tranquilai-media/meditation/emotional_heart_opening.mp3',          0),
('emotional_processing',             'https://storage.googleapis.com/tranquilai-media/meditation/emotional_processing.mp3',             0),
('emotional_inner_peace',            'https://storage.googleapis.com/tranquilai-media/meditation/emotional_inner_peace.mp3',            0),
('emotional_forgiveness',            'https://storage.googleapis.com/tranquilai-media/meditation/emotional_forgiveness.mp3',            0),
('emotional_joy_cultivation',        'https://storage.googleapis.com/tranquilai-media/meditation/emotional_joy_cultivation.mp3',        0),
-- CONFIDENCE (6)
('confidence_self_worth',            'https://storage.googleapis.com/tranquilai-media/meditation/confidence_self_worth.mp3',            0),
('confidence_inner_strength',        'https://storage.googleapis.com/tranquilai-media/meditation/confidence_inner_strength.mp3',        0),
('confidence_positive_affirmation',  'https://storage.googleapis.com/tranquilai-media/meditation/confidence_positive_affirmation.mp3',  0),
('confidence_success_visualization', 'https://storage.googleapis.com/tranquilai-media/meditation/confidence_success_visualization.mp3', 0),
('confidence_empowerment',           'https://storage.googleapis.com/tranquilai-media/meditation/confidence_empowerment.mp3',           0),
('confidence_courage',               'https://storage.googleapis.com/tranquilai-media/meditation/confidence_courage.mp3',               0),
-- SELF LOVE (6)
('self_love_compassion',             'https://storage.googleapis.com/tranquilai-media/meditation/self_love_compassion.mp3',             0),
('self_love_acceptance',             'https://storage.googleapis.com/tranquilai-media/meditation/self_love_acceptance.mp3',             0),
('self_love_kindness',               'https://storage.googleapis.com/tranquilai-media/meditation/self_love_kindness.mp3',               0),
('self_love_healing',                'https://storage.googleapis.com/tranquilai-media/meditation/self_love_healing.mp3',                0),
('self_love_worthiness',             'https://storage.googleapis.com/tranquilai-media/meditation/self_love_worthiness.mp3',             0),
('self_love_gratitude',              'https://storage.googleapis.com/tranquilai-media/meditation/self_love_gratitude.mp3',              0)
ON CONFLICT (topic_id) DO NOTHING;

-- Seed ambient sound audio URLs.
-- sound_id values must match the id field in the mobile's ambientSounds list in MeditationContent.kt.

INSERT INTO ambient_sound_audio (sound_id, audio_url, updated_at) VALUES
-- NATURE (6)
('nature_rain',          'https://storage.googleapis.com/tranquilai-media/ambient/nature_rain.mp3',          0),
('nature_ocean',         'https://storage.googleapis.com/tranquilai-media/ambient/nature_ocean.mp3',         0),
('nature_forest',        'https://storage.googleapis.com/tranquilai-media/ambient/nature_forest.mp3',        0),
('nature_river',         'https://storage.googleapis.com/tranquilai-media/ambient/nature_river.mp3',         0),
('nature_thunderstorm',  'https://storage.googleapis.com/tranquilai-media/ambient/nature_thunderstorm.mp3',  0),
('nature_wind',          'https://storage.googleapis.com/tranquilai-media/ambient/nature_wind.mp3',          0),
-- TRAFFIC (4)
('traffic_cafe',         'https://storage.googleapis.com/tranquilai-media/ambient/traffic_cafe.mp3',         0),
('traffic_city',         'https://storage.googleapis.com/tranquilai-media/ambient/traffic_city.mp3',         0),
('traffic_train',        'https://storage.googleapis.com/tranquilai-media/ambient/traffic_train.mp3',        0),
('traffic_library',      'https://storage.googleapis.com/tranquilai-media/ambient/traffic_library.mp3',      0),
-- SLEEP (4)
('sleep_white_noise',    'https://storage.googleapis.com/tranquilai-media/ambient/sleep_white_noise.mp3',    0),
('sleep_pink_noise',     'https://storage.googleapis.com/tranquilai-media/ambient/sleep_pink_noise.mp3',     0),
('sleep_brown_noise',    'https://storage.googleapis.com/tranquilai-media/ambient/sleep_brown_noise.mp3',    0),
('sleep_fan',            'https://storage.googleapis.com/tranquilai-media/ambient/sleep_fan.mp3',            0),
-- ANIMALS (4)
('animals_birds',        'https://storage.googleapis.com/tranquilai-media/ambient/animals_birds.mp3',        0),
('animals_crickets',     'https://storage.googleapis.com/tranquilai-media/ambient/animals_crickets.mp3',     0),
('animals_frogs',        'https://storage.googleapis.com/tranquilai-media/ambient/animals_frogs.mp3',        0),
('animals_whales',       'https://storage.googleapis.com/tranquilai-media/ambient/animals_whales.mp3',       0)
ON CONFLICT (sound_id) DO NOTHING;
