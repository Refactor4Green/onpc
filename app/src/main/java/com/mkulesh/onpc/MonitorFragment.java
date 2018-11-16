/*
 * Copyright (C) 2018. Mikhail Kulesh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General
 * Public License along with this program.
 */

package com.mkulesh.onpc;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mkulesh.onpc.iscp.messages.AmpOperationCommandMsg;
import com.mkulesh.onpc.iscp.messages.AudioMutingMsg;
import com.mkulesh.onpc.iscp.messages.MasterVolumeMsg;
import com.mkulesh.onpc.iscp.messages.MenuStatusMsg;
import com.mkulesh.onpc.iscp.messages.OperationCommandMsg;
import com.mkulesh.onpc.iscp.messages.PlayStatusMsg;
import com.mkulesh.onpc.iscp.messages.TimeSeekMsg;
import com.mkulesh.onpc.iscp.messages.TrackInfoMsg;
import com.mkulesh.onpc.utils.Logging;
import com.mkulesh.onpc.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MonitorFragment extends BaseFragment
{
    private AppCompatImageButton btnRepeat;
    private AppCompatImageButton btnPrevious;
    private AppCompatImageButton btnPausePlay;
    private AppCompatImageButton btnNext;
    private AppCompatImageButton btnRandom;
    private final List<AppCompatImageButton> cmdButtons = new ArrayList<>();
    private final List<AppCompatImageButton> soundControlButtons = new ArrayList<>();
    private ImageView cover;
    private AppCompatSeekBar seekBar;

    public MonitorFragment()
    {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        initializeFragment(inflater, container, R.layout.monitor_fragment);
        rootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Command Buttons
        btnRepeat = rootView.findViewById(R.id.btn_repeat);
        cmdButtons.add(btnRepeat);

        btnPrevious = rootView.findViewById(R.id.btn_previous);
        cmdButtons.add(btnPrevious);

        final AppCompatImageButton btnStop = rootView.findViewById(R.id.btn_stop);
        cmdButtons.add(btnStop);

        btnPausePlay = rootView.findViewById(R.id.btn_pause_play);
        cmdButtons.add(btnPausePlay);

        btnNext = rootView.findViewById(R.id.btn_next);
        cmdButtons.add(btnNext);

        btnRandom = rootView.findViewById(R.id.btn_random);
        cmdButtons.add(btnRandom);

        for (AppCompatImageButton b : cmdButtons)
        {
            final OperationCommandMsg msg = new OperationCommandMsg((String) (b.getTag()));
            prepareButton(b, msg, msg.getCommand().getImageId(), msg.getCommand().getDescriptionId());
            setButtonEnabled(b, false);
        }

        // Amplifier command buttons
        LinearLayout soundControlLayout = rootView.findViewById(R.id.sound_control_layout);
        final String defaultSoundControl = preferences.getString(SettingsActivity.SOUND_CONTROL,
                activity.getResources().getString(R.string.pref_default_sound_control));
        switch (defaultSoundControl)
        {
            case "none":
                soundControlLayout.setVisibility(View.GONE);
                break;
            case "external-amplifier":
                soundControlLayout.setVisibility(View.VISIBLE);
                setSoundControlAmplifier(soundControlLayout);
                break;
            case "device":
                soundControlLayout.setVisibility(View.VISIBLE);
                setSoundControlDevice(soundControlLayout);
                break;
        }

        cover = rootView.findViewById(R.id.tv_cover);
        seekBar = rootView.findViewById(R.id.progress_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar)
            {
                // empty
            }

            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if (activity.getStateManager() != null)
                {
                    seekTime(progressChanged);
                }
            }
        });

        update(null, null);
        return rootView;
    }

    private void setSoundControlAmplifier(LinearLayout soundControlLayout)
    {
        final AmpOperationCommandMsg.Command[] commands = new AmpOperationCommandMsg.Command[]
        {
            AmpOperationCommandMsg.Command.SLIDOWN,
            AmpOperationCommandMsg.Command.AMTTG,
            AmpOperationCommandMsg.Command.MVLDOWN,
            AmpOperationCommandMsg.Command.MVLUP
        };
        for (AmpOperationCommandMsg.Command c : commands)
        {
            final AmpOperationCommandMsg msg = new AmpOperationCommandMsg(c.getCode());
            soundControlButtons.add(createButton(
                    msg.getCommand().getImageId(), msg.getCommand().getDescriptionId(),
                    msg, msg.getCommand().getCode(),
                    selectorButtonMargin, selectorButtonMargin));
        }
        for (AppCompatImageButton b : soundControlButtons)
        {
            soundControlLayout.addView(b);
            setButtonEnabled(b, false);
        }
    }

    public void setSoundControlDevice(LinearLayout soundControlLayout)
    {
        // audio muting
        {
            final AudioMutingMsg msg = new AudioMutingMsg(AudioMutingMsg.Status.TOGGLE);
            soundControlButtons.add(createButton(
                    R.drawable.volume_amp_muting, msg.getStatus().getDescriptionId(),
                    msg, msg.getStatus().getCode(),
                    selectorButtonMargin, selectorButtonMargin));
        }
        // volume down
        {
            final MasterVolumeMsg msg = new MasterVolumeMsg(MasterVolumeMsg.Command.DOWN);
            soundControlButtons.add(createButton(
                    msg.getCommand().getImageId(), msg.getCommand().getDescriptionId(),
                    msg, msg.getCommand().getCode(),
                    selectorButtonMargin, selectorButtonMargin));
        }
        // volume up
        {
            final MasterVolumeMsg msg = new MasterVolumeMsg(MasterVolumeMsg.Command.UP);
            soundControlButtons.add(createButton(
                    msg.getCommand().getImageId(), msg.getCommand().getDescriptionId(),
                    msg, msg.getCommand().getCode(),
                    selectorButtonMargin, selectorButtonMargin));
        }
        for (AppCompatImageButton b : soundControlButtons)
        {
            soundControlLayout.addView(b);
            setButtonEnabled(b, false);
        }
    }

    @Override
    protected void updateStandbyView(@Nullable final State state, @NonNull final HashSet<State.ChangeType> eventChanges)
    {
        ((TextView) rootView.findViewById(R.id.tv_time_start)).setText(
                activity.getResources().getString(R.string.tv_time_default));
        ((TextView) rootView.findViewById(R.id.tv_time_end)).setText(
                activity.getResources().getString(R.string.tv_time_default));

        TextView track = rootView.findViewById(R.id.tv_track);
        track.setText("");
        track.setCompoundDrawables(null, null, null, null);

        ((TextView) rootView.findViewById(R.id.tv_album)).setText("");
        ((TextView) rootView.findViewById(R.id.tv_artist)).setText("");
        ((TextView) rootView.findViewById(R.id.tv_title)).setText("");
        ((TextView) rootView.findViewById(R.id.tv_file_format)).setText("");
        cover.setImageResource(R.drawable.empty_cover);
        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        for (AppCompatImageButton b : soundControlButtons)
        {
            setButtonEnabled(b, state != null);
        }
        for (AppCompatImageButton b : cmdButtons)
        {
            setButtonEnabled(b, false);
        }
    }

    @Override
    protected void updateActiveView(@NonNull final State state, @NonNull final HashSet<State.ChangeType> eventChanges)
    {
        // time seek only
        if (eventChanges.size() == 1 && eventChanges.contains(State.ChangeType.TIME_SEEK))
        {
            updateProgressBar(state);
            return;
        }

        Logging.info(this, "Updating playback monitor");
        // Text
        ((TextView) rootView.findViewById(R.id.tv_album)).setText(state.album);
        ((TextView) rootView.findViewById(R.id.tv_artist)).setText(state.artist);
        ((TextView) rootView.findViewById(R.id.tv_title)).setText(state.title);
        ((TextView) rootView.findViewById(R.id.tv_file_format)).setText(state.fileFormat);

        // service icon and track
        {
            final TextView track = rootView.findViewById(R.id.tv_track);
            final Drawable bg = Utils.getDrawable(activity, state.serviceIcon.getImageId());
            Utils.setDrawableColorAttr(activity, bg, android.R.attr.textColorSecondary);
            track.setCompoundDrawablesWithIntrinsicBounds(bg, null, null, null);
            track.setText(TrackInfoMsg.getTrackInfo(state.currentTrack, state.maxTrack));
        }

        // cover
        if (state.cover == null)
        {
            cover.setImageResource(R.drawable.empty_cover);
        }
        else
        {
            cover.setImageBitmap(state.cover);
        }

        // progress bar
        updateProgressBar(state);

        // buttons
        for (AppCompatImageButton b : soundControlButtons)
        {
            setButtonEnabled(b, true);
        }
        for (AppCompatImageButton b : cmdButtons)
        {
            setButtonEnabled(b, true);
        }

        if (state.repeatStatus == PlayStatusMsg.RepeatStatus.DISABLE)
        {
            setButtonEnabled(btnRepeat, false);
        }
        else
        {
            setButtonEnabled(btnRepeat, true);
            setButtonSelected(btnRepeat, state.repeatStatus != PlayStatusMsg.RepeatStatus.OFF);
        }

        if (state.shuffleStatus == PlayStatusMsg.ShuffleStatus.DISABLE)
        {
            setButtonEnabled(btnRandom, false);
        }
        else
        {
            setButtonEnabled(btnRandom, true);
            setButtonSelected(btnRandom, state.shuffleStatus != PlayStatusMsg.ShuffleStatus.OFF);
        }

        setButtonEnabled(btnPrevious, state.isPlaying());
        setButtonEnabled(btnNext, state.isPlaying());

        switch (state.playStatus)
        {
        case STOP:
            btnPausePlay.setImageResource(R.drawable.cmd_play);
            break;
        case PLAY:
            btnPausePlay.setImageResource(R.drawable.cmd_pause);
            break;
        case PAUSE:
            btnPausePlay.setImageResource(R.drawable.cmd_play);
            break;
        default:
            break;
        }
        setButtonEnabled(btnPausePlay, state.isOn());
    }

    private void updateProgressBar(@NonNull final State state)
    {
        ((TextView) rootView.findViewById(R.id.tv_time_start)).setText(state.currentTime);
        ((TextView) rootView.findViewById(R.id.tv_time_end)).setText(state.maxTime);
        final int currTime = Utils.timeToSeconds(state.currentTime);
        final int maxTime = Utils.timeToSeconds(state.maxTime);
        if (currTime >= 0 && maxTime >= 0)
        {
            seekBar.setMax(maxTime);
            seekBar.setProgress(currTime);
        }
        else
        {
            seekBar.setMax(1000);
            seekBar.setProgress(0);
        }
        seekBar.setEnabled(state.isPlaying() && state.timeSeek == MenuStatusMsg.TimeSeek.ENABLE);
    }

    private void seekTime(int newSec)
    {
        final State state = activity.getStateManager().getState();
        final int currTime = Utils.timeToSeconds(state.currentTime);
        final int maxTime = Utils.timeToSeconds(state.maxTime);
        if (currTime >= 0 && maxTime >= 0)
        {
            final int hour = newSec / 3600;
            final int min = (newSec - hour * 3600) / 60;
            final int sec = newSec - hour * 3600 - min * 60;
            activity.getStateManager().requestSkipNextTimeMsg(2);
            final TimeSeekMsg msg = new TimeSeekMsg(hour, min, sec);
            state.currentTime = msg.getTimeAsString();
            ((TextView) rootView.findViewById(R.id.tv_time_start)).setText(state.currentTime);
            activity.getStateManager().sendMessage(msg);
        }
    }
}
