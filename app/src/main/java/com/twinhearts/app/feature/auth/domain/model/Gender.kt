package com.twinhearts.app.feature.auth.domain.model

/** Chosen at sign up ("I am: Girl / Boy"). [apiValue] is what the Session stores. */
enum class Gender(val apiValue: String) {
    Female("female"),
    Male("male"),
}
