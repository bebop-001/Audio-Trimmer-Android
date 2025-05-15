//  The MIT License (MIT)
//  Copyright (c) 2018 Intuz Pvt Ltd.
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
//  (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
//  merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
//  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
//  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
@file:Suppress("SpellCheckingInspection")

package com.demo.audiotrimmer

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.demo.audiotrimmer.customAudioViews.MarkerView
import com.demo.audiotrimmer.customAudioViews.MarkerView.MarkerListener
import com.demo.audiotrimmer.customAudioViews.SamplePlayer
import com.demo.audiotrimmer.customAudioViews.SoundFile
import com.demo.audiotrimmer.customAudioViews.SoundFile.Companion.create
import com.demo.audiotrimmer.customAudioViews.SoundFile.Companion.record
import com.demo.audiotrimmer.customAudioViews.WaveformView
import com.demo.audiotrimmer.customAudioViews.WaveformView.WaveformListener
import com.demo.audiotrimmer.utils.Utility
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

class AudioTrimmerActivity : AppCompatActivity(), View.OnClickListener, MarkerListener,
    WaveformListener {
    /* Audio trimmer*/
    private var txtAudioCancel: TextView? = null
    private var txtAudioUpload: TextView? = null
    private var txtStartPosition: TextView? = null
    private var txtEndPosition: TextView? = null
    private var llAudioCapture: LinearLayout? = null
    private var txtAudioRecord: TextView? = null
    private var txtAudioRecordTime: TextView? = null
    private var rlAudioEdit: RelativeLayout? = null
    private var markerStart: MarkerView? = null
    private var markerEnd: MarkerView? = null
    private var audioWaveform: WaveformView? = null
    private var txtAudioRecordTimeUpdate: TextView? = null
    private var txtAudioReset: TextView? = null
    private var txtAudioDone: TextView? = null
    private var txtAudioPlay: TextView? = null
    private var txtAudioRecordUpdate: TextView? = null
    private var txtAudioCrop: TextView? = null
    private var isAudioRecording = false
    private var mRecordingLastUpdateTime: Long = 0
    private var mRecordingTime = 0.0
    private var mRecordingKeepGoing = false
    private var mLoadedSoundFile: SoundFile? = null
    private var mRecordedSoundFile: SoundFile? = null
    private var mPlayer: SamplePlayer? = null
    private var mHandler: Handler? = null
    private var mTouchDragging = false
    private var mTouchStart = 0f
    private var mTouchInitialOffset = 0
    private var mTouchInitialStartPos = 0
    private var mTouchInitialEndPos = 0
    private var mDensity = 0f
    private var mMarkerLeftInset = 0
    private var mMarkerRightInset = 0
    private var mMarkerTopOffset = 0
    private var mMarkerBottomOffset = 0
    private var mTextLeftInset = 0
    private var mTextRightInset = 0
    private var mTextTopOffset = 0
    private var mTextBottomOffset = 0
    private var mOffset = 0
    private var mOffsetGoal = 0
    private var mFlingVelocity = 0
    private var mPlayEndMillSec = 0
    private var mWidth = 0
    private var mMaxPos = 0
    private var mStartPos = 0
    private var mEndPos = 0
    private var mStartVisible = false
    private var mEndVisible = false
    private var mLastDisplayedStartPos = 0
    private var mLastDisplayedEndPos = 0
    private var mIsPlaying = false
    private var mKeyDown = false
    private var mProgressDialog: ProgressDialog? = null
    private var mLoadingLastUpdateTime: Long = 0
    private var mLoadingKeepGoing = false
    private var mFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_trim)
        mHandler = Handler()
        txtAudioCancel = findViewById<View>(R.id.txtAudioCancel) as TextView
        txtAudioUpload = findViewById<View>(R.id.txtAudioUpload) as TextView
        txtStartPosition = findViewById<View>(R.id.txtStartPosition) as TextView
        txtEndPosition = findViewById<View>(R.id.txtEndPosition) as TextView
        llAudioCapture = findViewById<View>(R.id.llAudioCapture) as LinearLayout
        txtAudioRecord = findViewById<View>(R.id.txtAudioRecord) as TextView
        txtAudioRecordTime = findViewById<View>(R.id.txtAudioRecordTime) as TextView
        rlAudioEdit = findViewById<View>(R.id.rlAudioEdit) as RelativeLayout
        markerStart = findViewById<View>(R.id.markerStart) as MarkerView
        markerEnd = findViewById<View>(R.id.markerEnd) as MarkerView
        audioWaveform = findViewById<View>(R.id.audioWaveform) as WaveformView
        txtAudioRecordTimeUpdate = findViewById<View>(R.id.txtAudioRecordTimeUpdate) as TextView
        txtAudioReset = findViewById<View>(R.id.txtAudioReset) as TextView
        txtAudioDone = findViewById<View>(R.id.txtAudioDone) as TextView
        txtAudioPlay = findViewById<View>(R.id.txtAudioPlay) as TextView
        txtAudioRecordUpdate = findViewById<View>(R.id.txtAudioRecordUpdate) as TextView
        txtAudioCrop = findViewById<View>(R.id.txtAudioCrop) as TextView
        mRecordedSoundFile = null
        mKeyDown = false
        audioWaveform!!._setListener(this)
        markerStart!!.markerChangedListener(this)
        markerStart!!.setAlpha(1f)
        markerStart!!.isFocusable = true
        markerStart!!.setFocusableInTouchMode(true)
        mStartVisible = true
        markerEnd!!.markerChangedListener(this)
        markerEnd!!.setAlpha(1f)
        markerEnd!!.isFocusable = true
        markerEnd!!.setFocusableInTouchMode(true)
        mEndVisible = true
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mDensity = metrics.density
        /**
         * Change this for marker handle as per your view
         */
        mMarkerLeftInset = (17.5 * mDensity).toInt()
        mMarkerRightInset = (19.5 * mDensity).toInt()
        mMarkerTopOffset = (6 * mDensity).toInt()
        mMarkerBottomOffset = (6 * mDensity).toInt()
        /**
         * Change this for duration text as per your view
         */
        mTextLeftInset = (20 * mDensity).toInt()
        mTextTopOffset = (-1 * mDensity).toInt()
        mTextRightInset = (19 * mDensity).toInt()
        mTextBottomOffset = (-40 * mDensity).toInt()
        txtAudioCancel!!.setOnClickListener(this)
        txtAudioUpload!!.setOnClickListener(this)
        txtAudioRecord!!.setOnClickListener(this)
        txtAudioDone!!.setOnClickListener(this)
        txtAudioPlay!!.setOnClickListener(this)
        txtAudioRecordUpdate!!.setOnClickListener(this)
        txtAudioCrop!!.setOnClickListener(this)
        txtAudioReset!!.setOnClickListener(this)
        mHandler!!.postDelayed(mTimerRunnable, 100)
    }

    private val mTimerRunnable: Runnable = object : Runnable {
        override fun run() {
            // Updating Text is slow on Android.  Make sure
            // we only do the update if the text has actually changed.
            if (mStartPos != mLastDisplayedStartPos) {
                txtStartPosition!!.text = formatTime(mStartPos)
                mLastDisplayedStartPos = mStartPos
            }
            if (mEndPos != mLastDisplayedEndPos) {
                txtEndPosition!!.text = formatTime(mEndPos)
                mLastDisplayedEndPos = mEndPos
            }
            mHandler!!.postDelayed(this, 100)
        }
    }

    override fun onClick(view: View) {
        if (view === txtAudioRecord) {
            if (isAudioRecording) {
                isAudioRecording = false
                mRecordingKeepGoing = false
            } else {
                isAudioRecording = true
                txtAudioRecord!!.setBackgroundResource(R.drawable.ic_stop_btn1)
                txtAudioRecordTime!!.visibility = View.VISIBLE
                startRecording()
                mRecordingLastUpdateTime = Utility.currentTime
                mRecordingKeepGoing = true
            }
        } else if (view === txtAudioCancel) {
            finish()
        } else if (view === txtAudioRecordUpdate) {
            rlAudioEdit!!.visibility = View.GONE
            txtAudioUpload!!.visibility = View.GONE
            llAudioCapture!!.visibility = View.VISIBLE
            isAudioRecording = true
            txtAudioRecord!!.setBackgroundResource(R.drawable.ic_stop_btn1)
            txtAudioRecordTime!!.visibility = View.VISIBLE
            startRecording()
            mRecordingLastUpdateTime = Utility.currentTime
            mRecordingKeepGoing = true
            //            txtAudioCrop.setBackgroundResource(R.drawable.ic_crop_btn);
            txtAudioDone!!.visibility = View.GONE
            txtAudioCrop!!.visibility = View.VISIBLE
            txtAudioPlay!!.setBackgroundResource(R.drawable.ic_play_btn)
            markerStart!!.setVisibility(View.INVISIBLE)
            markerEnd!!.setVisibility(View.INVISIBLE)
            txtStartPosition!!.visibility = View.VISIBLE
            txtEndPosition!!.visibility = View.VISIBLE
        } else if (view === txtAudioPlay) {
            if (!mIsPlaying) {
                txtAudioPlay!!.setBackgroundResource(R.drawable.ic_pause_btn)
            } else {
                txtAudioPlay!!.setBackgroundResource(R.drawable.ic_play_btn)
            }
            onPlay(mStartPos)
        } else if (view === txtAudioDone) {
            val startTime = audioWaveform!!.pixelsToSeconds(mStartPos)
            val endTime = audioWaveform!!.pixelsToSeconds(mEndPos)
            val difference = endTime - startTime
            if (difference <= 0) {
                Toast.makeText(
                    this@AudioTrimmerActivity,
                    "Trim seconds should be greater than 0 seconds",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (difference > 60) {
                Toast.makeText(
                    this@AudioTrimmerActivity,
                    "Trim seconds should be less than 1 minute",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                if (mIsPlaying) {
                    handlePause()
                }
                saveRingtone(0)
                txtAudioDone!!.visibility = View.GONE
                txtAudioReset!!.visibility = View.VISIBLE
                //                txtAudioCrop.setBackgroundResource(R.drawable.ic_crop_btn_fill);
                txtAudioCrop!!.visibility = View.VISIBLE
                markerStart!!.setVisibility(View.INVISIBLE)
                markerEnd!!.setVisibility(View.INVISIBLE)
                txtStartPosition!!.visibility = View.INVISIBLE
                txtEndPosition!!.visibility = View.INVISIBLE
            }
        } else if (view === txtAudioReset) {
            audioWaveform!!._setIsDrawBorder(true)
            mPlayer = SamplePlayer(mRecordedSoundFile!!)
            finishOpeningSoundFile(mRecordedSoundFile, 1)
        } else if (view === txtAudioCrop) {

//            txtAudioCrop.setBackgroundResource(R.drawable.ic_crop_btn);
            txtAudioCrop!!.visibility = View.GONE
            txtAudioDone!!.visibility = View.VISIBLE
            txtAudioReset!!.visibility = View.VISIBLE
            audioWaveform!!._setIsDrawBorder(true)
            audioWaveform!!.setBackgroundColor(getResources().getColor(R.color.colorWaveformBg))
            markerStart!!.setVisibility(View.VISIBLE)
            markerEnd!!.setVisibility(View.VISIBLE)
            txtStartPosition!!.visibility = View.VISIBLE
            txtEndPosition!!.visibility = View.VISIBLE
        } else if (view === txtAudioUpload) {
            if (txtAudioDone!!.visibility == View.VISIBLE) {
                if (mIsPlaying) {
                    handlePause()
                }
                saveRingtone(1)
            } else {
                val conData = Bundle()
                conData.putString("INTENT_AUDIO_FILE", mFile!!.absolutePath)
                val intent = Intent()
                intent.putExtras(conData)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    /**
     * Start recording
     */
    private fun startRecording() {
        val listener: SoundFile.ProgressListener = object : SoundFile.ProgressListener {
            override fun reportProgress(elapsedTime: Double): Boolean {
                val now = Utility.currentTime
                if (now - mRecordingLastUpdateTime > 5) {
                    mRecordingTime = elapsedTime
                    // Only UI thread can update Views such as TextViews.
                    runOnUiThread {
                        val min = (mRecordingTime / 60).toInt()
                        val sec = (mRecordingTime - 60 * min).toFloat()
                        txtAudioRecordTime!!.text =
                            String.format(Locale.US, "%02d:%05.2f", min, sec)
                    }
                    mRecordingLastUpdateTime = now
                }
                return mRecordingKeepGoing
            }
        }

        // Record the audio stream in a background thread
        val mRecordAudioThread: Thread = object : Thread() {
            override fun run() {
                try {
                    mRecordedSoundFile = record(listener)
                    if (mRecordedSoundFile == null) {
                        finish()
                        val runnable = Runnable { Log.e("error >> ", "sound file null") }
                        mHandler!!.post(runnable)
                        return
                    }
                    mPlayer = SamplePlayer(mRecordedSoundFile!!)
                } catch (e: Exception) {
                    finish()
                    e.printStackTrace()
                    return
                }
                val runnable = Runnable {
                    audioWaveform!!._setIsDrawBorder(true)
                    finishOpeningSoundFile(mRecordedSoundFile, 0)
                    txtAudioRecord!!.setBackgroundResource(R.drawable.ic_stop_btn1)
                    txtAudioRecordTime!!.visibility = View.INVISIBLE
                    txtStartPosition!!.visibility = View.VISIBLE
                    txtEndPosition!!.visibility = View.VISIBLE
                    markerEnd!!.setVisibility(View.VISIBLE)
                    markerStart!!.setVisibility(View.VISIBLE)
                    llAudioCapture!!.visibility = View.GONE
                    rlAudioEdit!!.visibility = View.VISIBLE
                    txtAudioUpload!!.visibility = View.VISIBLE
                    txtAudioReset!!.visibility = View.VISIBLE
                    txtAudioCrop!!.visibility = View.GONE
                    txtAudioDone!!.visibility = View.VISIBLE
                }
                mHandler!!.post(runnable)
            }
        }
        mRecordAudioThread.start()
    }

    /**
     * After recording finish do necessary steps
     * @param mSoundFile sound file
     * @param isReset isReset
     */
    private fun finishOpeningSoundFile(mSoundFile: SoundFile?, isReset: Int) {
        audioWaveform!!.visibility = View.VISIBLE
        audioWaveform!!._setSoundFile(mSoundFile)
        audioWaveform!!.recomputeHeights(mDensity)
        mMaxPos = audioWaveform!!.maxPos()
        mLastDisplayedStartPos = -1
        mLastDisplayedEndPos = -1
        mTouchDragging = false
        mOffset = 0
        mOffsetGoal = 0
        mFlingVelocity = 0
        resetPositions()
        if (mEndPos > mMaxPos) mEndPos = mMaxPos
        if (isReset == 1) {
            mStartPos = audioWaveform!!.secondsToPixels(0.0)
            mEndPos = audioWaveform!!.secondsToPixels(audioWaveform!!.pixelsToSeconds(mMaxPos))
        }
        if (audioWaveform != null && audioWaveform!!.isInitialized) {
            val seconds = audioWaveform!!.pixelsToSeconds(mMaxPos)
            val min = (seconds / 60).toInt()
            val sec = (seconds - 60 * min).toFloat()
            txtAudioRecordTimeUpdate!!.text =
                String.format(Locale.US, "%02d:%05.2f", min, sec)
        }
        updateDisplay()
    }

    /**
     * Update views
     */
    @Synchronized
    private fun updateDisplay() {
        if (mIsPlaying) {
            val now = mPlayer!!._getCurrentPosition()
            val frames = audioWaveform!!.millisecsToPixels(now)
            audioWaveform!!._setPlayback(frames)
            Log.e("mWidth >> ", "" + mWidth)
            setOffsetGoalNoUpdate(frames - mWidth / 2)
            if (now >= mPlayEndMillSec) {
                handlePause()
            }
        }
        if (!mTouchDragging) {
            var offsetDelta: Int
            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80
                } else {
                    mFlingVelocity = 0
                }
                mOffset += offsetDelta
                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2
                    mFlingVelocity = 0
                }
                if (mOffset < 0) {
                    mOffset = 0
                    mFlingVelocity = 0
                }
                mOffsetGoal = mOffset
            } else {
                offsetDelta = mOffsetGoal - mOffset
                offsetDelta =
                    if (offsetDelta > 10) offsetDelta / 10 else if (offsetDelta > 0) 1 else if (offsetDelta < -10) offsetDelta / 10 else if (offsetDelta < 0) -1 else 0
                mOffset += offsetDelta
            }
        }
        audioWaveform!!._setParameters(mStartPos, mEndPos, mOffset)
        audioWaveform!!.invalidate()
        markerStart!!.setContentDescription(
            " Start Marker" +
                    formatTime(mStartPos)
        )
        markerEnd!!.setContentDescription(
            " End Marker" +
                    formatTime(mEndPos)
        )
        var startX = mStartPos - mOffset - mMarkerLeftInset
        if (startX + markerStart!!.width >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler!!.postDelayed({
                    mStartVisible = true
                    markerStart!!.setAlpha(1f)
                    txtStartPosition!!.setAlpha(1f)
                }, 0)
            }
        } else {
            if (mStartVisible) {
                markerStart!!.setAlpha(0f)
                txtStartPosition!!.setAlpha(0f)
                mStartVisible = false
            }
            startX = 0
        }
        var startTextX = mStartPos - mOffset - mTextLeftInset
        if (startTextX + markerStart!!.width < 0) {
            startTextX = 0
        }
        var endX = mEndPos - mOffset - markerEnd!!.width + mMarkerRightInset
        if (endX + markerEnd!!.width >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler!!.postDelayed({
                    mEndVisible = true
                    markerEnd!!.setAlpha(1f)
                }, 0)
            }
        } else {
            if (mEndVisible) {
                markerEnd!!.setAlpha(0f)
                mEndVisible = false
            }
            endX = 0
        }
        var endTextX = mEndPos - mOffset - txtEndPosition!!.width + mTextRightInset
        if (endTextX + markerEnd!!.width < 0) {
            endTextX = 0
        }
        var params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        //        params.setMargins(
//                startX,
//                mMarkerTopOffset,
//                -markerStart.getWidth(),
//                -markerStart.getHeight());
        params.setMargins(
            startX,
            audioWaveform!!.measuredHeight / 2 + mMarkerTopOffset,
            -markerStart!!.width,
            -markerStart!!.height
        )
        markerStart!!.setLayoutParams(params)
        params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            startTextX,
            mTextTopOffset,
            -txtStartPosition!!.width,
            -txtStartPosition!!.height
        )
        txtStartPosition!!.setLayoutParams(params)
        params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            endX,
            audioWaveform!!.measuredHeight / 2 + mMarkerBottomOffset,
            -markerEnd!!.width,
            -markerEnd!!.height
        )
        //        params.setMargins(
