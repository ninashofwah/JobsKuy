package com.proweb.jobskuy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.proweb.jobskuy.data.Application
import com.proweb.jobskuy.databinding.ItemApplicantCardBinding

class ApplicantAdapter(
    private var applicants: List<Application>,
    private val onActionClick: (Application, String) -> Unit
) : RecyclerView.Adapter<ApplicantAdapter.ApplicantViewHolder>() {

    class ApplicantViewHolder(val binding: ItemApplicantCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val binding = ItemApplicantCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ApplicantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        val app = applicants[position]
        holder.binding.tvSeekerName.text = app.seekerName
        holder.binding.tvAppliedJob.text = "Posisi: ${app.jobTitle}"
        holder.binding.tvCurrentStatus.text = "Status: ${app.status}"

        // Tombol Manajemen Perubahan Alur Pelamar
        holder.binding.btnNextStep.setOnClickListener {
            val nextStatus = when(app.status) {
                "Terkirim" -> "Review Berkas"
                "Review Berkas" -> "Jadwal Interview"
                "Jadwal Interview" -> "Diterima"
                else -> "Selesai"
            }
            onActionClick(app, nextStatus)
        }
    }

    override fun getItemCount(): Int = applicants.size

    fun updateData(newList: List<Application>) {
        this.applicants = newList
        notifyDataSetChanged()
    }
}