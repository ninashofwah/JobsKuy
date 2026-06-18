package com.proweb.jobskuy.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.data.Application
import com.proweb.jobskuy.databinding.FragmentMyApplicationsBinding

class MyApplicationsFragment : Fragment(R.layout.fragment_my_applications) {

    private var _binding: FragmentMyApplicationsBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appList = ArrayList<Application>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyApplicationsBinding.bind(view)

        binding.rvMyApplications.layoutManager = LinearLayoutManager(requireContext())

        db.collection("applications")
            .whereEqualTo("seekerUid", auth.currentUser?.uid ?: "")
            .addSnapshotListener { snapshots, _ ->
                appList.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        appList.add(doc.toObject(Application::class.java))
                    }
                }
                val adapter = ApplicantListFragment.ApplicantAdapter(appList)
                binding.rvMyApplications.adapter = adapter
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}