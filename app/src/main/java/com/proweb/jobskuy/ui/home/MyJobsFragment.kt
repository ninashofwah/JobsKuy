package com.proweb.jobskuy.ui.home

import com.proweb.jobskuy.data.Job
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentMyJobsBinding

class MyJobsFragment : Fragment(R.layout.fragment_my_jobs) {

    private var _binding: FragmentMyJobsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val jobList = ArrayList<Job>()
    private lateinit var jobAdapter: ComplexJobsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyJobsBinding.bind(view)

        binding.rvMyJobs.layoutManager = LinearLayoutManager(requireContext())
        jobAdapter = ComplexJobsAdapter(jobList)
        binding.rvMyJobs.adapter = jobAdapter

        fetchMyJobsFromFirestore()
    }

    private fun fetchMyJobsFromFirestore() {
        val currentRecruiterUid = auth.currentUser?.uid ?: return

        db.collection("jobs")
            .whereEqualTo("recruiterUid", currentRecruiterUid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                jobList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val job = doc.toObject(Job::class.java)
                        jobList.add(job)
                    }
                }

                if (jobList.isEmpty()) {
                    binding.tvNoData.visibility = View.VISIBLE
                    binding.rvMyJobs.visibility = View.GONE
                } else {
                    binding.tvNoData.visibility = View.GONE
                    binding.rvMyJobs.visibility = View.VISIBLE
                }
                jobAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ComplexJobsAdapter(private val list: List<Job>) :
        RecyclerView.Adapter<ComplexJobsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val company: TextView = view.findViewById(R.id.tvItemCompany)
            val title: TextView = view.findViewById(R.id.tvItemJobTitle)
            val desc: TextView = view.findViewById(R.id.tvItemJobDescription)
            val badgeApplicants: TextView = view.findViewById(R.id.tvItemApplicantsBadge)
            val badgeSalary: TextView = view.findViewById(R.id.tvItemSalaryBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job_recruiter, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            holder.company.text = data.companyName
            holder.title.text = data.jobTitle
            holder.desc.text = data.jobDescription
            holder.badgeApplicants.text = "Pelamar: ${data.currentApplicants}/${data.maxApplicants}"
            holder.badgeSalary.text = "Gaji: ${data.salary}"
        }

        override fun getItemCount(): Int = list.size
    }
}
