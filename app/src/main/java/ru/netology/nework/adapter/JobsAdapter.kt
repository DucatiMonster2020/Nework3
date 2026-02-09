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
                companyName.text = job.name
                position.text = job.position
                period.text = formatPeriod(job.start, job.finish)
                companyLink.isVisible = !job.link.isNullOrEmpty()
                if (!job.link.isNullOrEmpty()) {
                    companyLink.text = job.link
                }
                menuButton.isVisible = job.ownedByMe
                root.setOnClickListener {
                    onItemClickListener(job)
                }
                if (!job.link.isNullOrEmpty()) {
                    companyLink.setOnClickListener {
                    }
                }
                menuButton.setOnClickListener {
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