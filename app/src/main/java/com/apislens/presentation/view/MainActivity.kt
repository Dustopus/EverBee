package com.apislens.presentation.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.apislens.R
import com.apislens.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 让 BottomNav 与 Navigation Graph 联动
        binding.bottomNav.setupWithNavController(navController)

        // 在非顶级页面隐藏 BottomNav
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in setOf(
                R.id.deviceListFragment,
                R.id.dashboardFragment,
                R.id.settingsFragment
            )
            binding.bottomNav.visibility = if (isTopLevel) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
    }
}
