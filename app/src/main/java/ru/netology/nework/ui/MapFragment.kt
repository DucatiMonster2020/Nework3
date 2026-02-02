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
import com.yandex.mapkit.map.CameraPosition
import com.yandex.runtime.image.ImageProvider
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.BuildConfig
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentMapBinding

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
    }

    private fun setupMap() {
        val mapView = binding.mapView
        val map = mapView.mapWindow.map
        val targetLocation = Point(55.7558, 37.6176)
        map.move(CameraPosition(targetLocation, 10.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
        map.addInputListener(object : MapInputListener {
            override fun onMapTap(map: Map, point: Point) {
                selectedLocation = point
                addMarker(point)
                binding.confirmButton.visibility = View.VISIBLE
            }

            override fun onMapLongTap(map: Map, point: Point) {
                // Не используем
            }
        })
    }

    private fun addMarker(point: Point) {
        val mapView = binding.mapView
        val map = mapView.mapWindow.map
        map.mapObjects.clear()
        val mapObjects = map.mapObjects.addCollection()
        mapObjects.addPlacemark().apply {
            geometry = point
            setIcon(
                ImageProvider.fromResource(requireContext(), R.drawable.ic_map_pin)
            )
            opacity = 1.0f
        }
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
                    putDouble("lat", location.latitude)
                    putDouble("lng", location.longitude)
                }
                parentFragmentManager.setFragmentResult(
                    "location_request_key",
                    result
                )

                findNavController().popBackStack()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
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