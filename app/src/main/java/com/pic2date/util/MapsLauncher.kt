package com.pic2date.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Opens turn-by-turn navigation to a free-text address, preferring Google Maps. */
object MapsLauncher {

    fun navigate(context: Context, location: String) {
        val query = Uri.encode(location)

        // Preferred: Google Maps navigation.
        val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$query"))
            .setPackage("com.google.android.apps.maps")
        if (navIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(navIntent)
            return
        }

        // Fallback: any maps app via a geo: query.
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
        if (geoIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(geoIntent)
            return
        }

        Toast.makeText(context, "No maps app found", Toast.LENGTH_SHORT).show()
    }
}
