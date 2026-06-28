package com.proweb.jobskuy.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Job
import com.proweb.jobskuy.databinding.FragmentHomeSeekerBinding
import java.util.Locale

class HomeSeekerFragment : Fragment(R.layout.fragment_home_seeker) {

    private var _binding: FragmentHomeSeekerBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    // Master data list dari database
    private val allJobList = ArrayList<Job>()
    // Data list khusus yang telah terfilter sesuai kata kunci pencarian
    private val filteredJobList = ArrayList<Job>()

    private lateinit var jobAdapter: SeekerJobAdapter
    private var listenerRef: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeSeekerBinding.bind(view)

        // Setup Recycler View menggunakan list yang bisa difilter
        binding.rvJobList.layoutManager = LinearLayoutManager(requireContext())
        jobAdapter = SeekerJobAdapter(filteredJobList) { job ->
            val bundle = Bundle().apply { putString("JOB_ID", job.jobId) }
            findNavController().navigate(R.id.action_homeSeeker_to_jobDetail, bundle)
        }
        binding.rvJobList.adapter = jobAdapter

        // Navigasi ke Halaman Profil Akun
        binding.btnNavToProfileAccount.setOnClickListener {
            findNavController().navigate(R.id.action_homeSeeker_to_profileAccount)
        }

        // LOGIKA FITUR PENCARIAN (SEARCH FILTER) REAL-TIME
        binding.etSearchJob.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterJobs(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        listenToAvailableJobs()
    }

    /**
     * Fungsi untuk memfilter list lowongan berdasarkan input teks pencarian
     */
    private fun filterJobs(query: String) {
        filteredJobList.clear()

        if (query.isEmpty()) {
            // Jika kolom pencarian kosong, tampilkan kembali seluruh lowongan yang tersedia
            filteredJobList.addAll(allJobList)
        } else {
            val lowercaseQuery = query.lowercase(Locale.getDefault()).trim()
            for (job in allJobList) {
                val title = job.jobTitle?.lowercase(Locale.getDefault()) ?: ""
                val company = job.companyName?.lowercase(Locale.getDefault()) ?: ""

                // Lowongan lolos filter jika judul pekerjaan atau nama instansi mengandung kata kunci
                if (title.contains(lowercaseQuery) || company.contains(lowercaseQuery)) {
                    filteredJobList.add(job)
                }
            }
        }
        // Beritahu adapter bahwa data tampilan telah berubah
        jobAdapter.notifyDataSetChanged()
    }

    private fun listenToAvailableJobs() {
        listenerRef = db.collection("jobs")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (_binding == null) return@addSnapshotListener

                allJobList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val job = doc.toObject(Job::class.java)
                        if (job != null) {
                            allJobList.add(job)
                        }
                    }
                }

                // Jalankan filter ulang menggunakan teks yang sedang aktif di EditText saat ini
                filterJobs(binding.etSearchJob.text.toString())
            }
    }

    override fun onDestroyView() {
        listenerRef?.remove()
        super.onDestroyView()
        _binding = null
    }

    /**
     * ADAPTER RECYCLERVIEW INTERNAL
     */
    private class SeekerJobAdapter(
        private val list: List<Job>,
        private val onClick: (Job) -> Unit
    ) : RecyclerView.Adapter<SeekerJobAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val photo: ImageView = view.findViewById(R.id.ivItemJobPhoto)
            val title: TextView = view.findViewById(R.id.tvItemJobTitle)
            val company: TextView = view.findViewById(R.id.tvItemJobCompany)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val job = list[position]
            holder.title.text = job.jobTitle
            holder.company.text = job.companyName
            holder.photo.setImageResource(android.R.drawable.ic_menu_gallery) // Default placeholder

            // Render gambar Base64 bawaan dari objek lowongan
            if (!job.jobImage.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(job.jobImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    holder.photo.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            holder.itemView.setOnClickListener { onClick(job) }
        }

        override fun getItemCount() = list.size
    }
}