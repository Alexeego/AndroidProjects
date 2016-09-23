package com.example.alexey.dictaphone;


import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;


/**
 * A simple {@link Fragment} subclass.
 */
public class ServiceManageFragment extends Fragment {


    public ServiceManageFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_service_manage, container, false);
        Button startServiceManager = (Button)view.findViewById(R.id.buttonStartServiceManager);
        startServiceManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    Toast.makeText(getActivity(), "state sd-card is not available", Toast.LENGTH_SHORT).show();
                } else {

                    getActivity().startService(new Intent(getActivity(), RecordingService.class));
                }
            }
        });
        Button stopServiceManager = (Button)view.findViewById(R.id.buttonStopServiceManager);
        stopServiceManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "dictaphone stop", Toast.LENGTH_SHORT).show();
                getActivity().stopService(new Intent(getActivity(), RecordingService.class));
            }
        });
        return view;
    }

}
