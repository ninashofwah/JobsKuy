package com.proweb.jobskuy.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.proweb.jobskuy.databinding.FragmentHomeRecruiterBinding

class HomeRecruiterFragment : Fragment(R.layout.fragment_home_recruiter) {

    private var _binding: FragmentHomeRecruiterBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val incomingApplicants = ArrayList<HashMap<String, Any>>()
    private lateinit var homeApplicantAdapter: HomeApplicantAdapter
    private var listenerRef: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeRecruiterBinding.bind(view)

        binding.rvRecruiterJobList.layoutManager = LinearLayoutManager(requireContext())

        homeApplicantAdapter = HomeApplicantAdapter(incomingApplicants) { appData ->
            // PERBAIKAN DI SINI: Menggunakan action ID dari Beranda Recruiter langsung ke Detail Pelamar
            val bundle = Bundle().apply {
                putString("APPLICATION_ID", appData["applicationId"] as? String)
            }
            findNavController().navigate(R.id.action_homeRecruiter_to_applicantDetail, bundle)
        }
        binding.rvRecruiterJobList.adapter = homeApplicantAdapter

        binding.btnCreateJobNow.setOnClickListener {
            findNavController().navigate(R.id.action_homeRecruiter_to_createJob)
        }

        binding.btnNavToRecruiterProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeRecruiter_to_profile)
        }

        listenToIncomingApplicants()
    }

    private fun listenToIncomingApplicants() {
        val currentUid = auth.currentUser?.uid ?: return

        listenerRef = db.collection("applications")
            .whereEqualTo("recruiterUid", currentUid)
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener
                incomingApplicants.clear()

                if (snapshots != null) {
                    for (doc in snapshots) {
                        val data = doc.data as HashMap<String, Any>
                        incomingApplicants.add(data)
                    }
                }
                homeApplicantAdapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        listenerRef?.remove()
        super.onDestroyView()
        _binding = null
    }

    private class HomeApplicantAdapter(
        private val list: List<HashMap<String, Any>>,
        private val onClick: (HashMap<String, Any>) -> Unit
    ) : RecyclerView.Adapter<HomeApplicantAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val photo: ImageView = view.findViewById(R.id.ivItemApplicantPhoto)
            val name: TextView = view.findViewById(R.id.tvItemApplicantName)
            val email: TextView = view.findViewById(R.id.tvItemApplicantEmail)
            val status: TextView = view.findViewById(R.id.tvItemApplicantStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_applicant, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = list[position]
            val statusStr = data["status"] as? String ?: "Diproses"
            val seekerUid = data["seekerUid"] as? String ?: ""

            holder.email.text = data["seekerEmail"] as? String ?: "-"
            holder.status.text = "• $statusStr"
            holder.name.text = "Memuat nama..."
            holder.photo.setImageResource(android.R.drawable.sym_def_app_icon)

            val context = holder.itemView.context
            when (statusStr) {
                "Wawancara" -> holder.status.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                "Ditolak" -> holder.status.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                else -> holder.status.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            }

            if (seekerUid.isNotEmpty()) {
                FirebaseFirestore.getInstance().collection("users").document(seekerUid).get()
                    .addOnSuccessListener { userDoc ->
                        if (userDoc.exists()) {
                            holder.name.text = userDoc.getString("name") ?: "Pencari Kerja"

                            val photoBase64 = userDoc.getString("profilePhoto") ?: ""
                            if (photoBase64.isNotEmpty()) {
                                try {
                                    val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    holder.photo.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
            }

            holder.itemView.setOnClickListener { onClick(data) }
        }

        override fun getItemCount() = list.size
    }
}