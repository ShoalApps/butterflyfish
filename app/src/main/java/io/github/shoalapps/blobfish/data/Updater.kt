package io.github.shoalapps.blobfish.data

import io.github.shoalapps.common.update.GitHubUpdater

/** Keeps Butterflyfish up to date from its GitHub Releases, with the shared Shoal library's updater. */
object Updater : GitHubUpdater("https://github.com/ShoalApps/butterflyfish", "Butterflyfish.apk")
