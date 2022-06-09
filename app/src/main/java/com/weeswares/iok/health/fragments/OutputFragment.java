package com.weeswares.iok.health.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.weeswares.iok.health.MainActivity;
import com.weeswares.iok.health.R;
import com.weeswares.iok.health.databinding.FragmentOutputBinding;
import com.weeswares.iok.health.helpers.Bluetooth;
import com.weeswares.iok.health.interfaces.ResultParser;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.CompositeDisposable;

public class OutputFragment extends Fragment {
    private static final String TAG = OutputFragment.class.toString();

    private static final String ARG_PARAM1 = "icon";
    private static final String ARG_PARAM2 = "title";
    private static final String ARG_PARAM3 = "characteristic_id";
    private static final String ARG_PARAM4 = "notification_support";
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Bluetooth bluetooth;
    private String title;
    private UUID characteristicID;
    private boolean isNotificationSupported;
    private FragmentOutputBinding binding;
    private RxBleClient rxBleClient;
    private RxBleConnection.RxBleConnectionState rxBleConnectionState;
    private ResultParser resultParser;
    private int icon;

    public OutputFragment() {
        // Required empty public constructor
    }

    public static OutputFragment newInstance(String title, String charID, boolean notificationSupport, int icon) {
        OutputFragment fragment = new OutputFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, icon);
        args.putString(ARG_PARAM2, title);
        args.putString(ARG_PARAM3, charID);
        args.putBoolean(ARG_PARAM4, notificationSupport);
        fragment.setArguments(args);
        return fragment;
    }

    public void setResultParser(ResultParser resultParser) {
        this.resultParser = resultParser;
    }

    public void setBluetooth(Bluetooth bluetooth) {
        this.bluetooth = bluetooth;
        connect(bluetooth, getActivity());
    }

    public Bluetooth getBluetooth() {
        return bluetooth;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            icon = getArguments().getInt(ARG_PARAM1);
            title = getArguments().getString(ARG_PARAM2);
            characteristicID = UUID.fromString(getArguments().getString(ARG_PARAM3));
            isNotificationSupported = getArguments().getBoolean(ARG_PARAM4);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_output, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setTitle(title);
        binding.setDevice(bluetooth);
        binding.setValue("--");
        binding.setIcon(icon);
        init(getActivity());
    }

    @Override
    public void onStop() {
        disposables.clear();
        super.onStop();
    }

    private void init(Context context) {
        rxBleClient = RxBleClient.create(context);
    }

    void connect(Bluetooth b, Activity context) {
        if (b == null) return;
        if (this.resultParser == null) {
            Toast.makeText(getActivity(), "Result parser is required.", Toast.LENGTH_SHORT).show();
            return;
        }
        RxBleDevice device = rxBleClient.getBleDevice(b.getMacAddr());
        disposables.add(device.establishConnection(false) // <-- autoConnect flag
                .subscribe(
                        this::setUpDataQuery,
                        throwable -> Log.d(TAG, "connect:" + throwable.getMessage())
                ));

        disposables.add(device.observeConnectionStateChanges()
                .subscribe(
                        rxBleConnectionState1 -> onConnectionStateChanged(rxBleConnectionState1, context),
                        throwable -> Log.d(TAG, "connectStateChanged:" + throwable.getMessage())
                ));
    }

    private void onConnectionStateChanged(RxBleConnection.RxBleConnectionState connectionState, Activity context) {
        boolean isDisconnected = RxBleConnection.RxBleConnectionState.DISCONNECTED.equals(connectionState);
        boolean isConnected = RxBleConnection.RxBleConnectionState.CONNECTED.equals(connectionState);
        if (this.rxBleConnectionState == null || this.rxBleConnectionState != connectionState) {
            this.rxBleConnectionState = connectionState;
            if (isDisconnected) {
                Log.d(TAG, "onConnectionStateChanged: about to reconnect " + bluetooth.getName() + " after " + connectionState);
                disposables.clear();
                binding.getRoot().postDelayed(() -> connect(bluetooth, context), 3000);
            }
        }
        context.runOnUiThread(() -> {
                    String time = getTimestamp();
                    binding.value.setVisibility(View.VISIBLE);
                    binding.setValue("-");
                    binding.loadingIcon.setVisibility(View.GONE);
                    binding.loadingTxt.setVisibility(View.GONE);
                    binding.setIsConnected(isConnected);
                    binding.setInfo(connectionState.toString() + " " + time);
                }
        );
    }

    private void setUpDataQuery(RxBleConnection rxBleConnection) {
        if (isNotificationSupported) {
            if (bluetooth.getName().startsWith(MainActivity.TEMP_DEVICE_NAME)) {
                disposables.add(rxBleConnection
                        .setupNotification(characteristicID)
                        .flatMap(notificationObservable -> notificationObservable)
                        .subscribe(
                                this::onResponseReceived,
                                throwable -> {
                                    Log.e(TAG, "setupNotification: ", throwable);
                                    showToast(throwable.getMessage());
                                }
                        ));
                return;
            }
            disposables.add(rxBleConnection
                    .setupIndication(characteristicID)
                    .flatMap(notificationObservable -> notificationObservable)
                    .subscribe(
                            this::onResponseReceived,
                            throwable -> {
                                Log.e(TAG, "setupIndication: ", throwable);
                                showToast(throwable.getMessage());
                            }
                    ));
            return;
        }
        disposables.add(rxBleConnection
                .readCharacteristic(characteristicID)
                .repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS))
                .subscribe(
                        this::onResponseReceived,
                        throwable -> {
                            Log.e(TAG, "readCharacteristic: ", throwable);
                            showToast(throwable.getMessage());
                        }
                ));
    }

    private void showToast(String message) {
//        if (getActivity() != null) {
//            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show());
//        }
    }

    private void onResponseReceived(byte[] bytes) {
        displayResult(bytes);
    }

    private void displayResult(byte[] s) {
        Log.d(TAG, "displayResult: " + bluetooth.getName() + " = " + new String(s));
        String time = getTimestamp();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                binding.value.setVisibility(View.VISIBLE);
                binding.setValue(resultParser.parse(s));
                binding.loadingIcon.setVisibility(View.GONE);
                binding.loadingTxt.setVisibility(View.GONE);
                binding.setInfo(time);
                binding.setIsConnected(true);
            });
        }
    }

    private String getTimestamp() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return format.format(System.currentTimeMillis());
    }
}