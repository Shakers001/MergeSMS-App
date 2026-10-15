package com.mergesms.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 101;
    private static final int PERMISSION_REQUEST = 202;
    private final List<Map<String, String>> rows = new ArrayList<>();
    private final List<String> headers = new ArrayList<>();
    private final List<CheckBox> recipientChecks = new ArrayList<>();
    private LinearLayout root, recipientList;
    private TextView importStatus, countLabel;
    private EditText messageInput;
    private Spinner phoneColumnSpinner;
    private String csvName = "";
    private int selectedSubscriptionId = -1;

    private int navy = Color.rgb(16, 42, 86);
    private int blue = Color.rgb(23, 105, 224);
    private int pale = Color.rgb(241, 246, 255);

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private void addGap(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, lp(1, height));
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setBackground(bg(color, 24));
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = vertical();
        root.setPadding(18, 18, 18, 24);
        root.setBackgroundColor(Color.rgb(247, 249, 253));
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout hero = vertical();
        hero.setPadding(20, 18, 20, 18);
        hero.setBackground(bg(navy, 22));
        TextView title = text("MergeSMS", 27, Color.WHITE, true);
        TextView subtitle = text("Personalized bulk SMS • On-device sending", 13, Color.rgb(218, 230, 255), false);
        hero.addView(title);
        addGap(hero, 5);
        hero.addView(subtitle);
        root.addView(hero, lp(-1, -2));
        addGap(root, 16);

        root.addView(text("1. Import recipients", 18, navy, true));
        addGap(root, 8);
        Button importButton = button("＋  Import CSV / Excel-exported CSV", blue);
        root.addView(importButton, lp(-1, -2));
        importButton.setOnClickListener(v -> chooseFile());
        importStatus = text("No file imported yet. In Excel, save your sheet as CSV first.", 13, Color.DKGRAY, false);
        root.addView(importStatus);
        addGap(root, 14);

        root.addView(text("2. Choose the phone-number column", 18, navy, true));
        addGap(root, 6);
        phoneColumnSpinner = new Spinner(this);
        root.addView(phoneColumnSpinner, lp(-1, -2));
        addGap(root, 14);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text("3. Recipients", 18, navy, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button all = button("Select all", blue);
        all.setTextSize(12);
        row.addView(all, lp(-2, 44));
        root.addView(row);
        all.setOnClickListener(v -> {
            boolean allChecked = true;
            for (CheckBox c : recipientChecks) if (!c.isChecked()) { allChecked = false; break; }
            for (CheckBox c : recipientChecks) c.setChecked(!allChecked);
            updateCount();
        });
        countLabel = text("0 recipients loaded", 13, Color.DKGRAY, false);
        root.addView(countLabel);
        recipientList = vertical();
        recipientList.setPadding(10, 4, 10, 4);
        recipientList.setBackground(bg(Color.WHITE, 14));
        root.addView(recipientList, lp(-1, -2));
        addGap(root, 14);

        root.addView(text("4. Message template", 18, navy, true));
        addGap(root, 6);
        TextView hint = text("Use column names in braces, e.g. Hello {name}, your balance is {Balance}.", 13, Color.DKGRAY, false);
        root.addView(hint);
        messageInput = new EditText(this);
        messageInput.setMinLines(4);
        messageInput.setGravity(Gravity.TOP | Gravity.START);
        messageInput.setText("Hello {name}, welcome");
        messageInput.setHint("Type your personalized message");
        messageInput.setTextSize(16);
        messageInput.setPadding(14, 12, 14, 12);
        messageInput.setBackground(bg(Color.WHITE, 14));
        root.addView(messageInput, lp(-1, -2));
        addGap(root, 5);
        TextView limit = text("Maximum 500 characters per message.", 12, Color.DKGRAY, false);
        root.addView(limit);
        addGap(root, 14);

        Button send = button("Send SMS", Color.rgb(20, 150, 91));
        send.setTextSize(17);
        root.addView(send, lp(-1, -2));
        send.setOnClickListener(v -> beginSend());
        addGap(root, 10);
        TextView footer = text("Messages are sent through your phone's SIM and carrier plan. Carrier charges may apply. Test with one recipient first.", 12, Color.DKGRAY, false);
        root.addView(footer);
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "application/vnd.ms-excel", "text/plain"});
        startActivityForResult(intent, PICK_FILE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILE || resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            parseCsv(uri);
            csvName = uri.getLastPathSegment() == null ? "Imported file" : uri.getLastPathSegment();
            importStatus.setText("Imported " + rows.size() + " rows from file.");
            populateColumns();
            populateRecipients();
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Could not import file")
                .setMessage("Please save the spreadsheet as CSV (comma-separated values) and import it again.\n\nDetails: " + e.getMessage())
                .setPositiveButton("OK", null).show();
        }
    }

    private void parseCsv(Uri uri) throws Exception {
        rows.clear();
        headers.clear();
        InputStream stream = getContentResolver().openInputStream(uri);
        if (stream == null) throw new Exception("File could not be opened.");
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String line = br.readLine();
        if (line == null) throw new Exception("The file is empty.");
        List<String> first = splitCsv(line);
        for (String h : first) headers.add(h.replace("\uFEFF", "").trim());
        if (headers.isEmpty()) throw new Exception("No header row found.");
        while ((line = br.readLine()) != null && rows.size() < 1000) {
            if (line.trim().isEmpty()) continue;
            List<String> values = splitCsv(line);
            Map<String, String> item = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) item.put(headers.get(i), i < values.size() ? values.get(i).trim() : "");
            rows.add(item);
        }
        br.close();
        if (rows.isEmpty()) throw new Exception("No recipient rows found below the header row.");
    }

    // Handles commas and escaped quotes in ordinary CSV rows.
    private List<String> splitCsv(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                values.add(cell.toString()); cell.setLength(0);
            } else cell.append(ch);
        }
        values.add(cell.toString());
        return values;
    }

    private void populateColumns() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, headers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        phoneColumnSpinner.setAdapter(adapter);
        int best = 0;
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase();
            if (h.contains("phone") || h.contains("mobile") || h.contains("number") || h.equals("tel")) { best = i; break; }
        }
        phoneColumnSpinner.setSelection(best);
    }

    // Map.getOrDefault needs API 24; minSdk is 23, so use a plain lookup.
    private static String valueOrEmpty(Map<String, String> map, String key) {
        String v = map.get(key);
        return v == null ? "" : v;
    }

    private String phoneHeader() {
        Object selected = phoneColumnSpinner.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    private void populateRecipients() {
        recipientList.removeAllViews();
        recipientChecks.clear();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> r = rows.get(i);
            String phone = valueOrEmpty(r, phoneHeader());
            String name = r.containsKey("name") ? r.get("name") : (r.containsKey("Name") ? r.get("Name") : "");
            CheckBox cb = new CheckBox(this);
            cb.setText((name.isEmpty() ? "Recipient " + (i + 1) : name) + (phone.isEmpty() ? " • missing number" : " • " + phone));
            cb.setTextSize(14);
            cb.setTag(i);
            cb.setChecked(!phone.trim().isEmpty());
            cb.setOnCheckedChangeListener((button, checked) -> updateCount());
            recipientList.addView(cb, lp(-1, -2));
            recipientChecks.add(cb);
        }
        updateCount();
    }

    private void updateCount() {
        int selected = 0;
        for (CheckBox c : recipientChecks) if (c.isChecked()) selected++;
        if (countLabel != null) countLabel.setText(selected + " selected • " + rows.size() + " loaded (maximum 1,000)");
    }

    private void beginSend() {
        if (rows.isEmpty()) { alert("Import a CSV file first."); return; }
        String template = messageInput.getText().toString();
        if (template.trim().isEmpty()) { alert("Enter a message first."); return; }
        if (template.length() > 500) { alert("Your message is " + template.length() + " characters. The limit is 500."); return; }
        int selected = 0;
        for (CheckBox c : recipientChecks) if (c.isChecked()) selected++;
        if (selected == 0) { alert("Select at least one recipient."); return; }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST);
            return;
        }
        chooseSimAndConfirm();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) chooseSimAndConfirm();
            else alert("SMS permission is required to send messages from this phone.");
        }
    }

    private void chooseSimAndConfirm() {
        List<SubscriptionInfo> sims = new ArrayList<>();
        try {
            SubscriptionManager sm = (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);
            if (Build.VERSION.SDK_INT >= 22 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && sm != null) {
                List<SubscriptionInfo> active = sm.getActiveSubscriptionInfoList();
                if (active != null) sims.addAll(active);
            }
        } catch (SecurityException ignored) { }

        if (sims.size() > 1) {
            String[] labels = new String[sims.size()];
            for (int i = 0; i < sims.size(); i++) {
                SubscriptionInfo s = sims.get(i);
                String carrier = s.getCarrierName() == null ? "SIM" : s.getCarrierName().toString();
                labels[i] = carrier + " • SIM slot " + (s.getSimSlotIndex() + 1);
            }
            new AlertDialog.Builder(this).setTitle("Choose SIM to send from")
                .setItems(labels, (dialog, which) -> {
                    selectedSubscriptionId = sims.get(which).getSubscriptionId();
                    confirmSend();
                }).setNegativeButton("Cancel", null).show();
        } else {
            selectedSubscriptionId = sims.size() == 1 ? sims.get(0).getSubscriptionId() : -1;
            confirmSend();
        }
    }

    private void confirmSend() {
        int selected = 0;
        for (CheckBox c : recipientChecks) if (c.isChecked()) selected++;
        new AlertDialog.Builder(this)
            .setTitle("Confirm bulk SMS")
            .setMessage("Send " + selected + " personalized message(s) using your selected/default SIM? Your carrier may charge for each SMS.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send", (dialog, which) -> sendMessages())
            .show();
    }

    private String renderMessage(Map<String, String> row, String template) {
        String result = template;
        for (Map.Entry<String, String> entry : row.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private void sendMessages() {
        int sent = 0, failed = 0;
        String phoneKey = phoneHeader();
        String template = messageInput.getText().toString();
        try {
            SmsManager sms;
            if (Build.VERSION.SDK_INT >= 31) {
                sms = selectedSubscriptionId >= 0
                    ? getSystemService(SmsManager.class).createForSubscriptionId(selectedSubscriptionId)
                    : getSystemService(SmsManager.class);
            } else {
                sms = selectedSubscriptionId >= 0
                    ? SmsManager.getSmsManagerForSubscriptionId(selectedSubscriptionId)
                    : SmsManager.getDefault();
            }
            for (CheckBox cb : recipientChecks) {
                if (!cb.isChecked()) continue;
                int index = (Integer) cb.getTag();
                Map<String, String> r = rows.get(index);
                String phone = valueOrEmpty(r, phoneKey).trim();
                if (phone.isEmpty()) { failed++; continue; }
                String message = renderMessage(r, template);
                if (message.length() > 500) { failed++; continue; }
                ArrayList<String> parts = sms.divideMessage(message);
                sms.sendMultipartTextMessage(phone, null, parts, null, null);
                sent++;
                try { Thread.sleep(120); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            new AlertDialog.Builder(this).setTitle("Sending requested")
                .setMessage("Handed " + sent + " message(s) to Android's SMS service.\nSkipped/failed before submission: " + failed + ".\n\nThis does not confirm delivery. Check your Messages app and carrier balance.")
                .setPositiveButton("OK", null).show();
        } catch (Exception e) {
            alert("Could not send SMS: " + e.getMessage() + "\nCheck SIM status, SMS permission, and carrier service.");
        }
    }

    private void alert(String message) {
        new AlertDialog.Builder(this).setTitle("MergeSMS").setMessage(message).setPositiveButton("OK", null).show();
    }
}
