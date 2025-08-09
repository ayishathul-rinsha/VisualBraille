package com.example.braillekeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class BrailleKeyboard extends InputMethodService {

    private Map<String, String> brailleMap;
    private TextView statusBar;
    private int charactersTyped = 0;
    private boolean capsLockOn = false;
    private String currentMode = "Grade 1";

    @Override
    public void onCreate() {
        super.onCreate();
        initBrailleMap();
    }

    private void initBrailleMap() {
        brailleMap = new HashMap<>();
        // Grade 1 Braille (basic)
        brailleMap.put("a", "⠁"); brailleMap.put("b", "⠃"); brailleMap.put("c", "⠉");
        brailleMap.put("d", "⠙"); brailleMap.put("e", "⠑"); brailleMap.put("f", "⠋");
        brailleMap.put("g", "⠛"); brailleMap.put("h", "⠓"); brailleMap.put("i", "⠊");
        brailleMap.put("j", "⠚"); brailleMap.put("k", "⠅"); brailleMap.put("l", "⠇");
        brailleMap.put("m", "⠍"); brailleMap.put("n", "⠝"); brailleMap.put("o", "⠕");
        brailleMap.put("p", "⠏"); brailleMap.put("q", "⠟"); brailleMap.put("r", "⠗");
        brailleMap.put("s", "⠎"); brailleMap.put("t", "⠞"); brailleMap.put("u", "⠥");
        brailleMap.put("v", "⠧"); brailleMap.put("w", "⠺"); brailleMap.put("x", "⠭");
        brailleMap.put("y", "⠽"); brailleMap.put("z", "⠵");

        // Numbers with prefix
        brailleMap.put("1", "⠼⠁"); brailleMap.put("2", "⠼⠃"); brailleMap.put("3", "⠼⠉");
        brailleMap.put("4", "⠼⠙"); brailleMap.put("5", "⠼⠑"); brailleMap.put("6", "⠼⠋");
        brailleMap.put("7", "⠼⠛"); brailleMap.put("8", "⠼⠓"); brailleMap.put("9", "⠼⠊");
        brailleMap.put("0", "⠼⠚");

        // Extended punctuation
        brailleMap.put(" ", " ");
        brailleMap.put(".", "⠲"); brailleMap.put(",", "⠂"); brailleMap.put("?", "⠦");
        brailleMap.put("!", "⠖"); brailleMap.put(":", "⠒"); brailleMap.put(";", "⠆");
        brailleMap.put("'", "⠄"); brailleMap.put("-", "⠤"); brailleMap.put("(", "⠣");
        brailleMap.put(")", "⠜"); brailleMap.put("@", "⠈⠁"); brailleMap.put("#", "⠼");
    }

    @Override
    public View onCreateInputView() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1a1a1a"));
        mainLayout.setPadding(8, 8, 8, 8);

        // Status bar
        statusBar = new TextView(this);
        statusBar.setText("🔸 Braille Keyboard Pro | Mode: " + currentMode + " | Characters: " + charactersTyped);
        statusBar.setTextColor(Color.WHITE);
        statusBar.setBackgroundColor(Color.parseColor("#333333"));
        statusBar.setPadding(12, 8, 12, 8);
        statusBar.setTextSize(12);
        mainLayout.addView(statusBar);

        // Mode selector row
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);

        Button modeBtn = createSpecialButton("MODE", Color.parseColor("#4CAF50"));
        modeBtn.setOnClickListener(v -> toggleMode());
        modeRow.addView(modeBtn);

        Button capsBtn = createSpecialButton("CAPS", capsLockOn ? Color.parseColor("#FF5722") : Color.parseColor("#666666"));
        capsBtn.setOnClickListener(v -> toggleCapsLock(capsBtn));
        modeRow.addView(capsBtn);

        Button clearBtn = createSpecialButton("CLEAR", Color.parseColor("#F44336"));
        clearBtn.setOnClickListener(v -> clearAll());
        modeRow.addView(clearBtn);

        mainLayout.addView(modeRow);

        // Number row
        createKeyRow(mainLayout, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}, false);

        // QWERTY layout
        createKeyRow(mainLayout, new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}, false);
        createKeyRow(mainLayout, new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"}, false);

        // Bottom row with DEL
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        String[] bottomKeys = {"z", "x", "c", "v", "b", "n", "m"};
        for (String key : bottomKeys) {
            Button button = createKeyButton(key, false);
            bottomRow.addView(button);
        }

        Button delBtn = createSpecialButton("⌫", Color.parseColor("#FF9800"));
        delBtn.setOnClickListener(v -> handleDelete());
        bottomRow.addView(delBtn);

        mainLayout.addView(bottomRow);

        // Punctuation row
        createKeyRow(mainLayout, new String[]{".", ",", "?", "!", ":", ";", "'", "-"}, false);

        // Action row
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button spaceBtn = createSpecialButton("SPACE", Color.parseColor("#2196F3"));
        spaceBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 3.0f));
        spaceBtn.setOnClickListener(v -> handleKeyPress(" "));
        actionRow.addView(spaceBtn);

        Button enterBtn = createSpecialButton("↵", Color.parseColor("#9C27B0"));
        enterBtn.setOnClickListener(v -> handleEnter());
        actionRow.addView(enterBtn);

        Button sendBtn = createSpecialButton("SEND", Color.parseColor("#4CAF50"));
        sendBtn.setOnClickListener(v -> handleSend());
        actionRow.addView(sendBtn);

        mainLayout.addView(actionRow);

        return mainLayout;
    }

    private void createKeyRow(LinearLayout parent, String[] keys, boolean isSpecial) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (String key : keys) {
            Button button = createKeyButton(key, isSpecial);
            row.addView(button);
        }

        parent.addView(row);
    }

    private Button createKeyButton(String key, boolean isSpecial) {
        Button button = new Button(this);
        button.setText(capsLockOn ? key.toUpperCase() : key);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 1.0f));
        button.setBackgroundColor(isSpecial ? Color.parseColor("#607D8B") : Color.parseColor("#424242"));
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setOnClickListener(v -> handleKeyPress(key));
        return button;
    }

    private Button createSpecialButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 1.0f));
        button.setBackgroundColor(color);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        return button;
    }

    private void handleKeyPress(String key) {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            String processedKey = capsLockOn ? key.toUpperCase() : key.toLowerCase();
            String brailleChar = brailleMap.get(processedKey.toLowerCase());

            if (brailleChar != null) {
                // Apply mode transformations
                String finalChar = applyModeTransformation(brailleChar);
                inputConnection.commitText(finalChar, 1);
                charactersTyped++;
                updateStatusBar();
            } else {
                inputConnection.commitText(key, 1);
            }
        }
    }

    private String applyModeTransformation(String brailleChar) {
        switch (currentMode) {
            case "Grade 2":
                // Add contractions (simplified)
                if (brailleChar.equals("⠁⠝⠙")) return "⠯"; // "and" becomes single character
                if (brailleChar.equals("⠞⠓⠑")) return "⠮"; // "the" becomes single character
                return brailleChar + "⠄"; // Add grade 2 indicator
            case "8-Dot":
                return "⣿" + brailleChar; // Add 8-dot prefix
            case "Reversed":
                return new StringBuilder(brailleChar).reverse().toString();
            default:
                return brailleChar;
        }
    }

    private void toggleMode() {
        String[] modes = {"Grade 1", "Grade 2", "8-Dot", "Reversed"};
        for (int i = 0; i < modes.length; i++) {
            if (currentMode.equals(modes[i])) {
                currentMode = modes[(i + 1) % modes.length];
                break;
            }
        }
        updateStatusBar();
        // Recreate keyboard with new mode
        setInputView(onCreateInputView());
    }

    private void toggleCapsLock(Button capsBtn) {
        capsLockOn = !capsLockOn;
        capsBtn.setBackgroundColor(capsLockOn ? Color.parseColor("#FF5722") : Color.parseColor("#666666"));
        updateStatusBar();
        // Recreate keyboard to update button labels
        setInputView(onCreateInputView());
    }

    private void handleDelete() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.deleteSurroundingText(1, 0);
            if (charactersTyped > 0) charactersTyped--;
            updateStatusBar();
        }
    }

    private void handleEnter() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void handleSend() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            // Try common send key combinations
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void clearAll() {
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            // Clear the entire text field
            CharSequence allText = inputConnection.getTextBeforeCursor(1000, 0);
            if (allText != null) {
                inputConnection.deleteSurroundingText(allText.length(), 0);
            }
            charactersTyped = 0;
            updateStatusBar();
        }
    }

    private void updateStatusBar() {
        if (statusBar != null) {
            String capsStatus = capsLockOn ? " | CAPS ON" : "";
            statusBar.setText("🔸 Braille Keyboard Pro | Mode: " + currentMode + " | Characters: " + charactersTyped + capsStatus);
        }
    }
}