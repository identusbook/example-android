package com.identusbook.flighttix.ui

import com.identusbook.flighttix.model.Flight

/** Static demo flight catalog — port of iOS `FlightSearch.swift` (§11.1). */
object FlightSearch {
    fun availableFlights(): List<Flight> = listOf(
        Flight(departure = "ATL", arrival = "SCL", price = 500.00),
        Flight(departure = "SFO", arrival = "TYO", price = 800.00),
        Flight(departure = "LAS", arrival = "VIE", price = 700.00)
    )
}
