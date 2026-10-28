package com.twinhearts.app.feature.meetplanner.domain

/** Why this file exists: the "Popular Destinations" row on Home, same ten cities and order as the web app. */
data class PopularDestination(val name: String, val lat: Double, val lng: Double, val emoji: String)

val PopularDestinations: List<PopularDestination> = listOf(
    PopularDestination("Bengaluru", 12.9716, 77.5946, "🏙️"),
    PopularDestination("Hyderabad", 17.3850, 78.4867, "🕌"),
    PopularDestination("Visakhapatnam", 17.6868, 83.2185, "🌊"),
    PopularDestination("Goa", 15.2993, 74.1240, "🏖️"),
    PopularDestination("Mysuru", 12.2958, 76.6394, "🏰"),
    PopularDestination("Ooty", 11.4102, 76.6950, "🌄"),
    PopularDestination("Coorg", 12.3375, 75.8069, "🌿"),
    PopularDestination("Chennai", 13.0827, 80.2707, "🌅"),
    PopularDestination("Munnar", 10.0889, 77.0595, "🍃"),
    PopularDestination("Pondicherry", 11.9416, 79.8083, "⛱️"),
)
