package com.sneha.safeherapp.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sneha.safeherapp.model.SafetyZone

enum class ChildStatus(val label: String, val color: Color, val icon: ImageVector) {
    SAFE("Safe", Color(0xFF4CAF50), Icons.Default.CheckCircle),
    DANGER("In Danger", Color(0xFFF44336), Icons.Default.Warning),
    INSIDE_SAFE_ZONE("Inside Safe Zone", Color(0xFF4CAF50), Icons.Default.GppGood),
    OUTSIDE_SAFE_ZONE("Outside Safe Zone", Color(0xFFFF9800), Icons.Default.Info),
    LOCATION_UNAVAILABLE("Location Unavailable", Color(0xFF9E9E9E), Icons.Default.LocationOff),
    PENDING("Pending Connection", Color(0xFFFF9800), Icons.Default.HourglassEmpty)
}

object StatusUtils {
    fun calculateStatus(child: Child?, zones: List<SafetyZone>): ChildStatus {
        if (child == null) return ChildStatus.LOCATION_UNAVAILABLE
        if (child.status == "Pending Connection") return ChildStatus.PENDING
        if (!child.isSafe) return ChildStatus.DANGER
        
        // If location is not available (0.0, 0.0), return Location Unavailable
        if (child.latitude == 0.0 && child.longitude == 0.0) return ChildStatus.LOCATION_UNAVAILABLE
        
        // Filter zones for this specific child (relevant for Guardian Portal view)
        val childZones = zones.filter { it.childId == child.id }
        
        val isInsideSafeZone = childZones.filter { it.zoneType == "Safe" }.any { zone ->
            val results = FloatArray(1)
            try {
                android.location.Location.distanceBetween(child.latitude, child.longitude, zone.latitude, zone.longitude, results)
                results[0] <= zone.radius
            } catch (e: Exception) {
                false
            }
        }
        
        return if (isInsideSafeZone) ChildStatus.INSIDE_SAFE_ZONE else ChildStatus.OUTSIDE_SAFE_ZONE
    }
}
