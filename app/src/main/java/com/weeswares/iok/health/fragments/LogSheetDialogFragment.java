package com.weeswares.iok.health.fragments;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.weeswares.iok.health.R;
import com.weeswares.iok.health.databinding.FragmentLogSheetBinding;
import com.weeswares.iok.health.databinding.FragmentLogSheetDialogBinding;
import com.weeswares.iok.health.helpers.Logger;

import java.util.ArrayList;
import java.util.List;

public class LogSheetDialogFragment extends BottomSheetDialogFragment {

    private FragmentLogSheetBinding binding;

    public static LogSheetDialogFragment newInstance() {
        final LogSheetDialogFragment fragment = new LogSheetDialogFragment();
        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().setOnShowListener(dialogInterface -> {
            View bottomSheetInternal = ((BottomSheetDialog) dialogInterface).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheetInternal).setPeekHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_log_sheet, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final RecyclerView recyclerView = binding.list;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        StringAdapter stringAdapter = new StringAdapter();
        recyclerView.setAdapter(stringAdapter);
        stringAdapter.setLogs(Logger.getProcessLog(getActivity()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView text;

        ViewHolder(FragmentLogSheetDialogBinding binding) {
            super(binding.getRoot());
            text = binding.text;
        }

    }

    private static class StringAdapter extends RecyclerView.Adapter<ViewHolder> {

        private List<String> logs = new ArrayList<>();

        StringAdapter() {
        }

        public void setLogs(List<String> logs) {
            Log.d("LogFragment", "setLogs: log list size=" + logs.size());
            this.logs = logs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            return new ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.fragment_log_sheet_dialog, parent, false));

        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.text.setText(logs.get(position));
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

    }
}