package com.proweb.jobskuy.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentProfileSeekerBinding

class ProfileSeekerFragment : Fragment(R.layout.fragment_profile_seeker) {

    private var _binding: FragmentProfileSeekerBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileSeekerBinding.bind(view)

        val uid = auth.currentUser?.uid ?: return

        db.collection("seeker_profiles").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.etProfileDocsLink.setText(doc.getString("resumeUrl"))
                binding.etProfileVideoLink.setText(doc.getString("videoUrl"))
                binding.etProfileExperience.setText(doc.getString("experience"))
            }
        }

        binding.btnSaveSeekerProfile.setOnClickListener {
            val docs = binding.etProfileDocsLink.text.toString().trim()
            val video = binding.etProfileVideoLink.text.toString().trim()
            val exp = binding.etProfileExperience.text.toString().trim()

            val publicProfileData = hashMapOf(
                "seekerUid" to uid,
                "resumeUrl" to docs,
                "videoUrl" to video,
                "experience" to exp
            )

            db.collection("seeker_profiles").document(uid).set(publicProfileData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profil Publik diperbarui!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}