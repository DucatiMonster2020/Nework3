package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentMapBinding
import ru.netology.nework.utils.CoordinatesUtils

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var selectedPoint: Point? = null
    private var placemark: PlacemarkMapObject? = null
    private lateinit var mapObjects: MapObjectCollection

    companion object {
        const val LOCATION_REQUEST_KEY = "location_request"
        const val LOCATION_LAT = "lat"
        const val LOCATION_LNG = "lng"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMap()
        setupListeners()
    }

    private fun setupMap() {
        MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)

        val map = binding.mapView.mapWindow.map
        mapObjects = map.mapObjects

        val startPoint = Point(55.751244, 37.618423)

        map.move(
            CameraPosition(startPoint, 10.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
        map.addTapListener(object : GeoObjectTapListener {
            override fun onObjectTap(event: GeoObjectTapEvent): Boolean {
                val point = event.geoObject.geometry.firstOrNull()?.point
                point?.let { setPlacemark(it) }
                return true
            }
        })
    }

    private fun setPlacemark(point: Point) {
        placemark?.let { mapObjects.remove(it) }
        placemark = mapObjects.addPlacemark(point,
            ImageProvider.fromResource(requireContext(), R.drawable.ic_map_pin)
        )

        selectedPoint = point
        updateCoordinatesText(point)
        binding.selectButton.isEnabled = true
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.selectButton.setOnClickListener {
            selectedPoint?.let { point ->
                parentFragmentManager.setFragmentResult(
                    LOCATION_REQUEST_KEY,
                    Bundle().apply {
                        putDouble(LOCATION_LAT, point.latitude)
                        putDouble(LOCATION_LNG, point.longitude)
                    }
                )
                findNavController().popBackStack()
            }
        }
    }

    private fun updateCoordinatesText(point: Point) {
        binding.coordinatesText.text = CoordinatesUtils.formatCoordinates(point)
        binding.coordinatesText.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}