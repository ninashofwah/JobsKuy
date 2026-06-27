package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentMyApplicationsBinding

class MyApplicationsFragment : Fragment(R.layout.fragment_my_applications) {

    private var _binding: FragmentMyApplicationsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val applicationList = ArrayList<HashMap<String, Any>>()
    private lateinit var listAdapter: ApplicationsAdapter
    private var listenerRef: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyApplicationsBinding.bind(view)

        binding.rvMyApplicationsList.layoutManager = LinearLayoutManager(requireContext())

        listAdapter = ApplicationsAdapter(applicationList) { appData ->
            val bundle = Bundle().apply {
                putString("APPLICATION_ID", appData["applicationId"] as? String)
            }
            findNavController().navigate(R.id.action_myApplications_to_myApplicationDetail, bundle)
        }
        binding.rvMyApplicationsList.adapter = listAdapter

        loadMySubmittedApplications()
    }

    private fun loadMySubmittedApplications() {
        val currentUid = auth.currentUser?.uid ?: return

        listenerRef = db.collection("applications")
            .whereEqualTo("seekerUid", currentUid)
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                applicationList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val data = doc.data as HashMap<String, Any>
                        applicationList.add(data)
                    }
                }
                listAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        listenerRef?.remove()
        super.onDestroyView()
        _binding = null
    }

    private class ApplicationsAdapter(
        private val list: List<HashMap<String, Any>>,
        private val onClick: (HashMap<String, Any>) -> Unit
    ) : RecyclerView.Adapter<ApplicationsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvItemApplicantEmail)
            val status: TextView = view.findViewById(R.id.tvItemApplicantStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_applicant, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            val jobId = data["jobId"] as? String ?: ""
            val statusStr = data["status"] as? String ?: "Diproses"

            holder.title.text = "Memuat rincian..."
            holder.status.text = "Status: $statusStr"

            // SINKRONISASI WARNA STATUS DI LIST ITEM HALAMAN DEPAN
            val context = holder.itemView.context
            when (statusStr) {
                "Wawancara" -> {
                    holder.status.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                }
                "Ditolak" -> {
                    holder.status.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                }
                else -> {
                    holder.status.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
                }
            }

            if (jobId.isNotEmpty()) {
                FirebaseFirestore.getInstance().collection("jobs").document(jobId).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            holder.title.text = doc.getString("jobTitle") ?: "Lowongan Pekerjaan"
                        }
                    }
            }

            holder.itemView.setOnClickListener { onClick(data) }
        }

        override fun getItemCount() = list.size
    }
}