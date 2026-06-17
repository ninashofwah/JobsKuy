package com.proweb.jobskuy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.ItemJobCardBinding
import kotlin.math.*

class JobAdapter(
    private var jobs: List<Job>,
    private val myLat: Double,
    private val myLng: Double,
    private val onItemClick: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    class JobViewHolder(val binding: ItemJobCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val binding = ItemJobCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]
        holder.binding.tvTitle.text = job.title
        holder.binding.tvSalary.text = "Gaji: ${job.salary}"

        // Kalkulasi Jarak Geospasial Secara Real-Time
        val distance = calculateDistance(myLat, myLng, job.latitude, job.longitude)
        holder.binding.tvDistance.text = String.format("%.1f Km dari lokasi Anda", distance)

        holder.itemView.setOnClickListener { onItemClick(job) }
    }

    override fun getItemCount(): Int = jobs.size

    fun updateData(newJobs: List<Job>) {
        this.jobs = newJobs
        notifyDataSetChanged()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = sin(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(theta))
        dist = acos(dist)
        dist = Math.toDegrees(dist)
        return dist * 60 * 1.1515 * 1.609344 // Mengubah hitungan mil ke Kilometer (KM)
    }
}