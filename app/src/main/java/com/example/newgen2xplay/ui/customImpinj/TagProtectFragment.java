package com.example.newgen2xplay.ui.customImpinj;

import static com.example.newgen2xplay.RFIDHandler.impinjExtensions;
import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newgen2xplay.R;
import com.example.newgen2xplay.databinding.FragmentTagProtectBinding;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TagProtectFragment extends Fragment {

    private TagProtectViewModel mViewModel;
    private FragmentTagProtectBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static TagProtectFragment newInstance() {
        return new TagProtectFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTagProtectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        executor.shutdown();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.Protect_op_type_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.protectModeSpinner.setAdapter(adapter);

        // Tag pattern AutoCompleteTextView setup
        java.util.List<String> tagIDs = com.example.newgen2xplay.RFIDHandler.tagIDs;
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.item_tag_suggestion, tagIDs);
        binding.etMask.setAdapter(tagAdapter);

        binding.protectModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                // Set visibility based on selected value
                switch (selected) {
                    case "Enable Inventory Of Protected Tags":
                        binding.etMask.setVisibility(View.GONE);
                        //binding.etPassword.setVisibility(View.GONE);
                        break;
                    case "Clear Protected Mode Configuration":
                        binding.etMask.setVisibility(View.GONE);
                        //binding.etPassword.setVisibility(View.GONE);
                        break;

                    default:
                        // Default case
                        binding.etMask.setVisibility(View.VISIBLE);
                        binding.etPassword.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Default visibility
                binding.etMask.setVisibility(View.VISIBLE);
                binding.etPassword.setVisibility(View.VISIBLE);
            }
        });

        binding.buttonPerformOp.setOnClickListener(v -> {
            // Get spinner value
            String spinnerValue = binding.protectModeSpinner.getSelectedItem().toString();
            // Get edit text values
            String editText1 = binding.etMask.getText().toString();
            String editText2 = binding.etPassword.getText().toString();
            // Perform your operation here
            handlePerformOp(spinnerValue, editText1, editText2);

//            binding.checkEnableVisibility.setChecked(false);
//            binding.checkDisableVisibility.setChecked(false);
        });

        binding.modeInfo.setOnClickListener(v -> {

            showRequiredValuesDialog();
        });
    }

    private void handleEnableVisibilityChecked() {
        String password = binding.etPassword.getText().toString();
        if (password.isEmpty()) {
            //Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
            mainHandler.post(() ->  Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show());
            return;
        }
        executor.execute(() -> {
            try {
                mConnectedRfidReader.Actions.PreFilters.deleteAll();
                impinjExtensions.enableTagVisibility(password, (short) 1);
                mainHandler.post(() -> Toast.makeText(requireContext(), "Enable Visibility Success", Toast.LENGTH_SHORT).show());
            } catch (OperationFailureException | InvalidUsageException e) {
                mainHandler.post(() -> Toast.makeText(requireContext(), "Enable Visibility Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        });
    }
    private void handleDisableVisibilityChecked() {
        String password = binding.etPassword.getText().toString();
        if (password.isEmpty()) {
            mainHandler.post(() ->  Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show() );
            return;
        }
        executor.execute(() -> {
            try {
                mConnectedRfidReader.Actions.PreFilters.deleteAll();
                impinjExtensions.disableTagVisibility(password, (short)1);
                mainHandler.post(() -> Toast.makeText(requireContext(), "Disable Visibility Success", Toast.LENGTH_SHORT).show());
            } catch (OperationFailureException | InvalidUsageException | IllegalStateException e) {
                mainHandler.post(() -> Toast.makeText(requireContext(), "Disable Visibility Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                e.printStackTrace();
            }
        });
    }
    private void handlePerformOp(String spinnerValue, String tagId, String password) {

        AtomicReference<TagData> tagData = new AtomicReference<>();
        TagData writeTagData = new TagData();
        AtomicBoolean isPrefilterRead = new AtomicBoolean(false);
        AtomicBoolean isPrefilterWrite = new AtomicBoolean(false);

        executor.execute(() -> {
            if(spinnerValue.equals("Protect")) {
                if (password.isEmpty() || tagId.isEmpty()) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "TagID and Password cannot be empty", Toast.LENGTH_SHORT).show());
                    return;
                }
                try {
                    impinjExtensions.enableTagProtection(tagId, password, null);
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Protect Success", Toast.LENGTH_SHORT).show());
                } catch (OperationFailureException e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Protect Failed " + e.getVendorMessage(), Toast.LENGTH_SHORT).show());
                    return;
                }catch ( InvalidUsageException e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Protect Failed " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    return;
                }
            } else if(spinnerValue.equals("Unprotect")) {
                if (password.isEmpty() || tagId.isEmpty()) {
                    mainHandler.post(() ->   Toast.makeText(requireContext(), "TagID and Password cannot be empty", Toast.LENGTH_SHORT).show());
                    return;
                }
                try {
                    mConnectedRfidReader.Actions.PreFilters.deleteAll();
                    impinjExtensions.disableTagProtection(tagId, password, null, (short) 1);
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Unprotect Success", Toast.LENGTH_SHORT).show());
                } catch (OperationFailureException e){
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Unprotect Failed " + e.getVendorMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                    return;
                } catch ( InvalidUsageException | IllegalStateException  | NumberFormatException e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Unprotect Failed " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                    return;
                }
            } else if(spinnerValue.equals("GetPassword")) {
                if ( tagId.isEmpty()) {
                    mainHandler.post(() ->  Toast.makeText(requireContext(), "TagID  cannot be empty", Toast.LENGTH_SHORT).show());
                    return;
                }
                TagAccess tagAccess = new TagAccess();
                TagAccess.ReadAccessParams readAccessParams = tagAccess.new ReadAccessParams();
                readAccessParams.setAccessPassword(Long.decode("00" ));
                readAccessParams.setCount(2);
                readAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_RESERVED);
                readAccessParams.setOffset(2);
                if(tagId.length() <= 24){
                    isPrefilterRead.set(true);
                }

                try {
                    tagData.set(mConnectedRfidReader.Actions.TagAccess.readWait(tagId, readAccessParams, null, isPrefilterRead.get()));
                }catch (OperationFailureException e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Read password failed " + e.getVendorMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                    return;
                }
                catch ( InvalidUsageException | IllegalStateException  | NumberFormatException e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Read password failed " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                            if (tagData.get().getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                                Toast.makeText(requireContext(),  "Read Success", Toast.LENGTH_SHORT).show();
                                binding.etPassword.setText(tagData.get().getMemoryBankData());
                            } else {
                                Toast.makeText(requireContext(), "Read Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
            else if(spinnerValue.equals("SetPassword")) {
                if (password.isEmpty() || tagId.isEmpty()) {
                    mainHandler.post(() ->   Toast.makeText(requireContext(), "TagID and Password cannot be empty", Toast.LENGTH_SHORT).show());
                    return;
                }
                TagAccess tagAccess = new TagAccess();
                TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();
                String writeData = password ;
                writeAccessParams.setAccessPassword(Long.decode("00" ));
                writeAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_RESERVED);
                writeAccessParams.setOffset(2);
                writeAccessParams.setWriteData(writeData);
                writeAccessParams.setWriteDataLength(2);
                if(tagId.length() <= 24){
                    isPrefilterWrite.set(true);
                }
                try {
                    mConnectedRfidReader.Actions.TagAccess.writeWait(tagId, writeAccessParams, null,writeTagData, isPrefilterWrite.get(), false);
                } catch (OperationFailureException  e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Write password Failed " + e.getVendorMessage()+ " " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                    return;
                }
                catch ( InvalidUsageException | IllegalStateException  | NumberFormatException e) {
                    mainHandler.post(() -> Toast.makeText(requireContext(), "Write password Failed " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                            if (writeTagData.getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                                Toast.makeText(requireContext(),  "Write password Success", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Write password Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
            else if(spinnerValue.equals("Enable Inventory Of Protected Tags")){
                handleEnableVisibilityChecked();
            }
            else if(spinnerValue.equals("Clear Protected Mode Configuration")){
                handleDisableVisibilityChecked();
            }

        });

    }

    private void showRequiredValuesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        ScrollView scrollView = new ScrollView(requireContext());
        TextView messageView = new TextView(requireContext());
        messageView.setPadding(50, 30, 50, 30);
        messageView.setTextSize(14);
        String htmlText = "Protect: Requires TagID and Password/PIN<br><br>" +
                "Unprotect: Requires TagID and Password/PIN<br><br>" +
                "GetPassword: Requires TagID<br><br>" +
                "SetPassword: Requires TagID and Password/PIN<br><br>" +
                "Enable Inventory Of Protected Tags: Requires Password/PIN<br><br>" +
                "Clear Protected Mode Configuration: Requires Password/PIN<br><br>" +
                "<b>Note:</b> When user performs \"Clear Protected Mode Configuration\" reader will no longer read protected tags";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            messageView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT));
        } else {
            messageView.setText(Html.fromHtml(htmlText));
        }

        scrollView.addView(messageView);

        builder.setTitle("Required values for each mode of operation")
                .setView(scrollView)
                .setPositiveButton("OK", null)
                .show();

    }

}
