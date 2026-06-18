package com.proweb.jobskuy.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentProfileAccountSeekerBinding

class ProfileAccountSeekerFragment : Fragment(R.layout.fragment_profile_account_seeker) {

    private var _binding: FragmentProfileAccountSeekerBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileAccountSeekerBinding.bind(view)

        val user = auth.currentUser
        if (user != null) {
            binding.tvAccountEmail.text = user.email
            binding.tvAccountUid.text = user.uid
        }

        binding.btnLogoutSeeker.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Berhasil keluar!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profileAccount_to_roleSelection)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}