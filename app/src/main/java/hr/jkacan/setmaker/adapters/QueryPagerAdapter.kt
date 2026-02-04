package hr.jkacan.setmaker.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import hr.jkacan.setmaker.models.song.SongProvider
import hr.jkacan.setmaker.fragments.QueryTabFragment

class QueryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> QueryTabFragment.Companion.newInstance(SongProvider.SPOTIFY)
            1 -> QueryTabFragment.Companion.newInstance(SongProvider.SOUNDCLOUD)
            2 -> QueryTabFragment.Companion.newInstance(SongProvider.LOCAL)
            else -> QueryTabFragment.Companion.newInstance(SongProvider.SPOTIFY)
        }
    }
}