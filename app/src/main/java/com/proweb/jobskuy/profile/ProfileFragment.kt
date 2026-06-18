package com.proweb.jobskuy.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Tampilkan email data privat akun recruiter
            binding.tvRecruiterEmail.text = currentUser.email

            // Muat data profil instansi/perusahaan recruiter dari Firestore
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        binding.tvRecruiterName.text = document.getString("name") ?: "Nama Tidak Diatur"
                        binding.tvCompanyName.text = document.getString("company") ?: "UMKM/Perusahaan"
                    }
                }
        }

        // AKSI TOMBOL LOGOUT (Penyebab utama force close jika ID salah)
        binding.btnLogoutRecruiter.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Berhasil keluar dari akun Recruiter!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileRecruiter_to_roleSelection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}