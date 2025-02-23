// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
  id("org.jetbrains.intellij.platform.settings") version "2.2.1"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "IntelliJ-Plugin-Playground"

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

  repositories {
    mavenCentral()
    maven("https://download.jetbrains.com/teamcity-repository/")
    maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public/")

    intellijPlatform {
      defaultRepositories()
    }
  }
}
