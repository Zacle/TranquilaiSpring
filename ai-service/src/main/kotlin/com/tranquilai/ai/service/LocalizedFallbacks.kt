package com.tranquilai.ai.service

object LocalizedFallbacks {

    fun chat(languageCode: String): String = localized(languageCode, chat)

    fun wellnessSessionTitle(languageCode: String): String = localized(languageCode, wellnessSessionTitle)

    fun greeting(firstName: String, languageCode: String): String {
        val name = firstName.takeIf { it.isNotBlank() && !it.equals("there", ignoreCase = true) }
        return when (normalized(languageCode)) {
            "ar" -> if (name != null) "مرحبًا، $name. كيف تشعر اليوم؟" else "مرحبًا. كيف تشعر اليوم؟"
            "de" -> if (name != null) "Hallo, $name. Wie fühlst du dich heute?" else "Hallo. Wie fühlst du dich heute?"
            "es" -> if (name != null) "Hola, $name. ¿Cómo te sientes hoy?" else "Hola. ¿Cómo te sientes hoy?"
            "fr" -> if (name != null) "Bonjour, $name. Comment te sens-tu aujourd’hui ?" else "Bonjour. Comment te sens-tu aujourd’hui ?"
            "it" -> if (name != null) "Ciao, $name. Come ti senti oggi?" else "Ciao. Come ti senti oggi?"
            "ja" -> if (name != null) "${name}さん、こんにちは。今日はどんな気持ちですか？" else "こんにちは。今日はどんな気持ちですか？"
            "ko" -> if (name != null) "${name}님, 안녕하세요. 오늘 기분은 어떠세요?" else "안녕하세요. 오늘 기분은 어떠세요?"
            "hi" -> if (name != null) "नमस्ते, $name। आज आप कैसा महसूस कर रहे हैं?" else "नमस्ते। आज आप कैसा महसूस कर रहे हैं?"
            "pt" -> if (name != null) "Olá, $name. Como você está se sentindo hoje?" else "Olá. Como você está se sentindo hoje?"
            "ru" -> if (name != null) "Здравствуйте, $name. Как вы себя сегодня чувствуете?" else "Здравствуйте. Как вы себя сегодня чувствуете?"
            "zh" -> if (name != null) "$name，你好。今天感觉怎么样？" else "你好。今天感觉怎么样？"
            else -> if (name != null) "Hello, $name. How are you feeling today?" else "Hello. How are you feeling today?"
        }
    }

    fun moodInsight(score: Int, languageCode: String): String {
        val group = when {
            score <= 3 -> lowMoodInsight
            score <= 6 -> mediumMoodInsight
            else -> highMoodInsight
        }
        return localized(languageCode, group)
    }

    fun affirmation(languageCode: String): String = localized(languageCode, affirmation)

