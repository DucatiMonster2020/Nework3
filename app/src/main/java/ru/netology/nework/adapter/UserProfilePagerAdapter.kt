package ru.netology.nework.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.netology.nework.ui.UserJobsFragment
import ru.netology.nework.ui.UserWallFragment

class UserProfilePagerAdapter(
    fragment: Fragment,
    private val userId: Long,
    private val isCurrentUser: Boolean
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserWallFragment.newInstance(userId, isCurrentUser)
            1 -> UserJobsFragment.newInstance(userId, isCurrentUser)
            else -> throw IndexOutOfBoundsException()
        }
    }
}