package ru.netology.nework.utils

import com.yandex.mapkit.geometry.Point
import ru.netology.nework.dto.Coordinates

object CoordinatesUtils {

    fun Coordinates.toPoint(): Point = Point(lat, long)

    fun Point.toCoordinates(): Coordinates = Coordinates(latitude, longitude)

    fun formatCoordinates(point: Point): String =
        String.format("%.6f, %.6f", point.latitude, point.longitude)

    fun formatCoordinates(coords: Coordinates): String =
        String.format("%.6f, %.6f", coords.lat, coords.long)
}