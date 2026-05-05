package com.devscion.lingualink.data.model

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val deepgramCode: String
)

val SupportedLanguages = listOf(
    Language("en", "English", "English", "en-US"),
    Language("ur", "Urdu", "اردو", "ur"),
    Language("ar", "Arabic", "العربية", "ar"),
    Language("zh", "Chinese", "中文", "zh-CN"),
    Language("fr", "French", "Français", "fr"),
    Language("de", "German", "Deutsch", "de"),
    Language("es", "Spanish", "Español", "es"),
    Language("hi", "Hindi", "हिन्दी", "hi"),
    Language("ja", "Japanese", "日本語", "ja"),
    Language("ko", "Korean", "한국어", "ko"),
    Language("pt", "Portuguese", "Português", "pt"),
    Language("ru", "Russian", "Русский", "ru"),
    Language("tr", "Turkish", "Türkçe", "tr")
)

fun languageByCode(code: String) = SupportedLanguages.find { it.code == code }
    ?: SupportedLanguages.first()

fun deepgramCodeFor(bcp47: String): String = when (bcp47) {
    "en" -> "en-US"
    "zh" -> "zh-CN"
    "pt" -> "pt-BR"
    else -> bcp47
}
