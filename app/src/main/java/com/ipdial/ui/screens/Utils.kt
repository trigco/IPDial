package com.ipdial.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun cleanUri(uri: String): String =
    uri.removePrefix("sip:").substringBefore("@").substringBefore(";")

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

fun Char.uppercaseCharCompat(): String = this.uppercaseChar().toString()
