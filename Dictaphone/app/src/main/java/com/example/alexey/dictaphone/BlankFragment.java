package com.example.alexey.dictaphone;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 */
public class BlankFragment extends Fragment {


    public BlankFragment() {
        // Required empty public constructor
    }

    ViewPager viewPager;
    PagerAdapter pagerAdapter;


    static int duration = 0;
    static final PlayService.MyOnSeekBarChangeListener onSeekBarChangeListener = new PlayService.MyOnSeekBarChangeListener();
    static PlayOrPauseState nowState = PlayOrPauseState.Play;

    enum PlayOrPauseState{
        Play,
        Pause
    }

    final static String PLAY = "play";
    final static String PAUSE = "pause";
    final static String CONTINUE = "continue";
    final static String COMMAND = "command";

    static volatile SeekBar seekBar = null;
    static volatile ImageButton playOrPause = null;
    static int listFirstPosition = 0;
    static int listSecondPosition = 0;
    static int listThirdPosition = 0;

    static int currentTab = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank, container, false);
        viewPager = (ViewPager) view.findViewById(R.id.pager);
        pagerAdapter = new NewFragmentPagerAdapter(getActivity().getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekBar.setMax(duration);
        seekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        playOrPause = (ImageButton) view.findViewById(R.id.playOrPause);
        if (PlayOrPauseState.Play == nowState)
            playOrPause.setImageResource(android.R.drawable.ic_media_play);
        else if (PlayOrPauseState.Pause == nowState)
            playOrPause.setImageResource(android.R.drawable.ic_media_pause);
        playOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayService.playing) {
                    if (!PlayService.pause) {
                        getActivity().startService(new Intent(getActivity(), PlayService.class)
                                .putExtra(COMMAND, PAUSE));
                        playOrPause.setImageResource(android.R.drawable.ic_media_play);
                        nowState = PlayOrPauseState.Play;
                    } else {
                        getActivity().startService(new Intent(getActivity(), PlayService.class)
                                .putExtra(COMMAND, CONTINUE));
                        playOrPause.setImageResource(android.R.drawable.ic_media_pause);
                        nowState = PlayOrPauseState.Pause;
                    }
                }
            }
        });
            handler = new Handler();
            handler.postDelayed(runnable, 10);

        return view;
    }


    Handler handler = null;
    static volatile boolean startPlaying = false;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (startPlaying) {
                if (!PlayService.playing) {
                    startPlaying = false;
                    playOrPause.setImageResource(android.R.drawable.ic_media_play);
                }
            } else {
                if (PlayService.playing) {
                    startPlaying = true;
                }
            }
            handler.postDelayed(runnable, 10);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null)
            handler.removeCallbacks(runnable);

    }


    private class NewFragmentPagerAdapter extends FragmentStatePagerAdapter {
        public NewFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlayListFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Все звонки";
                case 1:
                    return "Входящие";
                case 2:
                    return "Исходящие";
            }
            return "Что-то странное";
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewPager != null)
            viewPager.setCurrentItem(currentTab);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (viewPager != null)
            currentTab = viewPager.getCurrentItem();
    }
}
