// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

import com.diffplug.gradle.spotless.BaseKotlinExtension
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("java")
  id("org.jetbrains.intellij.platform")
  kotlin("jvm") version "2.1.20-RC"
  id("com.diffplug.spotless") version "7.0.2"
  idea
}

group = "in.sungb.intellij.playground"
version = "0.0-preview"

idea {
  module {
    excludeDirs = excludeDirs + setOf(file(".kotlin"))
  }
}

kotlin {
  explicitApi()
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(17)
    vendor = JvmVendorSpec.JETBRAINS
  }
}

tasks.compileKotlin.configure {
  compilerOptions {
    optIn.addAll(
      "kotlin.OptIn",
      "kotlin.RequiresOptIn",
      "kotlin.contracts.ExperimentalContracts",
      "org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction",
    )
  }
}

tasks.test.configure {
  useJUnitPlatform()
  outputs.upToDateWhen { false }
}

spotless {
  fun BaseKotlinExtension.useKtlint() {
    ktlint("1.5.0").editorConfigOverride(
      mapOf(
        "indent_size" to "2",
        "ktlint_standard_filename" to "disabled",
        "ktlint_standard_package-name" to "disabled",
        "ktlint_standard_function-naming" to "disabled",
        "ktlint_standard_property-naming" to "disabled",
        "ktlint_standard_backing-property-naming" to "disabled",
        "ktlint_standard_class-signature" to "disabled",
        "ktlint_standard_function-signature" to "disabled",
        "ktlint_standard_function-expression-body" to "disabled",
        "ktlint_standard_blank-line-before-declaration" to "disabled",
        "ktlint_standard_spacing-between-declarations-with-annotations" to "disabled",
        "ktlint_standard_import-ordering" to "disabled",
        "ktlint_standard_max-line-length" to "disabled",
        "ktlint_standard_annotation" to "disabled",
        "ktlint_standard_multiline-if-else" to "disabled",
        "ktlint_standard_value-argument-comment" to "disabled",
        "ktlint_standard_value-parameter-comment" to "disabled",
        "ktlint_standard_comment-wrapping" to "disabled",
      ),
    )
  }

  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**/*.kt", "spotless/*.kt")
    useKtlint()
    licenseHeaderFile(rootProject.file("spotless/copyright.kt"))
  }
  kotlinGradle {
    target("**/*.kts")
    targetExclude("**/build/**/*.kts", "spotless/*.kts")
    useKtlint()
    licenseHeaderFile(
      rootProject.file("spotless/copyright.kts"),
      "(@file|import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
    )
  }
  format("xml") {
    target("**/*.xml")
    targetExclude("**/build/**/*.xml", "spotless/*.xml", ".idea/**/*.xml", ".intellijPlatform/**/*.xml")
    licenseHeaderFile(rootProject.file("spotless/copyright.xml"), "(<[^!?])")
  }
}

dependencies {
  intellijPlatform {
    intellijIdeaCommunity("2023.3")
    bundledPlugin("org.jetbrains.kotlin")
    instrumentationTools()

    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.Plugin.Java)
    testFramework(TestFrameworkType.JUnit5)
  }

  testImplementation(kotlin("test-junit5"))
  testImplementation(kotlin("reflect")) // Used by assertk
  testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")

  testImplementation("junit:junit:4.13.2") { because("IJPL-159134") }
}
