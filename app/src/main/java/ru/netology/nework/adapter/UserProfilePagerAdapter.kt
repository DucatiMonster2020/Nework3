package ru.netology.nework.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.ui.UserJobsFragment
import ru.netology.nework.ui.UserWallFragment

class UserProfilePagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val userId: Long,
    private val isCurrentUser: Boolean
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserWallFragment.newInstance(userId, isCurrentUser)
            1 -> UserJobsFragment.newInstance(userId, isCurrentUser)
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}