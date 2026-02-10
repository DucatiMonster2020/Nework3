package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentMapBinding
import ru.netology.nework.utils.Constants.LOCATION_LAT
import ru.netology.nework.utils.Constants.LOCATION_LNG
import ru.netology.nework.utils.Constants.LOCATION_REQUEST_KEY

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var selectedLocation: Point? = null

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

        setupToolbar()

        MapKitFactory.setApiKey(BuildConfig.YANDEX_MAPS_API_KEY)
        MapKitFactory.initialize(requireContext())

        setupMap()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.title = "Выбор места"
    }

    private fun setupMap() {
        val mapView = binding.mapView
        val map = mapView.mapWindow.map
        val targetLocation = Point(55.7558, 37.6176)
        map.move(
            CameraPosition(targetLocation, 10.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
        map.addInputListener(object : com.yandex.mapkit.map.InputListener {
            override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
                selectedLocation = point
                addMarker(point)
                binding.confirmButton.isVisible = true
            }

            override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
            }
        })
    }

    private fun addMarker(point: Point) {
        val mapView = binding.mapView
        val map = mapView.mapWindow.map
        map.mapObjects.clear()
        val mapObjects: MapObjectCollection = map.mapObjects
        val placemark = mapObjects.addPlacemark(point)
        placemark.setIcon(
            com.yandex.runtime.image.ImageProvider.fromResource(
                requireContext(), R.drawable.ic_map_pin
            )
        )
        placemark.opacity = 1.0f
        map.move(
            CameraPosition(point, 15.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )
    }

    private fun setupListeners() {
        binding.confirmButton.setOnClickListener {
            selectedLocation?.let { location ->
                val result = Bundle().apply {
                    putDouble(LOCATION_LAT, location.latitude)
                    putDouble(LOCATION_LNG, location.longitude)
                }
                parentFragmentManager.setFragmentResult(
                    LOCATION_REQUEST_KEY,
                    result
                )

                findNavController().popBackStack()
            } ?: run {
                Snackbar.make(binding.root, "Выберите место на карте", Snackbar.LENGTH_SHORT).show()
            }
        }
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
        binding.mapView.mapWindow.map.mapObjects.clear()
        super.onDestroyView()
        _binding = null
    }
}