    fun journalSummary(content: String, languageCode: String): JournalFallbackText {
        val preview = content.take(100).let { if (content.length > 100) "$it..." else it }
        return when (normalized(languageCode)) {
            "ar" -> JournalFallbackText(
                summary = "تأملت في أفكارك ومشاعرك: \"$preview\"",
                keyTheme = "التأمل الذاتي",
                emotionalTone = "معالجة",
                suggestedFollowUp = "ما الذي تود استكشافه أكثر مما كتبته؟",
            )
            "de" -> JournalFallbackText(
                summary = "Du hast über deine Gedanken und Gefühle nachgedacht: \"$preview\"",
                keyTheme = "Selbstreflexion",
                emotionalTone = "verarbeitend",
                suggestedFollowUp = "Was möchtest du an dem, was du geschrieben hast, weiter erkunden?",
            )
            "es" -> JournalFallbackText(
                summary = "Reflexionaste sobre tus pensamientos y sentimientos: \"$preview\"",
                keyTheme = "autorreflexión",
                emotionalTone = "procesando",
                suggestedFollowUp = "¿Qué te gustaría explorar más sobre lo que escribiste?",
            )
            "fr" -> JournalFallbackText(
                summary = "Vous avez réfléchi à vos pensées et à vos émotions : \"$preview\"",
                keyTheme = "autoréflexion",
                emotionalTone = "en cours d’intégration",
                suggestedFollowUp = "Qu’aimeriez-vous explorer davantage dans ce que vous avez écrit ?",
            )
            "it" -> JournalFallbackText(
                summary = "Hai riflettuto sui tuoi pensieri e sentimenti: \"$preview\"",
                keyTheme = "autoriflessione",
                emotionalTone = "elaborazione",
                suggestedFollowUp = "Cosa vorresti esplorare ancora di ciò che hai scritto?",
            )
            "ja" -> JournalFallbackText(
                summary = "あなたは自分の考えや気持ちについて振り返りました: \"$preview\"",
                keyTheme = "自己省察",
                emotionalTone = "整理中",
                suggestedFollowUp = "書いた内容について、さらに掘り下げたいことはありますか？",
            )
            "ko" -> JournalFallbackText(
                summary = "당신은 생각과 감정을 돌아보았습니다: \"$preview\"",
                keyTheme = "자기 성찰",
                emotionalTone = "정리 중",
                suggestedFollowUp = "쓴 내용 중 더 살펴보고 싶은 부분이 있나요?",
            )
            "hi" -> JournalFallbackText(
                summary = "आपने अपने विचारों और भावनाओं पर मनन किया: \"$preview\"",
                keyTheme = "आत्म-चिंतन",
                emotionalTone = "समझने की प्रक्रिया",
                suggestedFollowUp = "आपने जो लिखा है, उसमें से किस बात को और समझना चाहेंगे?",
            )
            "pt" -> JournalFallbackText(
                summary = "Você refletiu sobre seus pensamentos e sentimentos: \"$preview\"",
                keyTheme = "autorreflexão",
                emotionalTone = "processando",
                suggestedFollowUp = "O que você gostaria de explorar mais sobre o que escreveu?",
            )
            "ru" -> JournalFallbackText(
                summary = "Вы поразмышляли о своих мыслях и чувствах: \"$preview\"",
                keyTheme = "саморефлексия",
                emotionalTone = "осмысление",
                suggestedFollowUp = "Что из написанного вы хотели бы исследовать глубже?",
            )
            "zh" -> JournalFallbackText(
                summary = "你回顾了自己的想法和感受：\"$preview\"",
                keyTheme = "自我反思",
                emotionalTone = "整理中",
                suggestedFollowUp = "关于你写下的内容，还想进一步探索什么？",
            )
            else -> JournalFallbackText(
                summary = "You reflected on your thoughts and feelings: \"$preview\"",
                keyTheme = "self-reflection",
                emotionalTone = "processing",
                suggestedFollowUp = "What would you like to explore further about what you wrote?",
            )
        }
    }

    private fun localized(languageCode: String, values: Map<String, String>): String =
        values[normalized(languageCode)] ?: values.getValue("en")

    private fun normalized(languageCode: String): String =
        languageCode.lowercase().substringBefore('-').substringBefore('_')

    private val chat = mapOf(
        "en" to "I'm here with you. Take your time.",
        "ar" to "أنا هنا معك. خذ وقتك.",
        "de" to "Ich bin hier bei dir. Nimm dir Zeit.",
        "es" to "Estoy aquí contigo. Tómate tu tiempo.",
        "fr" to "Je suis là avec vous. Prenez votre temps.",
        "it" to "Sono qui con te. Prenditi il tempo che ti serve.",
        "ja" to "ここにいます。ゆっくりで大丈夫です。",
        "ko" to "제가 함께 있어요. 천천히 해도 괜찮아요.",
        "hi" to "मैं आपके साथ हूँ। अपना समय लें।",
        "pt" to "Estou aqui com você. Leve o tempo que precisar.",
        "ru" to "Я рядом с вами. Не торопитесь.",
        "zh" to "我在这里陪着你。慢慢来。",
    )

