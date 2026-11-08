package io.github.shoalapps.blobfish

import androidx.compose.runtime.Composable
import io.github.shoalapps.common.ui.WhatsNew as Shared

/** Butterflyfish's What's New, shown by the shared Shoal library from this app's whats-new.json. */
object WhatsNew {
    fun open() = Shared.open()

    @Composable
    fun Host() = Shared.Host(
        title = "What's new in Butterflyfish",
        fallbackTitle = "A fresh catch",
        fallbackBody = "Thanks for using Butterflyfish. This build includes the latest improvements from the Shoal community.",
        communityText = "By the community, for the community.",
        confirmText = "Let's dive in",
    )
}
