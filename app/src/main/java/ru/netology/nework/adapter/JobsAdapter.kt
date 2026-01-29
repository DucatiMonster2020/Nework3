package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.databinding.CardJobBinding
import ru.netology.nework.dto.Job

class JobsAdapter(
    private val onItemClickListener: (Job) -> Unit = {}
) : ListAdapter<Job, JobsAdapter.JobViewHolder>(JobDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = CardJobBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JobViewHolder(binding, onItemClickListener)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JobViewHolder(
        private val binding: CardJobBinding,
        private val onItemClickListener: (Job) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(job: Job) {
            binding.apply {
                // По ТЗ: название компании
                companyName.text = job.name

                // По ТЗ: должность
                position.text = job.position

                // По ТЗ: стаж в формате dd MMM yyyy
                period.text = formatPeriod(job.start, job.finish)

                // Ссылка на сайт (не обязательно по ТЗ, но может быть)
                companyLink.isVisible = !job.link.isNullOrEmpty()
                if (!job.link.isNullOrEmpty()) {
                    companyLink.text = job.link
                }

                // Кнопка меню только для своих работ (п.7 ТЗ)
                menuButton.isVisible = job.ownedByMe

                // Клик на карточку
                root.setOnClickListener {
                    onItemClickListener(job)
                }
                if (!job.link.isNullOrEmpty()) {
                    companyLink.setOnClickListener {
                        // Можно открыть в браузере, но не обязательно по ТЗ
                    }
                }

                // Кнопка меню
                menuButton.setOnClickListener {
                    // Для своего профиля (п.7 ТЗ)
                }
            }
        }

        private fun formatPeriod(start: String, finish: String?): String {
            return try {
                val startInstant = java.time.Instant.parse(start)
                val startFormatted = java.time.format.DateTimeFormatter
                    .ofPattern("dd MMM yyyy")
                    .withLocale(java.util.Locale("ru"))
                    .format(startInstant.atZone(java.time.ZoneId.systemDefault()))

                if (finish != null) {
                    val finishInstant = java.time.Instant.parse(finish)
                    val finishFormatted = java.time.format.DateTimeFormatter
                        .ofPattern("dd MMM yyyy")
                        .withLocale(java.util.Locale("ru"))
                        .format(finishInstant.atZone(java.time.ZoneId.systemDefault()))
                    "$startFormatted - $finishFormatted"
                } else {
                    "$startFormatted - настоящее время"
                }
            } catch (e: Exception) {
                // Если не удалось распарсить, показываем как есть
                if (finish != null) {
                    "$start - $finish"
                } else {
                    "$start - настоящее время"
                }
            }
        }
    }
}

class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
    override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem == newItem
    }
}