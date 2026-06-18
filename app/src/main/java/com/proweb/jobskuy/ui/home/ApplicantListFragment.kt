package com.proweb.jobskuy.ui.home

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
import com.proweb.jobskuy.data.Application
import com.proweb.jobskuy.databinding.FragmentApplicantListBinding

class ApplicantListFragment : Fragment(R.layout.fragment_applicant_list) {

    private var _binding: FragmentApplicantListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val applicantList = ArrayList<Application>()
    private lateinit var applicantAdapter: ApplicantAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicantListBinding.bind(view)

        // Konstruksi awal RecyclerView beserta Adapter-nya
        binding.rvApplicantList.layoutManager = LinearLayoutManager(requireContext())
        applicantAdapter = ApplicantAdapter(applicantList)
        binding.rvApplicantList.adapter = applicantAdapter

        listenToIncomingApplicants()
    }

    private fun listenToIncomingApplicants() {
        val currentRecruiterUid = auth.currentUser?.uid ?: return

        // Mengambil data dari koleksi "applications" secara real-time yang ditujukan ke Recruiter ini
        db.collection("applications")
            .whereEqualTo("recruiterUid", currentRecruiterUid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                applicantList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val application = doc.toObject(Application::class.java)
                        applicantList.add(application)
                    }
                }

                // Cek penanganan jika daftar pelamar masih kosong
                if (applicantList.isEmpty()) {
                    binding.tvNoApplicants.visibility = View.VISIBLE
                    binding.rvApplicantList.visibility = View.GONE
                } else {
                    binding.tvNoApplicants.visibility = View.GONE
                    binding.rvApplicantList.visibility = View.VISIBLE
                }

                applicantAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ADAPTER INTERNAL RECYCLERVIEW UNTUK MENGHEMAT FILE KELAS
    class ApplicantAdapter(private val list: List<Application>) :
        RecyclerView.Adapter<ApplicantAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvApplicantName)
            val status: TextView = view.findViewById(R.id.tvApplicantStatus)
            val jobTitle: TextView = view.findViewById(R.id.tvTargetJobTitle)
            val company: TextView = view.findViewById(R.id.tvTargetCompany)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_applicant, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            holder.name.text = data.seekerName
            holder.jobTitle.text = "Melamar posisi: ${data.jobTitle}"
            holder.company.text = "di ${data.companyName}"
            holder.status.text = data.status
        }

        override fun getItemCount(): Int = list.size
    }
}
