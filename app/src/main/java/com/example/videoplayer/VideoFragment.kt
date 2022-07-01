package com.example.videoplayer

import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.databinding.FragmentVideoBinding
import java.util.*
import kotlin.collections.ArrayList

class VideoFragment : Fragment() {

    private lateinit var adapter: VideoAdapter
    private lateinit var binding:FragmentVideoBinding
    override fun  onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        requireContext().theme.applyStyle(MainActivity.themesList[MainActivity.themeIndex],true)
        val view = inflater.inflate(R.layout.fragment_video, container, false)
        binding =FragmentVideoBinding.bind(view)
        binding.VideoRV.setHasFixedSize(true)
        binding.VideoRV.setItemViewCacheSize(10)
        binding.VideoRV.layoutManager = LinearLayoutManager(requireContext())
        adapter=VideoAdapter(requireContext(), MainActivity.videoList)
        binding.VideoRV.adapter =adapter
        binding.totalVideos.text="Total Videos :${MainActivity.videoList.size}"

        binding.root.setOnRefreshListener{
            MainActivity.videoList= getAllVideos(requireContext())
            adapter.updateList(MainActivity.videoList)
            binding.root.isRefreshing=false
            binding.totalVideos.text="Total Videos :${MainActivity.videoList.size}"

        }

        binding.nowPlayingBtn.setOnClickListener {
            val intent = Intent(requireContext(),PlayerActivity::class.java)
            intent.putExtra("class","NowPlaying")
            startActivity(intent)
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_view,menu)
        val searchView=menu.findItem(R.id.serachView)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean =true

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText!=null) {
                    MainActivity.searchlist= ArrayList()
                    for (video in MainActivity.videoList){
                        if (video.title.lowercase().contains(newText.lowercase()))
                            MainActivity.searchlist.add(video)

                    }
                    MainActivity.search=true
                    adapter.updateList(searchList=MainActivity.searchlist)
                }
                return  true
            }

        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onResume() {
        super.onResume()
        if (PlayerActivity.position!=-1)binding.nowPlayingBtn.visibility=View.VISIBLE
        if (MainActivity.adapterChanged)adapter.notifyDataSetChanged()
        MainActivity.adapterChanged=false
    }
}