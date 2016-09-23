package com.example.alexey.dictaphone;


import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class PlayListFragment extends Fragment {


    public PlayListFragment() {
        // Required empty public constructor
    }

    final String ATTRIBUTE_NAME = "fileName";
    final String ATTRIBUTE_NUMBER = "fileNumber";
    final String ATTRIBUTE_DATE = "fileDate";
    final String ATTRIBUTE_DURATION = "fileDuration";
    final String ATTRIBUTE_HIDDEN_INFO = "hiddenInfo";
    ListView playList;

    final String[] regTemplates = {"^\\d{4}\\|\\d{2}\\|\\d{2}-\\d{2}:\\d{2}:\\d{2}-.*\\(\\+?\\d{7,12}\\)(:((In)|(Out)))?\\.3gp$",
            "^\\d{4}\\|\\d{2}\\|\\d{2}-\\d{2}:\\d{2}:\\d{2}-.*\\(\\+?\\d{7,12}\\):In\\.3gp$",
            "^\\d{4}\\|\\d{2}\\|\\d{2}-\\d{2}:\\d{2}:\\d{2}-.*\\(\\+?\\d{7,12}\\):Out\\.3gp$"};

    static final String ARGUMENT_PAGE_NUMBER = "arg_page_number";
    int pageNumber;

    static PlayListFragment newInstance(int page) {
        PlayListFragment playListFragment = new PlayListFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(ARGUMENT_PAGE_NUMBER, page);
        playListFragment.setArguments(arguments);
        return playListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments().getInt(ARGUMENT_PAGE_NUMBER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_play_list, container, false);

        File directory = new File(MainActivity.FILES_PATH);
        if (directory.exists() && directory.isDirectory()) {
            playList = (ListView) view.findViewById(R.id.playList);
            List<Map<String, Object>> data = new LinkedList<>();

            String[] from = {ATTRIBUTE_NAME, ATTRIBUTE_NUMBER, ATTRIBUTE_DATE, ATTRIBUTE_DURATION, ATTRIBUTE_HIDDEN_INFO};
            int[] to = {R.id.fileName, R.id.fileNumber, R.id.fileDate, R.id.fileDuration, R.id.hiddenInfo};

            HashMap<String, Object> map;
            for (File file : directory.listFiles()) {
                String fileName = file.getName();
                if (fileName.matches(regTemplates[pageNumber])) {
                    map = new HashMap<>();
                    String attrName = fileName.substring(fileName.indexOf("-", fileName.indexOf("-") + 1) + 1, fileName.lastIndexOf("("));
                    map.put(ATTRIBUTE_NAME, attrName.trim().isEmpty() ? "Неизвестный" : attrName);
                    map.put(ATTRIBUTE_NUMBER, fileName.substring(fileName.lastIndexOf("(") + 1, fileName.lastIndexOf(")")));
                    map.put(ATTRIBUTE_DATE, new SimpleDateFormat("HH:mm:ss\ndd.MM.yy").format(new Date(file.lastModified())));
                    int duration = MediaPlayer.create(getActivity(), Uri.parse(MainActivity.FILES_PATH + "/" + file.getName())).getDuration() / 1000;
                    String hours = (duration / 3600) > 9 ? "" + (duration / 3600) : "0" + (duration / 3600);
                    String minutes = ((duration / 60) % 60) > 9 ? "" + ((duration / 60) % 60) : "0" + ((duration / 60) % 60);
                    String seconds = (duration % 60) > 9 ? "" + (duration % 60) : "0" + (duration % 60);
                    map.put(ATTRIBUTE_DURATION, hours + ":" + minutes + ":" + seconds);
                    map.put(ATTRIBUTE_HIDDEN_INFO, MainActivity.FILES_PATH + "/" + file.getName());
                    data.add(0, map);
                }
            }
            SimpleAdapter simpleAdapter = new SimpleAdapter(getActivity(), data, R.layout.item_play_list, from, to);
            playList.setAdapter(simpleAdapter);

            playList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    getActivity().startService(new Intent(getActivity(), PlayService.class)
                            .putExtra(BlankFragment.COMMAND, BlankFragment.PLAY)
                            .putExtra(MainActivity.PATH_TO_FILE, ((TextView) view.findViewById(R.id.hiddenInfo)).getText()));
                    BlankFragment.playOrPause.setImageResource(android.R.drawable.ic_media_pause);
                    BlankFragment.nowState = BlankFragment.PlayOrPauseState.Pause;
                }
            });
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (playList != null)
            switch (pageNumber) {
                case 0: {
                    playList.setSelection(BlankFragment.listFirstPosition);
                    break;
                }
                case 1: {
                    playList.setSelection(BlankFragment.listSecondPosition);
                    break;
                }
                case 2: {
                    playList.setSelection(BlankFragment.listThirdPosition);
                    break;
                }
            }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (playList != null)
            switch (pageNumber) {
                case 0: {
                    BlankFragment.listFirstPosition = playList.getFirstVisiblePosition();
                    break;
                }
                case 1: {
                    BlankFragment.listSecondPosition = playList.getFirstVisiblePosition();
                    break;
                }
                case 2: {
                    BlankFragment.listThirdPosition = playList.getFirstVisiblePosition();
                    break;
                }
            }
    }
}
