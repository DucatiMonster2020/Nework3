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
import com.yandex.mapkit.map.InputListener
import com.yandex.runtime.image.ImageProvider
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
    }

    private fun setupMap() {
        val mapView = binding.mapView
        val map = mapView.map
        val targetLocation = Point(55.7558, 37.6176)
        map.move(
            CameraPosition(targetLocation, 10.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )

        // ВАРИАНТ 1: Если есть метод addTapListener (новая версия)
        try {
            map.addTapListener { point ->
                selectedLocation = point
                addMarker(point)
                binding.confirmButton.isVisible = true
                true // возвращаем true, если обработка выполнена
            }
        } catch (e: NoSuchMethodError) {
            // ВАРИАНТ 2: Старая версия с InputListener
            setupMapWithInputListener(map)
        }
    }

    // Метод для старой версии MapKit
    private fun setupMapWithInputListener(map: com.yandex.mapkit.map.Map) {
        val listener = object : com.yandex.mapkit.map.InputListener {
            // Попробуйте разные варианты сигнатур:

            // Вариант A:
            override fun onMapTap(p0: com.yandex.mapkit.map.Map, p1: Point) {
                selectedLocation = p1
                addMarker(p1)
                binding.confirmButton.isVisible = true
            }

            override fun onMapLongTap(p0: com.yandex.mapkit.map.Map, p1: Point) {
                // Не используется
            }

            // ИЛИ Вариант B (если предыдущий не работает):
            // override fun onMapTap(map: com.yandex.mapkit.map.Map, point: Point) {
            //     selectedLocation = point
            //     addMarker(point)
            //     binding.confirmButton.isVisible = true
            // }

            // override fun onMapLongTap(map: com.yandex.mapkit.map.Map, point: Point) {
            //     // Не используется
            // }
        }

        try {
            map.addInputListener(listener)
        } catch (e: Exception) {
            // Если и это не работает, используем альтернативный подход
            setupMapAlternative()
        }
    }

    // Альтернативный подход без InputListener
    private fun setupMapAlternative() {
        // Используем GestureListener или другой подход
        Snackbar.make(
            binding.root,
            "Нажмите на карту для выбора местоположения",
            Snackbar.LENGTH_LONG
        ).show()

        // Можно добавить кнопку "Выбрать текущее местоположение"
    }

    private fun addMarker(point: Point) {
        val mapView = binding.mapView
        val map = mapView.map

        // Очищаем предыдущие маркеры
        map.mapObjects.clear()

        try {
            // Добавляем новый маркер
            val placemark = map.mapObjects.addPlacemark(point)
            placemark.setIcon(
                com.yandex.runtime.image.ImageProvider.fromResource(
                    requireContext(), R.drawable.ic_map_pin
                )
            )
            placemark.opacity = 1.0f

        } catch (e: Exception) {
            // Альтернативный способ добавления маркера
            try {
                val collection = map.mapObjects.addCollection()
                val placemark = collection.addPlacemark(point)
                placemark.setIcon(
                    com.yandex.runtime.image.ImageProvider.fromResource(
                        requireContext(), R.drawable.ic_map_pin
                    )
                )
            } catch (e2: Exception) {
                // Просто показываем координаты
                Snackbar.make(
                    binding.root,
                    "Выбрано: ${point.latitude}, ${point.longitude}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        // Приближаем к выбранной точке
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

                // Возвращаем результат предыдущему фрагменту
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
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        try {
            binding.mapView.map.mapObjects.clear()
        } catch (e: Exception) {
            // Игнорируем ошибку очистки
        }
        super.onDestroyView()
        _binding = null
    }
}