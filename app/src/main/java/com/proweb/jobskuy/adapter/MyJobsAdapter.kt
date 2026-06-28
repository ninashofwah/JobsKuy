package com.proweb.jobskuy.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.ItemMyJobBinding

class MyJobsAdapter(
    private var jobs: List<Job>,
    private val onItemClick: (Job) -> Unit
) : RecyclerView.Adapter<MyJobsAdapter.MyJobsViewHolder>() {

    class MyJobsViewHolder(val binding: ItemMyJobBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyJobsViewHolder {
        val binding = ItemMyJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyJobsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyJobsViewHolder, position: Int) {
        val job = jobs[position]
        holder.binding.tvItemMyJobTitle.text = job.jobTitle
        holder.binding.tvItemMyJobCompany.text = job.companyName
        
        holder.itemView.setOnClickListener { onItemClick(job) }
    }

    override fun getItemCount(): Int = jobs.size

    fun updateData(newJobs: List<Job>) {
        this.jobs = newJobs
        notifyDataSetChanged()
    }
}
