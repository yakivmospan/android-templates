package com.yakivmospan.templates.presentation.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class TextMessage {
    data class DynamicText(val text: String) : TextMessage()
    data class ResourceText(@StringRes val resource:  Int) : TextMessage()
    data class FormattedResourceText(
        val resource: Int,
        val args: List<Any>
    ) : TextMessage()
}

fun TextMessage?.orEmpty(): TextMessage = this ?: TextMessage.DynamicText("")

@Composable
fun TextMessage?.asString(): String? {
    return when (this) {
        null -> null
        is TextMessage.DynamicText -> text
        is TextMessage.ResourceText -> stringResource(resource)
        is TextMessage.FormattedResourceText -> stringResource(resource, *args.toTypedArray())
    }
}

fun String.asTextMessage(): TextMessage {
    return TextMessage.DynamicText(this)
}

fun Int.asTextMessage(): TextMessage {
    return TextMessage.ResourceText(this)
}

fun Int.asTextMessage(vararg args: Any): TextMessage {
    return TextMessage.FormattedResourceText(
        resource = this,
        args = args.toList()
    )
}