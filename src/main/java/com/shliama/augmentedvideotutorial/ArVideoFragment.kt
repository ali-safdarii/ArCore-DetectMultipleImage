package com.shliama.augmentedvideotutorial

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException


open class ArVideoFragment : ArFragment() {

    private  var node: AnchorNode?=null
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var externalTexture: ExternalTexture
    private lateinit var videoRenderable: ModelRenderable
    private lateinit var videoAnchorNode: AnchorNode
    private var activeAugmentedImage: AugmentedImage? = null
    private var hasFinishedLoading = false
    private var viewRenderable: ViewRenderable? = null
    lateinit var completableFuture: CompletableFuture<ViewRenderable>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaPlayer = MediaPlayer()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)



        planeDiscoveryController.hide()
        planeDiscoveryController.setInstructionView(null)
        arSceneView.planeRenderer.isEnabled = false
        arSceneView.isLightEstimationEnabled = false
        createArScene()
        initializeSession()


        // Build a renderable from a 2D View.
        completableFuture =
            ViewRenderable.builder().setView(context!!, R.layout.azimuth_layout).build()


        CompletableFuture.allOf(
            completableFuture
        )
            .handle<Any?> { notUsed: Void?, throwable: Throwable? ->
                if (throwable != null) {
                    DemoUtils.displayError(context!!, "Unable to load renderable", throwable)
                    return@handle null
                }
                try {

                    viewRenderable = completableFuture.get()

                    // Everything finished loading successfully.
                    hasFinishedLoading = true
                } catch (ex: InterruptedException) {
                    DemoUtils.displayError(context!!, "Unable to load renderable", ex)
                } catch (ex: ExecutionException) {
                    DemoUtils.displayError(context!!, "Unable to load renderable", ex)
                }
                null
            }



        return view
    }


    override fun getSessionConfiguration(session: Session?): Config {
        val arConfig = super.getSessionConfiguration(session)
        arConfig.focusMode = Config.FocusMode.AUTO
        if (session == null || !setupAugmentedImageDatabase(arConfig, session))
            activity?.finish()
        return arConfig
    }

    private fun setupAugmentedImageDatabase(config: Config, session: Session): Boolean {
        var db: AugmentedImageDatabase? = null
        try {
            context!!.assets.open("myimages.imgdb")
                .use { loadedDB ->
                    db = AugmentedImageDatabase.deserialize(session, loadedDB)

                    Toast.makeText(context, "${db!!.numImages}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: IOException) {
            return false
        }

        config.augmentedImageDatabase = db
        return true
    }

    private fun createArScene() {
        // Create an ExternalTexture for displaying the contents of the video.
        externalTexture = ExternalTexture().also {
            mediaPlayer.setSurface(it.surface)
        }

        // Create a renderable with a material that has a parameter of type 'samplerExternal' so that
        // it can display an ExternalTexture.
        ModelRenderable.builder()
            .setSource(requireContext(), R.raw.augmented_video_model)
            .build()
            .thenAccept { renderable ->
                videoRenderable = renderable
                renderable.isShadowCaster = false
                renderable.isShadowReceiver = false
                renderable.material.setExternalTexture("videoTexture", externalTexture)
            }
            .exceptionally { throwable ->
                Log.e(TAG, "Could not create ModelRenderable", throwable)
                return@exceptionally null
            }

        videoAnchorNode = AnchorNode().apply {
            setParent(arSceneView.scene)
        }
    }

    /**
     * In this case, we want to support the playback of one video at a time.
     * Therefore, if ARCore loses current active image FULL_TRACKING we will pause the video.
     * If the same image gets FULL_TRACKING back, the video will resume.
     * If a new image will become active, then the corresponding video will start from scratch.
     */


    fun visibleView(): Boolean {
        val v = viewRenderable!!.view
        val profileImage = v.findViewById<CircleImageView>(R.id.phone)
        val webImage = v.findViewById<CircleImageView>(R.id.webImage)
        val editImage = v.findViewById<CircleImageView>(R.id.editImage)
        profileImage.visibility = View.VISIBLE
        webImage.visibility = View.VISIBLE
        editImage.visibility = View.VISIBLE

        return true
    }

    fun goneView(): Boolean {
        val v = viewRenderable!!.view
        val profileImage = v.findViewById<CircleImageView>(R.id.phone)
        val webImage = v.findViewById<CircleImageView>(R.id.webImage)
        val editImage = v.findViewById<CircleImageView>(R.id.editImage)
        profileImage.visibility = View.GONE
        webImage.visibility = View.GONE
        editImage.visibility = View.GONE

        return true
    }

    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView.arFrame ?: return

        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)


        // If current active augmented image isn't tracked anymore and video playback is started - pause video playback
        val nonFullTrackingImages =
            updatedAugmentedImages.filter { it.trackingMethod != AugmentedImage.TrackingMethod.FULL_TRACKING }
        activeAugmentedImage?.let { activeAugmentedImage ->
            if (isArVideoPlaying() && nonFullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                pauseArVideo()
                if (visibleView())
                    goneView()
                else
                    return

            }
        }

        val fullTrackingImages =
            updatedAugmentedImages.filter { it.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING }
        if (fullTrackingImages.isEmpty()) return

        // If current active augmented image is tracked but video playback is paused - resume video playback
        activeAugmentedImage?.let { activeAugmentedImage ->
            if (fullTrackingImages.any { it.index == activeAugmentedImage.index }) {
                if (!isArVideoPlaying()) {
                    resumeArVideo()
                    if (goneView())
                        visibleView()
                    else
                        return
                }
                return
            }
        }

        // Otherwise - make the first tracked image active and start video playback
        fullTrackingImages.firstOrNull()?.let { augmentedImage ->
            try {
                playbackArVideo(augmentedImage)
                if (goneView())
                    visibleView()
                else
                    return
            } catch (e: Exception) {
                Log.e(TAG, "Could not play video [${augmentedImage.name}]", e)
            }
        }


        when (activeAugmentedImage!!.trackingState) {

            TrackingState.PAUSED -> {

                Toast.makeText(context!!, "Paused", Toast.LENGTH_SHORT).show()
            }
            TrackingState.STOPPED -> {
                Toast.makeText(context!!, "stop", Toast.LENGTH_SHORT).show()
            }
        }


        when (activeAugmentedImage!!.index) {

            0 -> {
                // Create an anchor for the image that is tracking
                node =
                    AnchorNode(activeAugmentedImage!!.createAnchor(activeAugmentedImage!!.centerPose))
                arSceneView.scene.addChild(node)
                viewRendrable(activeAugmentedImage!!, node!!)


            }
            1 -> {

                node =
                    AnchorNode(activeAugmentedImage!!.createAnchor(activeAugmentedImage!!.centerPose))
                arSceneView.scene.addChild(node)
                viewRendrable(activeAugmentedImage!!, node!!)

            }

            2 -> {
                node =
                    AnchorNode(activeAugmentedImage!!.createAnchor(activeAugmentedImage!!.centerPose))
                arSceneView.scene.addChild(node)
                viewRendrable(activeAugmentedImage!!, node!!)

            }

        }


    }

    private fun isArVideoPlaying() = mediaPlayer.isPlaying

    private fun pauseArVideo() {
        videoAnchorNode.renderable = null
        mediaPlayer.pause()


    }

    private fun resumeArVideo() {
        mediaPlayer.start()
        videoAnchorNode.renderable = videoRenderable
    }

    private fun dismissArVideo() {
        videoAnchorNode.anchor?.detach()
        videoAnchorNode.renderable = null
        activeAugmentedImage = null
        mediaPlayer.reset()
    }


    private fun viewRendrable(augmentedImage: AugmentedImage, anchorNode: AnchorNode) {
        completableFuture =
            ViewRenderable.builder().setView(context, R.layout.azimuth_layout).build()

        completableFuture.thenAccept {
            val nod = Node()
            nod.renderable = viewRenderable
            //scale the text to the size we need it.
            val localScale = Vector3()
            localScale.set(0.15f, 0.15f, 0.15f)
            nod.localScale = localScale

            val pose = Pose.makeTranslation(0.0f, 0.0f, augmentedImage.extentX)

            //nod.localScale = localScale
            nod.localPosition = Vector3(pose.tx(), pose.ty(), pose.tz())
            nod.localRotation = Quaternion(90f, 0f, 0f, -90f)
            nod.setParent(anchorNode)


            val call = viewRenderable!!.view.findViewById<CircleImageView>(R.id.phone)
            val web = viewRenderable!!.view.findViewById<CircleImageView>(R.id.webImage)
            val instagram = viewRenderable!!.view.findViewById<CircleImageView>(R.id.editImage)

            instagram.setOnClickListener {

                val uri = Uri.parse("http://instagram.com/_u/azimuthnegar?igshid=5ges6jru6ar9")
                val likeIng = Intent(Intent.ACTION_VIEW, uri)

                likeIng.setPackage("com.instagram.android")

                try {
                    startActivity(likeIng)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/xxx")
                        )
                    )
                }

            }
            web.setOnClickListener {


                Thread(Runnable {
                    // a potentially time consuming task

                    val url = "https://azimuthnegar.com"
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(url)
                    val title = "Select a browser"
                    val chooser = Intent.createChooser(i, title)

                        if (i.resolveActivity(context!!.packageManager) != null) {
                            startActivity(chooser)
                    

                    }
                }).start()




            }
            call.setOnClickListener {


                Dexter.withContext(context)
                    .withPermission(android.Manifest.permission.CALL_PHONE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse) {


                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = Uri.parse("tel:02144225163")
                            startActivity(intent)
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse) { /* ... */
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest,
                            token: PermissionToken
                        ) {
                        }
                    }).check()


            }


        }

    }


    private fun playbackArVideo(augmentedImage: AugmentedImage) {
        Log.d(TAG, "playbackVideo = ${augmentedImage.name}")

        val name = augmentedImage.name.replace(".jpg", "")

        Toast.makeText(context, name + "", Toast.LENGTH_SHORT).show()

        requireContext().assets.openFd(name)
            .use { descriptor ->
                mediaPlayer.reset()
                mediaPlayer.setDataSource(descriptor)
            }.also {

                mediaPlayer.prepare()
                mediaPlayer.isLooping = true
                mediaPlayer.start()

            }

        //addTextLabel(augmentedImage)

        videoAnchorNode.anchor?.detach()
        videoAnchorNode.anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
        videoAnchorNode.localScale = Vector3(
            augmentedImage.extentX, // width
            1.0f,
            augmentedImage.extentZ
        ) // height


        activeAugmentedImage = augmentedImage

        externalTexture.surfaceTexture.setOnFrameAvailableListener {
            it.setOnFrameAvailableListener(null)
            videoAnchorNode.renderable = videoRenderable
        }
    }

    override fun onPause() {
        super.onPause()
        dismissArVideo()
        if (node!=null)
        if (completableFuture.isCompletedExceptionally || completableFuture.isDone){
            removeAnchorNode(node!!)
            Log.d(TAG, "onPause: Remove Node Successfully")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    companion object {
        private const val TAG = "ArVideoFragment"

    }

    private fun removeAnchorNode(nodeToRemove: AnchorNode) {
        //Remove an Anchor node
        arSceneView.scene.removeChild(nodeToRemove);
        nodeToRemove.getAnchor()?.detach();
        nodeToRemove.setParent(null);
        nodeToRemove.renderable = null
    }

}