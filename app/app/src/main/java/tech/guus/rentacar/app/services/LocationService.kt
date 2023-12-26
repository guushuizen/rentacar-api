package tech.guus.rentacar.app.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.SnackbarHostState
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import tech.guus.rentacar.app.models.Coordinates
import tech.guus.rentacar.app.models.OpenStreetMapLocationInformation


class LocationService(
    private val activity: ComponentActivity,
    private val httpClient: HttpClient,
) : Service() {

    private var grantedPermissions: Boolean = false

    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this.activity)

    suspend fun getCurrentCoordinates(): Coordinates? = withContext(Dispatchers.IO) {
        return@withContext try {
            val locationTask =
                this@LocationService.fusedLocationProviderClient.getCurrentLocation(
                    CurrentLocationRequest.Builder().build(),
                    null
                )

            val result = Tasks.await(locationTask)
            Coordinates(latitude = result.latitude, longitude = result.longitude)
        } catch (e: SecurityException) {
            return@withContext null
        }
    }

    suspend fun searchAddressByCoordinates(coordinates: Coordinates): OpenStreetMapLocationInformation? {
        // https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=51.8449&lon=5.8428&addressdetails=1&zoom=18&layer=address
        val response = this.httpClient.get("https://nominatim.openstreetmap.org/reverse") {
            parameter("format", "jsonv2")
            parameter("lat", coordinates.latitude)
            parameter("lon", coordinates.longitude)
            parameter("addressdetails", 1)
            parameter("zoom", 18)
            parameter("layer", "address")
        }

        if (response.status != HttpStatusCode.OK)
            return null

        return response.body<OpenStreetMapLocationInformation>()
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}