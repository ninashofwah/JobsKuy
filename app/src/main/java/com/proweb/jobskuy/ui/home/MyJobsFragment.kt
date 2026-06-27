package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentMyJobsBinding

class MyJobsFragment : Fragment(R.layout.fragment_my_jobs) {

    private var _binding: FragmentMyJobsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val myJobList = ArrayList<Job>()
    private lateinit var myAdapter: MyJobsAdapter

    // Wadah pendaftaran pemutus sirkuit listener realtime
    private var snapshotListenerRegistration: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyJobsBinding.bind(view)

        binding.rvMyJobs.layoutManager = LinearLayoutManager(requireContext())
        myAdapter = MyJobsAdapter(myJobList) { clickedJob ->
            val bundle = Bundle().apply { putString("JOB_ID", clickedJob.jobId) }
            findNavController().navigate(R.id.action_myJobs_to_myJobDetail, bundle)
        }
        binding.rvMyJobs.adapter = myAdapter

        loadMyPostedJobs()
    }

    private fun loadMyPostedJobs() {
        val currentUid = auth.currentUser?.uid ?: return

        // Menyimpan status listener ke dalam variabel registrasi
        snapshotListenerRegistration = db.collection("jobs")
            .whereEqualTo("recruiterUid", currentUid)
            .addSnapshotListener { snapshots, _ ->
                // Mencegah crash crash jika UI fragment terlanjur dihancurkan ditengah jalan
                if (_binding == null) return@addSnapshotListener

                myJobList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        myJobList.add(doc.toObject(Job::class.java))
                    }
                }
                myAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        // MUTLAK: Putus sambungan pemantauan Firestore agar memori HP bersih dan anti keluar sendiri
        snapshotListenerRegistration?.remove()
        super.onDestroyView()
        _binding = null
    }

    private class MyJobsAdapter(
        private val list: List<Job>,
        private val onClick: (Job) -> Unit
    ) : RecyclerView.Adapter<MyJobsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvItemJobTitle)
            val company: TextView = view.findViewById(R.id.tvItemCompany)
            val salary: TextView = view.findViewById(R.id.tvItemSalaryBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job_recruiter, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val job = list[position]
            holder.title.text = job.jobTitle
            holder.company.text = job.companyName
            holder.salary.text = "Gaji: ${job.salary}"
            holder.itemView.setOnClickListener { onClick(job) }
        }

        override fun getItemCount() = list.size
    }
}
