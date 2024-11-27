package com.pos.hardware.connection.demo.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.pos.connection.bridge.binder.ECRConstant;
import com.pos.connection.bridge.binder.ECRParameters;
import com.pos.hardware.connection.demo.App;
import com.pos.hardware.connection.demo.PermissionUtil;
import com.pos.hardware.connection.demo.R;
import com.pos.hardware.connection.demo.databinding.ActivityMainBinding;
import com.pos.hardware.connection.demo.ecr.ECRHelper;
import com.pos.hardware.connection.demo.help.Logger;
import com.pos.hardware.connection.demo.ui.comm.ChatAdapter;
import com.pos.hardware.connection.demo.ui.comm.ChatBean;
import com.pos.hardware.connection.library.ECRServiceKernel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Executors;


/**
 * @author: Dadong
 * @date: 2024/11/21
 */

@SuppressLint("CheckResult")
public class MainActivity extends BaseAppCompatActivity {

    private ChatAdapter chatAdapter;
    private HashMap<String, String> bluetoothList;
    private ActivityMainBinding binding;
    private ArrayList<ChatBean> chatList = new ArrayList<ChatBean>();

    private String mode;
    private String bluetoothAddress;

    private int vspCableCheckState = ECRConstant.VSPCableCheckState.ENABLE_CHECK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
        initECR();
        showDeviceTypeDialog();
        requestPermission();
    }

    private void requestPermission() {
        String[] permissions = new String[]{"android.permission.BLUETOOTH_CONNECT"};
        PermissionUtil.checkPermission(this, permissions, 0x100);
    }

    private void initView() {
        mode = ECRConstant.Mode.RS232;
        int greenColor = Color.parseColor("#1DD959");
        int whiteColor = ContextCompat.getColor(this, R.color.white);
        int purpleColor = ContextCompat.getColor(this, R.color.purple_700);
        MaterialShapeDrawable btnDrawable = new MaterialShapeDrawable();
        btnDrawable.setCornerSize(36f);
        btnDrawable.setTint(purpleColor);
        MaterialShapeDrawable contentDrawable = new MaterialShapeDrawable();
        contentDrawable.setCornerSize(6f);
        contentDrawable.setTint(whiteColor);
        binding.contentEdit.setBackground(contentDrawable);
        MaterialShapeDrawable sendDrawable = new MaterialShapeDrawable();
        contentDrawable.setCornerSize(6f);
        contentDrawable.setTint(greenColor);
        binding.sendText.setBackground(sendDrawable);
        binding.connectText.setBackground(btnDrawable);
        binding.stopSendText.setBackground(btnDrawable);
        binding.disconnectText.setBackground(btnDrawable);
        binding.contentEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkSendButton();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.disconnectText.setOnClickListener(v -> {
            Logger.e("wl", "调用断开连接");
            ECRHelper.disconnect();
        });
        binding.stopSendText.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("method", "iBeaconStopTransmit");
            ECRHelper.extensionMethod(bundle);
            try {
                ECRServiceKernel.getInstance().ecrService.stop();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(chatAdapter = new ChatAdapter());
    }

    private void initECR() {
        ECRHelper.onBindSuccess = () -> {
            ECRHelper.registerECRListener();
        };
        ECRHelper.onBindFailure = () -> {
            runOnUiThread(this::changeConnectStatus);
            showToast(R.string.message_bind_ecr_service);
        };
        ECRHelper.onECRConnected = () -> {
            runOnUiThread(this::changeConnectStatus);
        };
        ECRHelper.onECRDisconnected = (code, message) -> {
            runOnUiThread(this::changeConnectStatus);
            showToast(message + " (" + code + ")");
        };
        ECRHelper.onSendSuccess = () -> {
            showToast("data send success");
        };
        ECRHelper.onSendFailure = (code, message) -> {
            showToast(message + " (" + code + ")");
            runOnUiThread(this::changeConnectStatus);
        };
        ECRHelper.onECRReceive = bytes -> {
            String text = new String(bytes, StandardCharsets.UTF_8);
            ChatBean chat = new ChatBean(text, App.server ? "Client" : "Server", App.server ? "Server" : "Client");
            chatList.add(chat);
            refreshAdapter();
        };
        ECRHelper.onWaitConnect = this::changeConnectStatus;
        ECRHelper.onBindError = (code, message) -> {
            runOnUiThread(() -> showToast(message + " (" + code + ")"));
        };
        ECRHelper.bindECRService();
    }

    private void send() {
        Executors.newCachedThreadPool().execute(() -> {
            String text = binding.contentEdit.getText().toString();
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            ChatBean chat = new ChatBean( App.server ? "Client" : "Server",text, App.server ? "Server" : "Client");
            Logger.e(App.TAG, "size: " + bytes.length);
            ECRHelper.send(bytes);
            chatList.add(chat);
            refreshAdapter();
        });
    }

    private void connect() {
        Bundle bundle = new Bundle();
        bundle.putString(ECRParameters.MODE, mode);
        bundle.putString(ECRParameters.TYPE, App.server ? ECRConstant.Type.MASTER : ECRConstant.Type.SLAVE);
        switch (mode) {
            case ECRConstant.Mode.Bluetooth:
                Logger.e(App.TAG, "bluetoothAddress: " + bluetoothAddress);
                bundle.putString(ECRParameters.BLUETOOTH_MAC_ADDRESS, bluetoothAddress);
                break;
            case ECRConstant.Mode.WIFI:
                showDialog(bundle);
                return;
            case ECRConstant.Mode.VSP:
                bundle.putInt(ECRParameters.ENABLE_VSP_CABLE_CHECK, vspCableCheckState);
                break;
            case ECRConstant.Mode.RS232:
                showSerialPortDialog(bundle);
                return;
        }
        binding.connectText.setText(R.string.connecting);
        binding.connectText.setOnClickListener(null);
        binding.connectText.setAlpha(0.5f);
        ECRHelper.connect(bundle);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAdapter();
        changeConnectStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ECRHelper.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (App.connected != -1) {
            showToast(R.string.message_select_connect);
            return super.onOptionsItemSelected(item);
        }
        switch (item.getItemId()) {
            case R.id.item_vsp:
                mode = ECRConstant.Mode.VSP;
                showVSPCableCheckStateDialog();
                break;
            case R.id.item_usb:
                mode = ECRConstant.Mode.USB;
                break;
            case R.id.item_beacon:
                mode = ECRConstant.Mode.iBeacon;
                break;
            case R.id.item_rs232:
                mode = ECRConstant.Mode.RS232;
                break;
            case R.id.item_bluetooth:
                mode = ECRConstant.Mode.Bluetooth;
                if (!App.server) {
                    switchBluetooth();
                }
                break;
            case R.id.item_wifi:
                mode = ECRConstant.Mode.WIFI;
                break;
            case R.id.item_extended_test:
                // openActivity(ExtendedTestActivity.class); // This method needs to be implemented in Java
                break;
        }
        showConnectStatus();
        return super.onOptionsItemSelected(item);
    }

    private void refreshAdapter() {
        runOnUiThread(() -> {
            chatAdapter.setData(chatList);
            if (chatAdapter.getItemCount() >= 1) {
                binding.recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }
        });
    }

    private void changeConnectStatus() {
        switch (App.connected) {
            case -1:
            case 0:
                binding.connectText.setAlpha(1f);
                binding.connectText.setOnClickListener(v -> connect());
                binding.connectText.setText(R.string.connect);
                break;
            case 1:
                binding.connectText.setAlpha(1f);
                binding.connectText.setOnClickListener(v -> connect());
                binding.connectText.setText(R.string.wait_connect);
                break;
        }
        checkSendButton();
        showConnectStatus();
    }

    private void checkSendButton() {
        boolean bool = binding.contentEdit.getText().toString().trim().length() > 0 && App.connected == 0;
        if (bool) {
            binding.sendText.setAlpha(1f);
            binding.sendText.setOnClickListener(v -> send());
        } else {
            binding.sendText.setAlpha(0.5f);
            binding.sendText.setOnClickListener(null);


        }
    }

    private void showConnectStatus() {
        String text = "";
        switch (App.connected) {
            case -1:
                text = getString(R.string.disconnected);
                break;
            case 0:
                text = getString(R.string.connected);
                break;
            case 1:
                text = getString(R.string.wait_connect);
                break;
        }
        String modeText = "";
        switch (mode) {
            case ECRConstant.Mode.VSP:
                modeText = getString(R.string.type_vsp) + " " + text;
                break;
            case ECRConstant.Mode.USB:
                modeText = getString(R.string.type_usb) + " " + text;
                break;
            case ECRConstant.Mode.RS232:
                modeText = getString(R.string.type_rs232) + " " + text;
                break;
            case ECRConstant.Mode.iBeacon:
                modeText = getString(R.string.type_beacon) + " " + text;
                break;
            case ECRConstant.Mode.Bluetooth:
                modeText = getString(R.string.type_bluetooth) + " " + text;
                break;
            case ECRConstant.Mode.WIFI:
                modeText = getString(R.string.type_wifi) + " " + text;
                break;
            default:
                modeText = "";
                break;
        }
        text = App.server ? "Server " + modeText : "Client " + modeText;
        binding.statusText.setText(text);
        binding.stopSendText.setVisibility(mode.equals(ECRConstant.Mode.iBeacon) ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("MissingPermission")
    private void switchBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_DENIED) {
                showToast("No required permissions, please check");
                return;
            }
        }
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothList.clear();
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                if (bondedDevices != null && !bondedDevices.isEmpty()) {
                    for (BluetoothDevice bluetoothDevice : bondedDevices) {
                        String name = bluetoothDevice.getName();
                        String address = bluetoothDevice.getAddress();
                        Logger.e(App.TAG, "name: " + name);
                        Logger.e(App.TAG, "address: " + address);
                        if (address.equals("00:11:22:33:44:55")) continue;
                        bluetoothList.put(name + " - " + address, address);
                    }
                }
                if (!bluetoothList.isEmpty()) {
                    showBluetoothDialog();
                } else {
                    showToast(R.string.message_bluetooth_not_find_bonded);
                }
            } else {
                showToast(R.string.message_bluetooth_disable);
            }
        } else {
            showToast(R.string.message_bluetooth_not_support);
        }
    }

    private void showDeviceTypeDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_devices_type, null);
        RadioGroup radioGroup = view.findViewById(R.id.rg_type);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.dialog_device_type).setView(view)
                .setCancelable(false).show();
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            App.server = checkedId == R.id.mr_server;
            showConnectStatus();
            dialog.dismiss();
        });
    }

    private void showDialog(Bundle bundle) {
        View view = getLayoutInflater().inflate(R.layout.dialog_content, null);
        EditText ip = view.findViewById(R.id.ip);
        EditText port = view.findViewById(R.id.port);
        if (App.server) {
            ip.setVisibility(View.GONE);
        }
        new AlertDialog.Builder(this).setTitle("Input Data").setView(view).setPositiveButton("Confirm", (dialog, which) -> {
            if (App.server) {
                if (TextUtils.isEmpty(port.getText().toString())) {
                    showToast("Please input port");
                    return;
                }
            } else {
                if (TextUtils.isEmpty(port.getText().toString()) || TextUtils.isEmpty(ip.getText().toString())) {
                    showToast("Please input port and ip");
                    return;
                }
            }
            bundle.putInt(ECRParameters.WIFI_PORT, Integer.parseInt(port.getText().toString()));
            bundle.putString(ECRParameters.WIFI_ADDRESS, ip.getText().toString());
            binding.connectText.setText(R.string.connecting);
            binding.connectText.setOnClickListener(null);
            binding.connectText.setAlpha(0.5f);
            ECRHelper.connect(bundle);
        }).show();
    }

    private void showSerialPortDialog(Bundle bundle) {
        View view = getLayoutInflater().inflate(R.layout.dialog_serial_port_param, null);
        new AlertDialog.Builder(this).setTitle("Input Data").setView(view).setPositiveButton("Confirm", (dialog, which) -> {
            EditText baudRate = view.findViewById(R.id.et_baud_rate);
            Spinner spDataBits = view.findViewById(R.id.sp_data_bits);
            Spinner spParity = view.findViewById(R.id.sp_parity);
            Spinner spStopBits = view.findViewById(R.id.sp_stop_bits);
            if (TextUtils.isEmpty(baudRate.getText().toString())) {
                showToast("Please input baud rate");
                return;
            }
            try {
                bundle.putInt(ECRParameters.BAUD_RATE, Integer.parseInt(baudRate.getText().toString()));
            } catch (Exception e) {
                e.printStackTrace();
                showToast("Please input correct baud rate");
                return;
            }
            int dataBits = spDataBits.getSelectedItemPosition() + 5;
            int parity = spParity.getSelectedItemPosition();
            int stopBits = spStopBits.getSelectedItemPosition() + 1;
            bundle.putInt(ECRParameters.DATA_BITS, dataBits);
            bundle.putInt(ECRParameters.PARITY, parity);
            bundle.putInt(ECRParameters.STOP_BITS, stopBits);

            binding.connectText.setText(R.string.connecting);
            binding.connectText.setOnClickListener(null);
            binding.connectText.setAlpha(0.5f);
            ECRHelper.connect(bundle);
        }).show();
        ((Spinner) view.findViewById(R.id.sp_data_bits)).setSelection(3);
    }

    private void showBluetoothDialog() {
        String[] arrays = new String[bluetoothList.size()];
        bluetoothList.keySet().toArray(arrays);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AlertDialog_Base);
        builder.setTitle(R.string.dialog_select_bluetooth)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setItems(arrays, (dialog, which) -> {
                    String item = arrays[which];
                    String bluetoothAddress = bluetoothList.get(item);
                })
                .show();
    }

    private void showVSPCableCheckStateDialog() {
        String[] arrays = new String[]{"ENABLE_CHECK", "DISABLE_CHECK"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_AlertDialog_Base);
        builder.setTitle(R.string.dialog_select_vsp_state)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, null)
                .setItems(arrays, (dialog, which) -> {
                    if (which == 0) {
                        vspCableCheckState = ECRConstant.VSPCableCheckState.ENABLE_CHECK;
                    } else {
                        vspCableCheckState = ECRConstant.VSPCableCheckState.DISABLE_CHECK;
                    }
                })
                .show();
    }
}