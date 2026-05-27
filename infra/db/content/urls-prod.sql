-- Seed meditation audio URLs.
-- Replace placeholder URLs with real CDN URLs after uploading files to Firebase Storage / S3.
-- topic_id values must match the id field in the mobile's MeditationContent.kt.

INSERT INTO meditation_audio (topic_id, audio_url, updated_at) VALUES
-- MINDFULNESS (7)
('mindfulness_calm_focus',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_calm_focus.mp3?alt=media&token=dfb0ce32-a73a-4a6b-b3cc-5c357bebbd16',           0),
('mindfulness_serene_breath',       'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_serene_breath.mp3?alt=media&token=f61eea54-6796-4c6d-a534-faba5a9c5dc8',       0),
('mindfulness_present_moment',     'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_present_moment.mp3?alt=media&token=2ecfdffb-1724-4607-b80f-5767d6d7161f',     0),
('mindfulness_body_awareness',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_body_awareness.mp3?alt=media&token=6e6a0733-1e16-4894-bed2-f968c3f2c4ba',      0),
('mindfulness_listening',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_listening.mp3?alt=media&token=20c4b70c-0e97-4112-aadb-cadd04bf67f7',             0),
('mindfulness_awareness_scan',       'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_awareness_scan.mp3?alt=media&token=dc205ae1-9fba-4c0e-ba7c-ba9556a9dc28',       0),
-- STRESS REDUCTION (6)
('stress_relief',                 'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_relief.mp3?alt=media&token=bb0e65eb-35e6-4471-9828-04c6d5fb4634',                 0),
('stress_deep_relaxation',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_relief.mp3?alt=media&token=bb0e65eb-35e6-4471-9828-04c6d5fb4634',        0),
('stress_letting_go',                   'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_letting_go.mp3?alt=media&token=7c7c1706-f349-4d86-8ad2-b7299e66249d',                   0),
('stress_release_tension',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_letting_go.mp3?alt=media&token=7c7c1706-f349-4d86-8ad2-b7299e66249d',               0),
('stress_gentle_unwind',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_letting_go.mp3?alt=media&token=7c7c1706-f349-4d86-8ad2-b7299e66249d',      0),
('stress_end_of_day',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_end_of_day.mp3?alt=media&token=a0067f58-c917-48df-b0d7-f843f8c522ed',           0),
-- ANXIETY RELIEF (6)
('anxiety_ease',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_end_of_day.mp3?alt=media&token=a0067f58-c917-48df-b0d7-f843f8c522ed',                0),
('anxiety_grounding_breath',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_grounding_breath.mp3?alt=media&token=f5af9691-20a8-4c1d-a9e6-c0460b139a19',               0),
('anxiety_safe_space',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_safe_space.mp3?alt=media&token=07308ea1-f691-4470-bb58-318409f6f921',      0),
('anxiety_quiet_mind',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_quiet_mind.mp3?alt=media&token=4e05ddee-4130-44da-aa06-905d43a17d87',            0),
('anxiety_reset',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_reset.mp3?alt=media&token=a3905fe3-c4df-44dd-8b91-63269bae684c',               0),
('anxiety_panic_calm',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_panic_calm.mp3?alt=media&token=d7dcd7b8-50e4-4b68-95ea-d1b8fd600a2d',                0),
-- SLEEP (6)
('sleep_preparation',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_preparation.mp3?alt=media&token=0de1f0a5-3e63-4341-9598-f4c7e3d19d7d',                0),
('sleep_deep_journey',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_deep_journey.mp3?alt=media&token=c20e4691-0c2d-4112-81dd-1a115e391362',            0),
('sleep_mind_rest',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_mind_rest.mp3?alt=media&token=069fae44-4376-42f8-a77c-ce01a47a0a0a',             0),
('sleep_wind_down',                  'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_wind_down.mp3?alt=media&token=6653e890-975e-41a9-a1cc-a0bbeb3c1101',                  0),
('sleep_body_into_sleep',                  'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_body_into_sleep.mp3?alt=media&token=5e529ac2-f908-411a-8387-7ca776452680',                  0),
('sleep_calm_before_bed',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_calm_before_bed.mp3?alt=media&token=70fc5a3e-e5d9-41d5-bea4-12dc78802d5a',           0),
-- EMOTIONAL BALANCE (6)
('emotional_mood_reset',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_mood_reset.mp3?alt=media&token=8b08f7f3-3f2d-445a-ab4b-863f315d1929',        0),
('emotional_release',          'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_release.mp3?alt=media&token=44c71a73-8883-4a48-96f1-752105a04e1a',          0),
('emotional_inner_balance',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_inner_balance.mp3?alt=media&token=558e8461-c1b9-43a2-9859-134bf33d6d2c',             0),
('emotional_gentle_healing',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_gentle_healing.mp3?alt=media&token=6989f0a6-26d3-45d0-820e-42309872026a',            0),
('emotional_self_compassion',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_self_compassion.mp3?alt=media&token=cc8f37c9-0649-45ae-9cfe-9975375ddd37',            0),
('emotional_clarity',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_clarity.mp3?alt=media&token=6ee4e30c-18dc-4789-ab62-08116c4cba4b',        0),
-- CONFIDENCE (6)
('confidence_build',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_build.mp3?alt=media&token=bc968bad-8fa0-41a5-aa17-cbeac00ed047',            0),
('confidence_inner_strength',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_inner_strength.mp3?alt=media&token=ad72a723-7615-46c2-ab8d-f1f4702c01b5',        0),
('confidence_believe',  'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_believe.mp3?alt=media&token=942e7fbb-3f66-40c9-90b5-520b9789aaf6',  0),
('confidence_calm', 'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_calm.mp3?alt=media&token=53c19018-388a-4050-907d-b4494b37f173', 0),
('confidence_self_trust',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_self_trust.mp3?alt=media&token=803f5376-e500-4580-9df3-6b022776403d',           0),
('confidence_positive_image',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_positive_image.mp3?alt=media&token=3da7f098-6a63-47fb-afe5-2c8786eeaa71',               0),
-- SELF LOVE (6)
('love_kindness',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_kindness.mp3?alt=media&token=9dea5194-d583-45ed-9a72-0a3920968ee7',             0),
('love_be_gentle',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_kindness.mp3?alt=media&token=9dea5194-d583-45ed-9a72-0a3920968ee7',             0),
('love_acceptance',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_acceptance.mp3?alt=media&token=a8a2b7d5-d6be-477f-82f6-0cd0cdb0aecd',               0),
('love_inner_warmth',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_inner_warmth.mp3?alt=media&token=61ac1e80-baa9-407e-bc02-189130e4df30',                0),
('love_compassion_practice',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_compassion_practice.mp3?alt=media&token=3d50ffe3-ca37-45f9-bd7b-434f5a49874b',             0),
('love_heart_opening',              'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_heart_opening.mp3?alt=media&token=74a36bb2-a336-4072-aec3-953f468b03cf',              0)
    ON CONFLICT (topic_id) DO UPDATE SET audio_url = EXCLUDED.audio_url, updated_at = EXCLUDED.updated_at;

-- Seed ambient sound audio URLs.
-- sound_id values must match the id field in the mobile's ambientSounds list in MeditationContent.kt.

INSERT INTO ambient_sound_audio (sound_id, audio_url, updated_at) VALUES
-- NATURE (6)
('nature_rain',          'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_rain.mp3?alt=media&token=0f2626fd-dad3-4b15-8b29-2e8ef37ed13e',          0),
('nature_ocean',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_ocean.mp3?alt=media&token=84b43731-f6eb-4f2b-913a-77fc5e1781cc',         0),
('nature_forest',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_forest.mp3?alt=media&token=fa532217-85e4-409d-be05-f79d0bafd87c',        0),
('nature_river',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_river.mp3?alt=media&token=66a7d707-bc31-445a-a8a6-63c4e0afa034',         0),
('nature_thunder',  'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_thunder.mp3?alt=media&token=538bbc6c-cf16-4940-83fd-58ade74eab1e',  0),
('nature_wind',          'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_wind.mp3?alt=media&token=c305d641-7549-42be-96ad-227d0a1a426f',          0),
-- TRAFFIC (4)
('traffic_cafe',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_cafe.mp3?alt=media&token=5466e168-f150-465f-9772-4a8f674b8921',         0),
('traffic_city',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_city.mp3?alt=media&token=2ea8a081-b493-46de-8210-e45cc67acd32',         0),
('traffic_train',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_train.mp3?alt=media&token=51716b7e-2fdc-4960-a89c-567c01a292a7',        0),
('traffic_airplane',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_airplane.mp3?alt=media&token=2452919d-0cab-4384-9bb0-199ab0822635',      0),
-- SLEEP (4)
('sleep_white_noise',    'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_white_noise.mp3?alt=media&token=edcd0e67-deb1-47c9-a307-3ea78ec9cf99',    0),
('sleep_pink_noise',     'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_pink_noise.mp3?alt=media&token=b3c06102-36b5-4ea5-a533-bb392d776dc0',     0),
('sleep_fireplace',    'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_fireplace.mp3?alt=media&token=dd240173-d8cc-4904-bd58-8a64dde0bc91',    0),
('sleep_fan',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_fireplace.mp3?alt=media&token=dd240173-d8cc-4904-bd58-8a64dde0bc91',            0),
-- ANIMALS (4)
('animals_birds',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_birds.mp3?alt=media&token=260254eb-30b5-4c51-b014-a8bad2e52f44',        0),
('animals_crickets',     'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_crickets.mp3?alt=media&token=223ab5c1-5d42-4a16-990b-5cf1143bc5ed',     0),
('animals_frogs',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_frogs.mp3?alt=media&token=2e812e9e-9877-4652-8ea2-6d9e49d225ab',        0),
('animals_whale',       'https://firebasestorage.googleapis.com/v0/b/tranquilai-bbbc4.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_whale.mp3?alt=media&token=45bdf238-ef64-49e1-88ba-0c1477e59a4f',       0)
    ON CONFLICT (sound_id) DO UPDATE SET audio_url = EXCLUDED.audio_url, updated_at = EXCLUDED.updated_at;
