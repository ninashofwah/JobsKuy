package com.proweb.jobskuy.ui.auth

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.proweb.jobskuy.R
import com.proweb.jobskuy.databinding.FragmentRoleSelectionBinding

class RoleSelectionFragment : Fragment(R.layout.fragment_role_selection) {
    private var _binding: FragmentRoleSelectionBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRoleSelectionBinding.bind(view)

        // Tombol untuk peran Pencari Kerja (Seeker)
        binding.btnPencariKerja.setOnClickListener {
            val bundle = Bundle().apply { putString("role", "Pencari Kerja") }
            findNavController().navigate(R.id.action_role_to_login, bundle)
        }

        // Tombol untuk peran Penyedia Kerja (Recruiter)
        binding.btnPenyediaKerja.setOnClickListener {
            val bundle = Bundle().apply { putString("role", "Penyedia Kerja") }
            findNavController().navigate(R.id.action_role_to_login, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}