//                endX,
//                audioWaveform.getMeasuredHeight() - markerEnd.getHeight() - mMarkerBottomOffset,
//                -markerEnd.getWidth(),
//                -markerEnd.getHeight());
        markerEnd!!.setLayoutParams(params)
        params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            endTextX,
            audioWaveform!!.measuredHeight - txtEndPosition!!.height - mTextBottomOffset,
            -txtEndPosition!!.width,
            -txtEndPosition!!.height
        )
        txtEndPosition!!.setLayoutParams(params)
    }

    /**
     * Reset all positions
     */
    private fun resetPositions() {
        mStartPos = audioWaveform!!.secondsToPixels(0.0)
        mEndPos = audioWaveform!!.secondsToPixels(15.0)
    }

    private fun setOffsetGoalNoUpdate(offset: Int) {
        if (mTouchDragging) {
            return
        }
        mOffsetGoal = offset
        if (mOffsetGoal + mWidth / 2 > mMaxPos) mOffsetGoal = mMaxPos - mWidth / 2
        if (mOffsetGoal < 0) mOffsetGoal = 0
    }

    private fun formatTime(pixels: Int): String {
        return if (audioWaveform != null && audioWaveform!!.isInitialized) {
            formatDecimal(audioWaveform!!.pixelsToSeconds(pixels))
        } else {
            ""
        }
    }

    private fun formatDecimal(x: Double): String {
        var xWhole = x.toInt()
        var xFrac = (100 * (x - xWhole) + 0.5).toInt()
        if (xFrac >= 100) {
            xWhole++ //Round up
            xFrac -= 100 //Now we need the remainder after the round up
            if (xFrac < 10) {
                xFrac *= 10 //we need a fraction that is 2 digits long
            }
        }
        return if (xFrac < 10) {
            if (xWhole < 10) "0$xWhole.0$xFrac" else "$xWhole.0$xFrac"
        } else {
            if (xWhole < 10) "0$xWhole.$xFrac" else "$xWhole.$xFrac"
        }
    }

    private fun trap(pos: Int): Int {
        if (pos < 0) return 0
        return if (pos > mMaxPos) mMaxPos else pos
    }

    private fun setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2)
    }

    private fun setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2)
    }

    private fun setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2)
    }

    private fun setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2)
    }

    private fun setOffsetGoal(offset: Int) {
        setOffsetGoalNoUpdate(offset)
        updateDisplay()
    }

    override fun markerDraw() {}
    override fun markerTouchStart(marker: MarkerView, pos: Float) {
        mTouchDragging = true
        mTouchStart = pos
        mTouchInitialStartPos = mStartPos
        mTouchInitialEndPos = mEndPos
        handlePause()
    }

    override fun markerTouchMove(marker: MarkerView, x: Float) {
        val delta = x - mTouchStart
        if (marker === markerStart) {
            mStartPos = trap((mTouchInitialStartPos + delta).toInt())
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
        } else {
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
            if (mEndPos < mStartPos) mEndPos = mStartPos
        }
        updateDisplay()
    }

    override fun markerTouchEnd(marker: MarkerView) {
        mTouchDragging = false
        if (marker === markerStart) {
            setOffsetGoalStart()
        } else {
            setOffsetGoalEnd()
        }
    }

    override fun markerLeft(marker: MarkerView, velocity: Int) {
        mKeyDown = true
        if (marker === markerStart) {
            val saveStart = mStartPos
            mStartPos = trap(mStartPos - velocity)
            mEndPos = trap(mEndPos - (saveStart - mStartPos))
            setOffsetGoalStart()
        }
        if (marker === markerEnd) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity)
                mEndPos = mStartPos
            } else {
                mEndPos = trap(mEndPos - velocity)
            }
            setOffsetGoalEnd()
        }
        updateDisplay()
    }

    override fun markerRight(marker: MarkerView, velocity: Int) {
        mKeyDown = true
        if (marker === markerStart) {
            val saveStart = mStartPos
            mStartPos += velocity
            if (mStartPos > mMaxPos) mStartPos = mMaxPos
            mEndPos += mStartPos - saveStart
            if (mEndPos > mMaxPos) mEndPos = mMaxPos
            setOffsetGoalStart()
        }
        if (marker === markerEnd) {
            mEndPos += velocity
            if (mEndPos > mMaxPos) mEndPos = mMaxPos
            setOffsetGoalEnd()
        }
        updateDisplay()
    }

    override fun markerEnter(marker: MarkerView) {}
    override fun markerKeyUp() {
        mKeyDown = false
        updateDisplay()
    }

    override fun markerFocus(marker: MarkerView) {
        mKeyDown = false
        if (marker === markerStart) {
            setOffsetGoalStartNoUpdate()
        } else {
            setOffsetGoalEndNoUpdate()
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler!!.postDelayed({ updateDisplay() }, 100)
    }
    //
    // WaveformListener
    //
    /**
     * Every time we get a message that our waveform drew, see if we need to
     * animate and trigger another redraw.
     */
    override fun waveformDraw() {
        mWidth = audioWaveform!!.measuredWidth
        if (mOffsetGoal != mOffset && !mKeyDown) updateDisplay() else if (mIsPlaying) {
            updateDisplay()
        } else if (mFlingVelocity != 0) {
            updateDisplay()
        }
    }

    override fun waveformTouchStart(x: Float) {
        mTouchDragging = true
        mTouchStart = x
        mTouchInitialOffset = mOffset
        mFlingVelocity = 0
        //        long mWaveformTouchStartMsec = Utility.getCurrentTime();
    }

    override fun waveformTouchMove(x: Float) {
        mOffset = trap((mTouchInitialOffset + (mTouchStart - x)).toInt())
        updateDisplay()
    }

    override fun waveformTouchEnd() {
        /*mTouchDragging = false;
        mOffsetGoal = mOffset;

        long elapsedMsec = Utility.Utility.getCurrentTime() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = audioWaveform.pixelsToMillisecs(
                        (int) (mTouchStart + mOffset));
                if (seekMsec >= mPlayStartMsec &&
                        seekMsec < mPlayEndMillSec) {
                    mPlayer.seekTo(seekMsec);
                } else {
//                    handlePause();
                }
            } else {
                onPlay((int) (mTouchStart + mOffset));
            }
        }*/
    }

    @Synchronized
    private fun handlePause() {
        txtAudioPlay!!.setBackgroundResource(R.drawable.ic_play_btn)
        if (mPlayer != null && mPlayer!!.isPlaying) {
            mPlayer!!.pause()
        }
        audioWaveform!!._setPlayback(-1)
        mIsPlaying = false
    }

    @Synchronized
    private fun onPlay(startPosition: Int) {
        if (mIsPlaying) {
            handlePause()
            return
        }
        if (mPlayer == null) {
            // Not initialized yet
            return
        }
        try {
            val mPlayStartMsec = audioWaveform!!.pixelsToMillisecs(startPosition)
            mPlayEndMillSec = if (startPosition < mStartPos) {
                audioWaveform!!.pixelsToMillisecs(mStartPos)
            } else if (startPosition > mEndPos) {
                audioWaveform!!.pixelsToMillisecs(mMaxPos)
            } else {
                audioWaveform!!.pixelsToMillisecs(mEndPos)
            }
            mPlayer!!._setOnCompletionListener(object : SamplePlayer.OnCompletionListener {
                override fun onCompletion() {
                    handlePause()
                }
            })
            mIsPlaying = true
            mPlayer!!.seekTo(mPlayStartMsec)
            mPlayer!!.start()
            updateDisplay()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun waveformFling(vx: Float) {
        mTouchDragging = false
        mOffsetGoal = mOffset
        mFlingVelocity = (-vx).toInt()
        updateDisplay()
    }

    override fun waveformZoomIn() {
        /*audioWaveform.zoomIn();
        mStartPos = audioWaveform.getStart();
        mEndPos = audioWaveform.getEnd();
        mMaxPos = audioWaveform.maxPos();
        mOffset = audioWaveform.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();*/
    }

    override fun waveformZoomOut() {
        /*audioWaveform.zoomOut();
        mStartPos = audioWaveform.getStart();
        mEndPos = audioWaveform.getEnd();
        mMaxPos = audioWaveform.maxPos();
        mOffset = audioWaveform.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();*/
    }

    /**
     * Save sound file as ringtone
     * @param finish flag for finish
     */
    private fun saveRingtone(finish: Int) {
        val startTime = audioWaveform!!.pixelsToSeconds(mStartPos)
        val endTime = audioWaveform!!.pixelsToSeconds(mEndPos)
        val startFrame = audioWaveform!!.secondsToFrames(startTime)
        val endFrame = audioWaveform!!.secondsToFrames(endTime - 0.04)
        val duration = (endTime - startTime + 0.5).toInt()

        // Create an indeterminate progress dialog
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        mProgressDialog!!.setTitle("Saving....")
        mProgressDialog!!.isIndeterminate = true
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.show()

        // Save the sound file in a background thread
        val mSaveSoundFileThread: Thread = object : Thread() {
            override fun run() {
                // Try AAC first.
                val outPath = makeRingtoneFilename("AUDIO_TEMP", Utility.AUDIO_FORMAT)
                if (outPath == null) {
                    Log.e(" >> ", "Unable to find unique filename")
                    return
                }
                val outFile = File(outPath)
                try {
                    // Write the new file
                    mRecordedSoundFile!!.writeFile(outFile, startFrame, endFrame - startFrame)
                } catch (e: Exception) {
                    // log the error and try to create a .wav file instead
                    if (outFile.exists()) {
                        outFile.delete()
                    }
                    e.printStackTrace()
                }
                mProgressDialog!!.dismiss()
                val finalOutPath: String = outPath
                val runnable = Runnable {
                    afterSavingRingtone(
                        "AUDIO_TEMP",
                        finalOutPath,
                        duration, finish
                    )
                }
                mHandler!!.post(runnable)
            }
        }
        mSaveSoundFileThread.start()
    }

    /**
     * After saving as ringtone set its content values
     * @param title title
     * @param outPath output path
     * @param duration duration of file
     * @param finish flag for finish
     */
    private fun afterSavingRingtone(
        title: CharSequence,
        outPath: String,
        duration: Int, finish: Int
    ) {
        val outFile = File(outPath)
        val fileSize = outFile.length()
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DATA, outPath)
        values.put(MediaStore.MediaColumns.TITLE, title.toString())
        values.put(MediaStore.MediaColumns.SIZE, fileSize)
        values.put(MediaStore.MediaColumns.MIME_TYPE, Utility.AUDIO_MIME_TYPE)
        values.put(MediaStore.Audio.Media.ARTIST, applicationInfo.name)
        values.put(MediaStore.Audio.Media.DURATION, duration)
        values.put(MediaStore.Audio.Media.IS_MUSIC, true)
        val uri = MediaStore.Audio.Media.getContentUriForPath(outPath)
        val newUri = contentResolver.insert(uri!!, values)
        Log.e("final URI >> ", newUri.toString() + " >> " + outPath)
        if (finish == 0) {
            loadFromFile(outPath)
        } else if (finish == 1) {
            val conData = Bundle()
            conData.putString("INTENT_AUDIO_FILE", outPath)
            val intent = intent
            intent.putExtras(conData)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    /**
     * Generating name for ringtone
     * @param title title of file
     * @param extension extension for file
     * @return filename
     */
    private fun makeRingtoneFilename(title: CharSequence, extension: String): String? {
        var externalRootDir = Environment.getExternalStorageDirectory().path
        if (!externalRootDir.endsWith("/")) {
            externalRootDir += "/"
        }
        val subDir = "media/audio/music/"
        var parentDir = externalRootDir + subDir

        // Create the parent directory
        val parentDirFile = File(parentDir)
        parentDirFile.mkdirs()

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory()) {
            parentDir = externalRootDir
        }

        // Turn the title into a filename
        var filename = ""
        for (i in title.indices) {
            if (Character.isLetterOrDigit(title[i])) {
                filename += title[i]
            }
        }

        // Try to make the filename unique
        var path: String? = null
        for (i in 0..99) {
            val testPath =
                if (i > 0)
                    parentDir + filename + i + extension
                else parentDir + filename + extension
            try {
                val f = RandomAccessFile(File(testPath), "r")
                f.close()
            } catch (e: Exception) {
                // Good, the file didn't exist
                path = testPath
                break
            }
        }
        return path
    }

    /**
     * Load file from path
     * @param mFilename file name
     */
    private fun loadFromFile(mFilename: String) {
        mFile = File(mFilename)
        //        SongMetadataReader metadataReader = new SongMetadataReader(this, mFilename);
        mLoadingLastUpdateTime = Utility.currentTime
        mLoadingKeepGoing = true
        mProgressDialog = ProgressDialog(this)
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog!!.setTitle("Loading ...")
        mProgressDialog!!.show()
        val listener: SoundFile.ProgressListener = object : SoundFile.ProgressListener {
            override fun reportProgress(fractionComplete: Double): Boolean {
                val now = Utility.currentTime
                if (now - mLoadingLastUpdateTime > 100) {
                    mProgressDialog!!.progress =
                        (mProgressDialog!!.getMax() * fractionComplete).toInt()
                    mLoadingLastUpdateTime = now
                }
                return mLoadingKeepGoing
            }
        }

        // Load the sound file in a background thread
        val mLoadSoundFileThread: Thread = object : Thread() {
            override fun run() {
                try {
                    mLoadedSoundFile = create(mFile!!.absolutePath, listener)
                    if (mLoadedSoundFile == null) {
                        mProgressDialog!!.dismiss()
                        val name = mFile!!.getName().lowercase(Locale.getDefault())
                        val components = name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        val err =
                            if (components.size < 2) "No Extension"
                            else "Bad Extension"
                        Log.e(" >> ", "" + err)
                        return
                    }
                    mPlayer = SamplePlayer(mLoadedSoundFile!!)
                } catch (e: Exception) {
                    mProgressDialog!!.dismiss()
                    e.printStackTrace()
                    return
                }
                mProgressDialog!!.dismiss()
                if (mLoadingKeepGoing) {
                    val runnable = Runnable {
                        audioWaveform!!.visibility = View.INVISIBLE
                        audioWaveform!!.setBackgroundColor(getResources().getColor(R.color.waveformUnselectedBackground))
                        audioWaveform!!._setIsDrawBorder(false)
                        finishOpeningSoundFile(mLoadedSoundFile, 0)
                    }
                    mHandler!!.post(runnable)
                }
            }
        }
        mLoadSoundFileThread.start()
    }
}
