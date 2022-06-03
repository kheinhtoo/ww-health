package com.weeswares.iok.health.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.weeswares.iok.health.R;
import com.weeswares.iok.health.databinding.FragmentOutputBinding;
import com.weeswares.iok.health.helpers.Bluetooth;

public class OutputFragment extends Fragment {

    private static final String ARG_PARAM1 = "device";
    private static final String ARG_PARAM2 = "title";

    private Bluetooth bluetooth;
    private String title;

    private FragmentOutputBinding binding;

    public OutputFragment() {
        // Required empty public constructor
    }

    public static OutputFragment newInstance(Bluetooth b,String title) {
        OutputFragment fragment = new OutputFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, b);
        args.putString(ARG_PARAM2, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bluetooth = (Bluetooth) getArguments().getSerializable(ARG_PARAM1);
            title = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_output, container, false);
        binding.setDevice(bluetooth);
        binding.setTitle(title);
        return binding.getRoot();
    }
}