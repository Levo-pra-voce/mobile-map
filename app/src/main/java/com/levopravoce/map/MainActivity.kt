package com.levopravoce.map

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.levopravoce.map.ui.theme.MapTheme
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onStop() {
        super.onStop()
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this.baseContext, "", WellKnownTileServer.MapTiler)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            MapTheme {
                val locationPermissions = rememberMultiplePermissionsState(
                    permissions = listOf(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )

                //1. when the app get launched for the first time
                LaunchedEffect(true) {
                    locationPermissions.launchMultiplePermissionRequest()
                }
                val mapView = rememberMapViewWithLifecycle()
                val coroutineScope = rememberCoroutineScope()
                val json = resources.openRawResource(R.raw.style).bufferedReader()
                    .use { it.readText() }
                val context = this.baseContext

                val selectedPoint = remember { mutableStateListOf<LatLng>() }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView },
                    update = { mapViewParam ->
                        coroutineScope.launch {
                            val map = mapViewParam.awaitMap()
                            map.setStyle(Style.Builder().fromJson(json))
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                fusedLocationClient.lastLocation.addOnSuccessListener {
                                    val lastLocation: Location = it
                                    map.cameraPosition = CameraPosition.Builder().target(
                                        LatLng(
                                            lastLocation.latitude,
                                            lastLocation.longitude
                                        )
                                    ).zoom(15.0).build()
                                }
                            }

                            map.setMinZoomPreference(10.0)

//                            map.setMaxZoomPreference(18.0)

                            map.addOnMapClickListener { point ->
                                selectedPoint.add(point)

                                if(selectedPoint.size > 2) {
                                    selectedPoint.removeFirst()
                                }

                                map.addMarker(
                                    MarkerOptions()
                                        .position(point)
                                        .title("Hello world")
                                        .icon(IconFactory.getInstance(context).fromResource(R.drawable.map_point))
                                )

                                true
                            }

                            map.setOnMarkerClickListener { marker ->
                                Toast.makeText(
                                    context,
                                    "Marker clicked: ${marker.title}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                true
                            }
//                            if (googleApiClient.isConnected) {
//                                registerForActivityResult(
//                                    ActivityResultContracts.RequestMultiplePermissions()
//                                ) { permissions ->
//                                    when {
//                                        permissions.getOrDefault(
//                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
//                                            false
//                                        )
//                                        -> {
//                                            val lastLocation: Location =
//                                                LocationServices.FusedLocationApi.getLastLocation(
//                                                    googleApiClient
//                                                )
//                                            map.cameraPosition = CameraPosition.Builder().target(
//                                                LatLng(
//                                                    lastLocation.latitude,
//                                                    lastLocation.longitude
//                                                )
//                                            ).zoom(15.0).build()
//                                        }
//
//                                        permissions.getOrDefault(
//                                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
//                                            false
//                                        ) -> {
//                                            // Only approximate location access granted.
//                                        }
//
//                                        else -> {
//                                            // No location access granted.
//                                        }
//                                    }
//                                }
//                            }
//                            map.setStyle("https://demotiles.maplibre.org/style.json")
                        }
                    }
                )
            }
        }
    }
}