    private val wellnessSessionTitle = mapOf(
        "en" to "Wellness Session",
        "ar" to "جلسة عافية",
        "de" to "Wohlfühlsitzung",
        "es" to "Sesión de bienestar",
        "fr" to "Séance de bien-être",
        "it" to "Sessione di benessere",
        "ja" to "ウェルネスセッション",
        "ko" to "웰니스 세션",
        "hi" to "वेलनेस सत्र",
        "pt" to "Sessão de bem-estar",
        "ru" to "Сеанс благополучия",
        "zh" to "身心健康会话",
    )

    private val lowMoodInsight = mapOf(
        "en" to "It sounds like today has been tough, and that is completely okay. Be gentle with yourself and try a few slow breaths to steady this moment.",
        "ar" to "يبدو أن اليوم كان صعبًا، وهذا مفهوم تمامًا. كن لطيفًا مع نفسك وجرب بعض الأنفاس البطيئة لتهدئة هذه اللحظة.",
        "de" to "Es klingt, als wäre der heutige Tag schwer gewesen, und das ist völlig in Ordnung. Sei sanft mit dir und versuche ein paar langsame Atemzüge.",
        "es" to "Parece que hoy ha sido difícil, y está bien sentirse así. Sé amable contigo y prueba unas respiraciones lentas para estabilizar este momento.",
        "fr" to "On dirait que la journée a été difficile, et c’est tout à fait compréhensible. Soyez doux avec vous-même et essayez quelques respirations lentes.",
        "it" to "Sembra che oggi sia stata una giornata difficile, ed è del tutto comprensibile. Sii gentile con te stesso e prova qualche respiro lento.",
        "ja" to "今日はつらい一日だったようですね。そう感じても大丈夫です。自分にやさしく、ゆっくり数回呼吸してみましょう。",
        "ko" to "오늘은 힘든 하루였던 것 같아요. 그렇게 느끼는 건 괜찮습니다. 자신에게 다정하게 대해 주고 천천히 몇 번 숨을 쉬어 보세요.",
        "hi" to "लगता है आज का दिन कठिन रहा, और ऐसा महसूस करना ठीक है। अपने साथ नरमी रखें और कुछ धीमी साँसें लेकर इस पल में ठहरें।",
        "pt" to "Parece que hoje foi difícil, e tudo bem se sentir assim. Seja gentil consigo e tente algumas respirações lentas para se firmar no momento.",
        "ru" to "Похоже, день был тяжелым, и это совершенно нормально. Отнеситесь к себе мягко и сделайте несколько медленных вдохов.",
        "zh" to "听起来今天很不容易，这样的感受是可以被理解的。请温柔对待自己，试着慢慢呼吸几次，让这一刻稳定下来。",
    )

    private val mediumMoodInsight = mapOf(
        "en" to "You are moving through a mixed day, and that takes energy. Notice one small thing that has offered comfort or steadiness today.",
        "ar" to "أنت تمر بيوم مختلط، وهذا يحتاج إلى طاقة. لاحظ شيئًا صغيرًا منحك بعض الراحة أو الثبات اليوم.",
        "de" to "Du bewegst dich durch einen gemischten Tag, und das kostet Energie. Nimm eine kleine Sache wahr, die dir heute Trost oder Halt gegeben hat.",
        "es" to "Estás atravesando un día mixto, y eso requiere energía. Nota algo pequeño que hoy te haya dado consuelo o estabilidad.",
        "fr" to "Vous traversez une journée contrastée, et cela demande de l’énergie. Remarquez une petite chose qui vous a apporté du réconfort ou de la stabilité aujourd’hui.",
        "it" to "Stai attraversando una giornata mista, e questo richiede energia. Nota una piccola cosa che oggi ti ha dato conforto o stabilità.",
        "ja" to "気持ちが揺れる一日を過ごしているのですね。それにはエネルギーが必要です。今日少しでも安心につながったことを一つ見つけてみましょう。",
        "ko" to "여러 감정이 섞인 하루를 지나고 있네요. 그것도 에너지가 필요한 일입니다. 오늘 작은 위로나 안정감을 준 것을 하나 떠올려 보세요.",
        "hi" to "आप मिला-जुला दिन पार कर रहे हैं, और इसमें ऊर्जा लगती है। आज किस छोटी चीज़ ने आपको थोड़ा आराम या स्थिरता दी, उसे नोटिस करें।",
        "pt" to "Você está atravessando um dia misto, e isso exige energia. Perceba uma pequena coisa que trouxe conforto ou estabilidade hoje.",
        "ru" to "Вы проходите через неоднозначный день, и на это нужны силы. Заметьте одну маленькую вещь, которая сегодня дала вам утешение или устойчивость.",
        "zh" to "你正在经历一个感受复杂的日子，这本身就需要能量。留意今天给你一点安慰或稳定感的小事。",
    )

