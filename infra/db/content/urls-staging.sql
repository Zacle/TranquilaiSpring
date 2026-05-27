-- Seed meditation audio URLs.
-- Replace placeholder URLs with real CDN URLs after uploading files to Firebase Storage / S3.
-- topic_id values must match the id field in the mobile's MeditationContent.kt.

INSERT INTO meditation_audio (topic_id, audio_url, updated_at) VALUES
-- MINDFULNESS (7)
('mindfulness_calm_focus',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_calm_focus.mp3?alt=media&token=a1191882-6086-4ec5-9029-26fb6b9ea94a',           0),
('mindfulness_serene_breath',       'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_serene_breath.mp3?alt=media&token=bbd46154-d467-43f7-b4d9-108b70b47d23',       0),
('mindfulness_present_moment',     'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_present_moment.mp3?alt=media&token=5b7e1077-7acc-473d-806f-ec878ec3dbdb',     0),
('mindfulness_body_awareness',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_body_awareness.mp3?alt=media&token=c41cb4de-9637-4e99-8b6a-771f1e535f83',      0),
('mindfulness_listening',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_listening.mp3?alt=media&token=a57a3a89-b724-4ba2-9390-023965d9d4e9',             0),
('mindfulness_awareness_scan',       'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fmindfulness%2Fmindfulness_awareness_scan.mp3?alt=media&token=0d392130-fd01-4be8-bb74-159433e4893f',       0),
-- STRESS REDUCTION (6)
('stress_relief',                 'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_relief.mp3?alt=media&token=80a99577-9e26-4507-b4c6-ad7bc33842b6',                 0),
('stress_deep_relaxation',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_deep_relaxation.mp3?alt=media&token=bbb5eea2-00d7-479e-95c9-5ef81f4cb9fc',        0),
('stress_letting_go',                   'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_letting_go.mp3?alt=media&token=637f2acd-2aca-46da-b7a3-913c96e54c44',                   0),
('stress_release_tension',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_release_tension.mp3?alt=media&token=5f59c7b0-f2f5-4b17-b37d-5188050aeaf5',               0),
('stress_gentle_unwind',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_gentle_unwind.mp3?alt=media&token=932c4a3c-155d-4b4d-9047-561cfa302328',      0),
('stress_end_of_day',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fstress%20reduction%2Fstress_end_of_day.mp3?alt=media&token=81116f91-25f2-4304-84f4-da0da2e257e7',           0),
-- ANXIETY RELIEF (6)
('anxiety_ease',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_ease.mp3?alt=media&token=5e6e2e02-4a2e-41c9-afc7-cd521ead399a',                0),
('anxiety_grounding_breath',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_grounding_breath.mp3?alt=media&token=1c9a5bbd-256c-4dc7-9e79-19fc4e545568',               0),
('anxiety_safe_space',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_safe_space.mp3?alt=media&token=9e1095fc-dfe9-4972-ba78-9fff66f02007',      0),
('anxiety_quiet_mind',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_quiet_mind.mp3?alt=media&token=9ef6187b-948f-4fa0-be51-a0297d003c3f',            0),
('anxiety_reset',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_reset.mp3?alt=media&token=794ec4d6-3921-43a9-92ae-292040c13bfd',               0),
('anxiety_panic_calm',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fanxiety%20relief%2Fanxiety_panic_calm.mp3?alt=media&token=bc1cda44-768f-478c-adea-fd0383d1c2b2',                0),
-- SLEEP (6)
('sleep_preparation',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_preparation.mp3?alt=media&token=95a5e1d6-475c-4be9-bea1-226609b3bf28',                0),
('sleep_deep_journey',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_deep_journey.mp3?alt=media&token=24dc4863-b4b3-46b6-8358-fd7785629125',            0),
('sleep_mind_rest',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_mind_rest.mp3?alt=media&token=344b2f99-5c73-42c4-bf3e-83e648d373ae',             0),
('sleep_wind_down',                  'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_wind_down.mp3?alt=media&token=41f916aa-5e92-4716-90c5-6841b77e691e',                  0),
('sleep_body_into_sleep',                  'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_body_into_sleep.mp3?alt=media&token=c65519a2-efe9-4ceb-bd76-abcc8ef78ba4',                  0),
('sleep_calm_before_bed',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fsleep%2Fsleep_calm_before_bed.mp3?alt=media&token=d1def1ff-7107-4177-8d50-58fd1ac3526a',           0),
-- EMOTIONAL BALANCE (6)
('emotional_mood_reset',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_mood_reset.mp3?alt=media&token=6343f8e6-410c-4836-b7a3-ad7d5d73df47',        0),
('emotional_release',          'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_release.mp3?alt=media&token=0455831f-e4cb-461d-8f1b-43497b152658',          0),
('emotional_inner_balance',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_inner_balance.mp3?alt=media&token=61f6e7a7-9f6c-43ea-8ca1-e0b28001ca5c',             0),
('emotional_gentle_healing',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_gentle_healing.mp3?alt=media&token=66ae2ad9-f8e9-4f5c-8155-ca47e66ee8d0',            0),
('emotional_self_compassion',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_self_compassion.mp3?alt=media&token=0e32a70d-438e-48e4-9242-5f890739d74b',            0),
('emotional_clarity',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Femotional%20balance%2Femotional_clarity.mp3?alt=media&token=d374599b-eb67-4832-9ac5-e94f7d0a68d5',        0),
-- CONFIDENCE (6)
('confidence_build',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_build.mp3?alt=media&token=c5e25a28-228b-4d88-8104-6f4d4c46c1a6',            0),
('confidence_inner_strength',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_inner_strength.mp3?alt=media&token=48ae4144-a023-4ba8-8270-769e561ba242',        0),
('confidence_believe',  'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_believe.mp3?alt=media&token=0ead0c63-207f-4117-825e-ebf1a586aac5',  0),
('confidence_calm', 'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_calm.mp3?alt=media&token=8f44afb5-ec4e-474b-823c-39af883770c3', 0),
('confidence_self_trust',           'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_self_trust.mp3?alt=media&token=c8c13361-6d7c-4fe6-8437-d86d3d94edc0',           0),
('confidence_positive_image',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fconfidence%2Fconfidence_positive_image.mp3?alt=media&token=5567dbcf-712f-4fdb-a803-77e420fd09cf',               0),
-- SELF LOVE (6)
('love_kindness',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_kindness.mp3?alt=media&token=653c7f43-fe09-4c41-95f3-907f8df36a3e',             0),
('love_be_gentle',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_be_gentle.mp3?alt=media&token=9f12e6cc-6219-4b9d-b364-d3e0bd5cf8e4',             0),
('love_acceptance',               'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_acceptance.mp3?alt=media&token=1a8f8ead-a980-4075-a367-faed0be48ccb',               0),
('love_inner_warmth',                'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_inner_warmth.mp3?alt=media&token=8d544c75-805b-4b05-ba68-106f9d38dc1b',                0),
('love_compassion_practice',             'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_compassion_practice.mp3?alt=media&token=f84b886d-98d5-4aae-bc5f-b3d5da3e8e70',             0),
('love_heart_opening',              'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fmeditation%2Fself-love%2Flove_heart_opening.mp3?alt=media&token=aac02a94-7d86-4fa6-94e5-dcf3bc03761d',              0)
    ON CONFLICT (topic_id) DO UPDATE SET audio_url = EXCLUDED.audio_url, updated_at = EXCLUDED.updated_at;

-- Seed ambient sound audio URLs.
-- sound_id values must match the id field in the mobile's ambientSounds list in MeditationContent.kt.

INSERT INTO ambient_sound_audio (sound_id, audio_url, updated_at) VALUES
-- NATURE (6)
('nature_rain',          'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_rain.mp3?alt=media&token=578d9bda-1aca-4ae0-ad3b-fe39deddb500',          0),
('nature_ocean',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_ocean.mp3?alt=media&token=d92adf15-d38d-460c-b872-766afe55dace',         0),
('nature_forest',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_forest.mp3?alt=media&token=1463a096-e5cb-465a-96c2-97598a0354be',        0),
('nature_river',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_river.mp3?alt=media&token=0f33eb3f-b755-4a45-a9fc-488b07b27e6a',         0),
('nature_thunder',  'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_thunder.mp3?alt=media&token=560c8e0e-66aa-43a8-904b-c226215a4c45',  0),
('nature_wind',          'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fnature%2Fnature_wind.mp3?alt=media&token=d8e94014-81cd-4f5c-9c30-58f8137fbc57',          0),
-- TRAFFIC (4)
('traffic_cafe',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_cafe.mp3?alt=media&token=bc9c9009-d596-49ec-a5f9-9d88548ec548',         0),
('traffic_city',         'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_city.mp3?alt=media&token=7ead7546-d6c7-49cd-ae13-6de96c17e2bf',         0),
('traffic_train',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_train.mp3?alt=media&token=1504e060-4dcf-46c0-8f82-3e91e5ad2855',        0),
('traffic_airplane',      'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Ftraffic%2Ftraffic_airplane.mp3?alt=media&token=c391b8fd-1503-43a8-ba0d-b74ea50fc0ba',      0),
-- SLEEP (4)
('sleep_white_noise',    'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_white_noise.mp3?alt=media&token=32f90aa7-942b-4bcc-802b-32cc035fba64',    0),
('sleep_pink_noise',     'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_pink_noise.mp3?alt=media&token=45c986a7-77bd-4207-9eb4-9759234bd3d7',     0),
('sleep_fireplace',    'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_fireplace.mp3?alt=media&token=c40be604-df2b-450b-9a3d-ebf899f14d11',    0),
('sleep_fan',            'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fsleep%2Fsleep_fan.mp3?alt=media&token=43ca2950-dc29-4eed-ac35-3d838d70325c',            0),
-- ANIMALS (4)
('animals_birds',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_birds.mp3?alt=media&token=e5c1c2ce-e8b9-40e9-b125-5860fb303857',        0),
('animals_crickets',     'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_crickets.mp3?alt=media&token=a7146892-12b3-41e1-af08-ac4f4c46d23b',     0),
('animals_frogs',        'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_frogs.mp3?alt=media&token=a2706547-3b43-463c-9a8b-b704e6e862cb',        0),
('animals_whale',       'https://firebasestorage.googleapis.com/v0/b/tranquilai-staging.firebasestorage.app/o/content%2Fsound%2Fanimals%2Fanimals_whale.mp3?alt=media&token=e8affd44-0c4d-4a11-b471-27126fc15ce0',       0)
    ON CONFLICT (sound_id) DO UPDATE SET audio_url = EXCLUDED.audio_url, updated_at = EXCLUDED.updated_at;
