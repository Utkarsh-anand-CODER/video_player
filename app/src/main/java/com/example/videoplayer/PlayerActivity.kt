package com.example.videoplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.opengl.Visibility
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore
import android.renderscript.ScriptGroup
import android.view.*
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.videoplayer.PlayerActivity.Companion.nowPlayingId
import com.example.videoplayer.PlayerActivity.Companion.pipStatus
import com.example.videoplayer.PlayerActivity.Companion.playerList
import com.example.videoplayer.PlayerActivity.Companion.position
import com.example.videoplayer.databinding.ActivityPlayerBinding
import com.example.videoplayer.databinding.BoosterBinding
import com.example.videoplayer.databinding.MoreFeaturesBinding
import com.example.videoplayer.databinding.SpeedDialogBinding
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.system.exitProcess



class PlayerActivity : AppCompatActivity(), AudioManager.OnAudioFocusChangeListener,
    GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityPlayerBinding
    private var isSubtitle: Boolean = true
    private lateinit var playPauseBtn: ImageButton
    private lateinit var fullScreenBtn: ImageButton
    private lateinit var videoTitle: TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat

    companion object {
        private var audioManager: AudioManager? = null
        private var timer: Timer? = null
        private lateinit var player: SimpleExoPlayer
        lateinit var playerList: ArrayList<Video>
        var position: Int = -1
        private var repeat: Boolean = false
        private var isFullscreen: Boolean = false
        private var isLocked: Boolean = false
        private lateinit var trackSelector: DefaultTrackSelector
        private lateinit var loudnessEnhancer: LoudnessEnhancer
        private var speed: Float = 1.0f
        var pipStatus: Int = 0
        var nowPlayingId: String = ""
        private var brightness:Int=0
        private var volume:Int=0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        //for immersive mode
        setTheme(R.style.playerActivityTheme)
        setContentView(binding.root)

        videoTitle = findViewById(R.id.videoTitle)
        playPauseBtn = findViewById(R.id.playPauseBtn)
        fullScreenBtn = findViewById(R.id.fullScreenBtn)


        gestureDetectorCompat = GestureDetectorCompat(this, this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
        try {
            if (intent.data?.scheme.contentEquals("content")) {
                playerList = ArrayList()
                position = 0
                val cursor = contentResolver.query(
                    intent.data!!,
                    arrayOf(MediaStore.Video.Media.DATA),
                    null,
                    null,
                    null
                )
                cursor?.let {
                    it.moveToFirst()
                    val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                    val file = File(path)
                    val video = Video(
                        id = "",
                        title = file.name,
                        duration = 0L,
                        artUri = Uri.fromFile(file),
                        path = path,
                        size = "",
                        folderName = ""
                    )
                    playerList.add(video)
                    cursor.close()
                }
                createPlayer()
                intializeBinding()
            } else {
                initializeLayout()
                intializeBinding()
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
        }

    }

    private fun initializeLayout() {
        when (intent.getStringExtra("class")) {
            "AllVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.videoList)
                createPlayer()

            }
            "FolderActivity" -> {
                playerList = ArrayList()
                playerList.addAll(FolderActivity.currentFolderVideos)
                createPlayer()
            }
            "SearchedVideos" -> {
                playerList = ArrayList()
                playerList.addAll(MainActivity.searchlist)
                createPlayer()
            }
            "NowPlaying" -> {
                speed = 1.0f

                videoTitle.text = playerList[position].title
                videoTitle.isSelected = true
                binding.playerView.player = player
                doubleTapEnable()
                playVideo()
                playInFullscreen(enable = isFullscreen)
                seekBarFeature()

            }
        }
        if (repeat) findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
        else findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
    }

    private fun intializeBinding() {
        findViewById<ImageButton>(R.id.orientationBtn).setOnClickListener {
            requestedOrientation =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }




        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }
        playPauseBtn.setOnClickListener {
            if (player.isPlaying) pauseVideo()
            else playVideo()
        }
        findViewById<ImageButton>(R.id.nextBtn).setOnClickListener { nextPrevVideo() }
        findViewById<ImageButton>(R.id.prevBtn).setOnClickListener { nextPrevVideo(isNext = false) }
        findViewById<ImageButton>(R.id.repeatBtn).setOnClickListener {
            if (repeat) {
                repeat = false
                player.repeatMode = Player.REPEAT_MODE_OFF
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_off)
            } else {
                repeat = true
                player.repeatMode = Player.REPEAT_MODE_ONE
                findViewById<ImageButton>(R.id.repeatBtn).setImageResource(com.google.android.exoplayer2.ui.R.drawable.exo_controls_repeat_all)
            }
        }
        fullScreenBtn.setOnClickListener {
            if (isFullscreen) {
                isFullscreen = false
                playInFullscreen(enable = false)
            } else {
                isFullscreen = true
                playInFullscreen(enable = true)
            }
        }
        binding.lockButton.setOnClickListener {
            if (!isLocked) {
                //for hiding
                isLocked = true
                binding.playerView.hideController()
                binding.playerView.useController = false
                binding.lockButton.setImageResource(R.drawable.close_lock_icon)
            } else {
                isLocked = false
                binding.playerView.useController = true
                binding.playerView.showController()
                binding.lockButton.setImageResource(R.drawable.lock_open_icon)
            }
        }
        findViewById<ImageButton>(R.id.moreFeaturesBtn).setOnClickListener {
            pauseVideo()
            val customDialog =
                LayoutInflater.from(this).inflate(R.layout.more_features, binding.root, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this).setView(customDialog)
                .setOnCancelListener { playVideo() }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialog.show()
            bindingMF.audioTrack.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val audioTrack = ArrayList<String>()
                val audioList =ArrayList<String>()
               for (i in 0 until player.currentTrackGroups.length) {
                    if (player.currentTrackGroups.get(i)
                            .getFormat(0).selectionFlags == C.SELECTION_FLAG_DEFAULT
                    ) {
                        audioTrack.add(
                                      Locale(
                                          player.currentTrackGroups.get(i).getFormat(0).language.toString()
                            ).displayLanguage
                       )
                    }
                }


                val tempTracks = audioTrack.toArray(arrayOfNulls<CharSequence>(audioTrack.size))
                MaterialAlertDialogBuilder(this, R.style.alertDialog)
                    .setTitle("Select Language")
                    .setOnCancelListener { playVideo() }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .setItems(tempTracks) { _, position ->
                        Toast.makeText(this, audioTrack[position] + "Selected", Toast.LENGTH_SHORT)
                            .show()
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setPreferredAudioLanguage(audioTrack[position])
                        )
                    }
                    .create()
                    .show()
            }
            bindingMF.subtitlesBtn.setOnClickListener {
                if (isSubtitle) {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, true
                        ).build()
                    Toast.makeText(this, "Subtitles off", Toast.LENGTH_SHORT).show()
                    isSubtitle = false
                } else {
                    trackSelector.parameters =
                        DefaultTrackSelector.ParametersBuilder(this).setRendererDisabled(
                            C.TRACK_TYPE_VIDEO, false
                        ).build()
                    Toast.makeText(this, "Subtitles on", Toast.LENGTH_SHORT).show()
                    isSubtitle = true
                }
                dialog.dismiss()
                playVideo()
            }
            bindingMF.audioBooster.setOnClickListener {
                dialog.dismiss()
                val customDialogB =
                    LayoutInflater.from(this).inflate(R.layout.booster, binding.root, false)
                val bindingB = BoosterBinding.bind(customDialogB)
                val dialogB = MaterialAlertDialogBuilder(this).setView(customDialogB)
                    .setOnCancelListener { playVideo() }
                    .setPositiveButton("ok") { self, _ ->
                        loudnessEnhancer.setTargetGain(bindingB.verticalBar.progress * 100)
                        playVideo()
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogB.show()
                bindingB.verticalBar.progress = loudnessEnhancer.targetGain.toInt() / 100
                bindingB.progressText.text =
                    "Audio Boost\n\n${loudnessEnhancer.targetGain.toInt() / 10} %"
                bindingB.verticalBar.setOnProgressChangeListener {
                    bindingB.progressText.text = "Audio Boost\n\n${it * 10} %"
                }
            }
            bindingMF.speedBtn.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val customDialogS =
                    LayoutInflater.from(this).inflate(R.layout.speed_dialog, binding.root, false)
                val bindingS = SpeedDialogBinding.bind(customDialogS)
                val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                    .setCancelable(false)
                    .setPositiveButton("ok") { self, _ ->
                        self.dismiss()
                    }
                    .setBackground(ColorDrawable(0x803700B3.toInt()))
                    .create()
                dialogS.show()
                bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                bindingS.minusBtn.setOnClickListener {
                    changeSpeed(isIncrement = false)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
                bindingS.plusBtn.setOnClickListener {
                    changeSpeed(isIncrement = true)
                    bindingS.speedText.text = "${DecimalFormat("#.##").format(speed)} X"
                }
            }
            bindingMF.sleepTimer.setOnClickListener {
                dialog.dismiss()
                if (timer != null) Toast.makeText(this, "Timer running", Toast.LENGTH_SHORT).show()
                else {
                    var sleepTime = 15
                    val customDialogS = LayoutInflater.from(this)
                        .inflate(R.layout.speed_dialog, binding.root, false)
                    val bindingS = SpeedDialogBinding.bind(customDialogS)
                    val dialogS = MaterialAlertDialogBuilder(this).setView(customDialogS)
                        .setCancelable(false)
                        .setPositiveButton("ok") { self, _ ->
                            timer = Timer()
                            val task = object : TimerTask() {
                                override fun run() {
                                    moveTaskToBack(true)
                                    exitProcess(1)
                                }
                            }
                            timer!!.schedule(task, sleepTime * 60 * 1000.toLong())
                            self.dismiss()
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0x803700B3.toInt()))
                        .create()
                    dialogS.show()
                    bindingS.speedText.text = "$sleepTime Min"
                    bindingS.minusBtn.setOnClickListener {
                        if (sleepTime >= 15) sleepTime -= 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                    bindingS.plusBtn.setOnClickListener {
                        if (sleepTime < 120) sleepTime += 15
                        bindingS.speedText.text = "$sleepTime Min"
                    }
                }
            }
            bindingMF.pipModeBtn.setOnClickListener {
                val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val status = if (appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        android.os.Process.myUid(),
                        packageName
                    ) == AppOpsManager.MODE_ALLOWED
                ) {
                    true
                } else {
                    false
                }
                if (status) {
                    this.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    dialog.dismiss()
                    binding.playerView.hideController()
                    playVideo()
                    pipStatus = 0
                } else {
                    val intent = Intent(
                        "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
        }
    }

    private fun createPlayer() {
        try {
            player.release()
        } catch (e: Exception) {
        }
        speed = 1.0f
        trackSelector = DefaultTrackSelector(this)
        videoTitle.text = playerList[position].title
        videoTitle.isSelected = true
        player = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        doubleTapEnable()
        val mediaItem = MediaItem.fromUri(playerList[position].artUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        playVideo()
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) nextPrevVideo()
            }
        })
        playInFullscreen(enable = isFullscreen)

        loudnessEnhancer = LoudnessEnhancer(player.audioSessionId)
        loudnessEnhancer.enabled = true
        nowPlayingId = playerList[position].id
        seekBarFeature()
        binding.playerView.setControllerVisibilityListener {
            when {
                isLocked -> binding.lockButton.visibility = View.VISIBLE
                binding.playerView.isControllerVisible -> binding.lockButton.visibility =
                    View.VISIBLE
                else -> binding.lockButton.visibility = View.INVISIBLE
            }

        }
    }

    private fun playVideo() {
        playPauseBtn.setImageResource(R.drawable.pause_icon)
        player.play()
    }

    private fun pauseVideo() {
        playPauseBtn.setImageResource(R.drawable.play_icon)
        player.pause()
    }

    private fun nextPrevVideo(isNext: Boolean = true) {
        if (isNext) setPostion()
        else setPostion(isIncrement = false)
        createPlayer()
    }

    private fun setPostion(isIncrement: Boolean = true) {
        if (!repeat) {
            if (isIncrement) {
                if (playerList.size - 1 == position)
                    position = 0
                else ++position
            } else {
                if (position == 0)
                    position = playerList.size - 1
                else --position
            }
        }
    }

    private fun playInFullscreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenBtn.setImageResource(R.drawable.fullscreen_exit_icon)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenBtn.setImageResource(R.drawable.fullscreen_icon)
        }
    }


    private fun changeSpeed(isIncrement: Boolean) {
        if (isIncrement) {
            if (speed <= 2.9f) {
                speed += 0.30f
            }
        } else {
            if (speed > 0.20f) {
                speed -= 0.30f
            }
        }
        player.setPlaybackSpeed(speed)
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) pauseVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.pause()
        audioManager?.abandonAudioFocus(this)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) pauseVideo()
    }

    override fun onResume() {
        super.onResume()
        if (audioManager == null) audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (brightness!=0)setScreenBrightness(brightness)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doubleTapEnable() {
        binding.playerView.player = player
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }

        })
        binding.ytOverlay.player(player)
        binding.playerView.setOnTouchListener { _, motionEvent ->
            binding.playerView.isDoubleTapEnabled=false
            if (!isLocked){
                binding.playerView.isDoubleTapEnabled=true
                gestureDetectorCompat.onTouchEvent(motionEvent)
                if (motionEvent.action==MotionEvent.ACTION_UP){
                    binding.brightnessIcon.visibility=View.GONE
                    binding.VolumeIcon.visibility=View.GONE
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
                    }
                }
            }
            return@setOnTouchListener false

        }
    }

    private fun seekBarFeature() {
        findViewById<DefaultTimeBar>(R.id.exo_progress).addListener(object :
            TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                pauseVideo()
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playVideo()
            }

        })
    }

    override fun onDown(p0: MotionEvent?): Boolean = false
    override fun onShowPress(p0: MotionEvent?) = Unit
    override fun onSingleTapUp(p0: MotionEvent?): Boolean = false
    override fun onLongPress(p0: MotionEvent?) = Unit
    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean =false

    override fun onScroll(event: MotionEvent?, event1: MotionEvent?, distanceX: Float, distacneY: Float): Boolean {
        val sWidth= Resources.getSystem().displayMetrics.widthPixels
        val sHeight=Resources.getSystem().displayMetrics.heightPixels

        val border=100*Resources.getSystem().displayMetrics.density.toInt()
        if (event!!.x<border||event.y<border||event.x>sWidth-border||event.y>sHeight-border)
            return  false

        if (abs (distanceX)< abs(distacneY)){
            if (event.x<sWidth/2){
                binding.brightnessIcon.visibility=View.VISIBLE
                binding.VolumeIcon.visibility=View.GONE
                val increase=distacneY>0
                val newValue=if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..30) brightness=newValue
                binding.brightnessIcon.text= brightness.toString()
                setScreenBrightness(brightness)
            }
            else{
                binding.brightnessIcon.visibility=View.GONE
                binding.VolumeIcon.visibility=View.VISIBLE
                val maxVolume= audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase=distacneY>0
                val newValue=if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume=newValue
                binding.VolumeIcon.text= volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume,0)
            }
        }
        return true
    }

    private fun setScreenBrightness(value:Int){
        val d=1.0f/30
        val lp =this.window.attributes
        lp.screenBrightness=d*value
        this.window.attributes=lp
    }
}