    private val highMoodInsight = mapOf(
        "en" to "There is some positive energy here, and it is worth noticing. Keep nurturing the habits and people that support your wellbeing.",
        "ar" to "هناك طاقة إيجابية هنا، ومن المهم ملاحظتها. استمر في رعاية العادات والأشخاص الذين يدعمون عافيتك.",
        "de" to "Hier ist positive Energie spürbar, und es lohnt sich, sie wahrzunehmen. Pflege weiter die Gewohnheiten und Menschen, die deinem Wohlbefinden guttun.",
        "es" to "Hay una energía positiva aquí, y vale la pena notarla. Sigue cuidando los hábitos y las personas que apoyan tu bienestar.",
        "fr" to "Il y a une énergie positive ici, et elle mérite d’être remarquée. Continuez à nourrir les habitudes et les relations qui soutiennent votre bien-être.",
        "it" to "C’è un’energia positiva qui, e merita di essere notata. Continua a coltivare le abitudini e le persone che sostengono il tuo benessere.",
        "ja" to "ここには前向きなエネルギーがあります。それに気づくことには意味があります。あなたを支える習慣や人とのつながりを大切にしてください。",
        "ko" to "긍정적인 에너지가 느껴지고, 그것을 알아차리는 건 의미가 있습니다. 당신의 안녕을 돕는 습관과 사람들을 계속 돌봐 주세요.",
        "hi" to "यहाँ कुछ सकारात्मक ऊर्जा है, और उसे नोटिस करना मायने रखता है। उन आदतों और लोगों को सँभालते रहें जो आपकी भलाई का साथ देते हैं।",
        "pt" to "Há uma energia positiva aqui, e vale a pena percebê-la. Continue nutrindo os hábitos e as pessoas que apoiam seu bem-estar.",
        "ru" to "Здесь чувствуется положительная энергия, и ее стоит заметить. Продолжайте поддерживать привычки и людей, которые помогают вашему благополучию.",
        "zh" to "这里有一些积极的能量，值得被看见。继续滋养那些支持你身心健康的习惯和关系。",
    )

    private val affirmation = mapOf(
        "en" to "I am worthy of peace and joy.",
        "ar" to "أنا أستحق السلام والفرح.",
        "de" to "Ich bin Frieden und Freude wert.",
        "es" to "Merezco paz y alegría.",
        "fr" to "Je mérite la paix et la joie.",
        "it" to "Merito pace e gioia.",
        "ja" to "私は安らぎと喜びに値します。",
        "ko" to "나는 평온과 기쁨을 누릴 자격이 있습니다.",
        "hi" to "मैं शांति और आनंद के योग्य हूँ।",
        "pt" to "Eu mereço paz e alegria.",
        "ru" to "Я достоин спокойствия и радости.",
        "zh" to "我值得拥有平静与喜悦。",
    )
}

data class JournalFallbackText(
    val summary: String,
    val keyTheme: String,
    val emotionalTone: String,
    val suggestedFollowUp: String,
)
