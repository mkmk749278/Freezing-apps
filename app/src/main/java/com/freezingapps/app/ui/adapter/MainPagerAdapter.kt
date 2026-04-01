package com.freezingapps.app.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.freezingapps.app.ui.fragment.AllAppsFragment
import com.freezingapps.app.ui.fragment.FrozenAppsFragment
import com.freezingapps.app.ui.fragment.SettingsFragment

/**
 * ViewPager2 adapter for the main three-tab layout.
 * Tab order: Frozen Apps (default), All Apps, Settings.
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val TAB_FROZEN = 0
        const val TAB_ALL_APPS = 1
        const val TAB_SETTINGS = 2
        const val TAB_COUNT = 3
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_FROZEN -> FrozenAppsFragment()
            TAB_ALL_APPS -> AllAppsFragment()
            TAB_SETTINGS -> SettingsFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
}
