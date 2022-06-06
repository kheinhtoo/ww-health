package com.weeswares.iok.health.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.weeswares.iok.health.R;
import com.weeswares.iok.health.databinding.FragmentOutputBinding;
import com.weeswares.iok.health.helpers.Bluetooth;
import com.weeswares.iok.health.helpers.HexString;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;

public class OutputFragment extends Fragment {
    private static final String TAG = OutputFragment.class.toString();

    private static final String ARG_PARAM1 = "device";
    private static final String ARG_PARAM2 = "title";
    private static final String ARG_PARAM3 = "characteristic_id";
    private static final String ARG_PARAM4 = "notification_support";

    private Bluetooth bluetooth;
    private String title;
    private UUID characteristicID;
    private boolean isNotificationSupported;

    private FragmentOutputBinding binding;
    private RxBleClient rxBleClient;
    private Disposable connection;
    private Disposable connectionListener;
    private Disposable queryListener;

    public OutputFragment() {
        // Required empty public constructor
    }

    public static OutputFragment newInstance(Bluetooth b, String title, String charID, boolean notificationSupport) {
        OutputFragment fragment = new OutputFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PARAM1, b);
        args.putString(ARG_PARAM2, title);
        args.putString(ARG_PARAM3, charID);
        args.putBoolean(ARG_PARAM4, notificationSupport);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bluetooth = (Bluetooth) getArguments().getSerializable(ARG_PARAM1);
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
        binding.setDevice(bluetooth);
        binding.setTitle(title);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.setTitle(title);
        binding.setDevice(bluetooth);
        binding.setValue("--");
        connect(bluetooth);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        init(context);
    }

    @Override
    public void onDetach() {
        if (connection != null && !connection.isDisposed()) connection.dispose();
        if (connectionListener != null && !connectionListener.isDisposed())
            connectionListener.dispose();
        if (queryListener != null && !queryListener.isDisposed()) queryListener.dispose();
        super.onDetach();
    }

    private void init(Context context) {
        rxBleClient = RxBleClient.create(context);
    }

    void connect(Bluetooth b) {
        if (b == null) return;
        RxBleDevice device = rxBleClient.getBleDevice(b.getMacAddr());
        connection = device.establishConnection(false) // <-- autoConnect flag
                .subscribe(
                        this::setUpDataQuery,
                        throwable -> displayResult(throwable.getMessage())
                );

        connectionListener = device.observeConnectionStateChanges()
                .subscribe(
                        this::onConnectionStateChanged,
                        throwable -> displayResult(throwable.getMessage())
                );

    }

    private void onConnectionStateChanged(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
        if (RxBleConnection.RxBleConnectionState.DISCONNECTED.equals(rxBleConnectionState)) {
            connect(bluetooth);
        }
        getActivity().runOnUiThread(() -> {
                    String time = getTimestamp();
                    binding.setInfo(rxBleConnectionState.toString() + " " + time);
                }
        );
    }

    private void setUpDataQuery(RxBleConnection rxBleConnection) {
        if (isNotificationSupported) {
            queryListener = rxBleConnection.setupNotification(characteristicID)
                    .doOnNext(
                            notificationObservable -> {
                                displayResult("notify set up");
                            }
                    )
                    .flatMap(notificationObservable -> notificationObservable)
                    .subscribe(
                            this::onResponseReceived,
                            throwable -> displayResult(throwable.getMessage())
                    );
            return;
        }
        queryListener = rxBleConnection
                .readCharacteristic(characteristicID)
                .repeatWhen(completed -> completed.delay(3, TimeUnit.SECONDS))
                .subscribe(
                        this::onResponseReceived,
                        throwable -> displayResult(throwable.getMessage())
                );
    }

    private void onResponseReceived(byte[] bytes) {
        displayResult(HexString.bytesToHex(bytes));
    }

    private void displayResult(String s) {
        String time = getTimestamp();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                binding.setValue(s);
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