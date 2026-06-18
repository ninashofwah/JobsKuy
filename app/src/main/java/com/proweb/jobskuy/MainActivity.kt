package com.proweb.jobskuy

import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.proweb.jobskuy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Hubungkan komponen menu XML ke Jetpack Controller
        binding.bottomNavigationRecruiter.setupWithNavController(navController)
        binding.bottomNavigationSeeker.setupWithNavController(navController)

        val layoutParams = binding.navHostFragment.layoutParams as RelativeLayout.LayoutParams

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // KLASTER PENYEDIA KERJA (RECRUITER)
                R.id.homeRecruiterFragment,
                R.id.myJobsFragment,
                R.id.applicantListFragment,
                R.id.profileFragment -> {
                    binding.bottomNavigationRecruiter.visibility = View.VISIBLE
                    binding.bottomNavigationSeeker.visibility = View.GONE

                    // Sandarkan fragment di atas footer Recruiter
                    layoutParams.addRule(RelativeLayout.ABOVE, R.id.bottomNavigationRecruiter)
                    binding.navHostFragment.layoutParams = layoutParams
                }

                // KLASTER PENCARI KERJA (SEEKER)
                R.id.homeSeekerFragment,
                R.id.myApplicationsFragment,
                R.id.profileSeekerFragment -> {
                    binding.bottomNavigationSeeker.visibility = View.VISIBLE
                    binding.bottomNavigationRecruiter.visibility = View.GONE

                    // Sandarkan fragment di atas footer Seeker
                    layoutParams.addRule(RelativeLayout.ABOVE, R.id.bottomNavigationSeeker)
                    binding.navHostFragment.layoutParams = layoutParams
                }

                // KLASTER REGISTRASI & LOGIN
                else -> {
                    binding.bottomNavigationRecruiter.visibility = View.GONE
                    binding.bottomNavigationSeeker.visibility = View.GONE

                    // Reset sandaran agar fragment memenuhi seluruh layar penuh (Full Screen)
                    layoutParams.removeRule(RelativeLayout.ABOVE)
                    binding.navHostFragment.layoutParams = layoutParams
                }
            }
        }
    }
}