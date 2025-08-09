package com.example.braillekeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.KeyEvent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import androidx.core.content.FileProvider;

public class BrailleKeyboard extends InputMethodService {

    private Map<String, String> brailleMap;
    private Map<String, String> morseMap;
    private TextView statusBar;
    private TextView composingArea;
    private int charactersTyped = 0;
    private boolean capsLockOn = false;
    private String currentMode = "Grade 1";
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean isComposingMode = false;

    // Modern color scheme
    private final int BACKGROUND_DARK = Color.parseColor("#121212");
    private final int SURFACE_DARK = Color.parseColor("#1E1E1E");
    private final int PRIMARY = Color.parseColor("#BB86FC");
    private final int SECONDARY = Color.parseColor("#03DAC6");
    private final int ERROR = Color.parseColor("#CF6679");
    private final int WARNING = Color.parseColor("#FFA726");
    private final int SUCCESS = Color.parseColor("#4CAF50");
    private final int TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    private final int TEXT_SECONDARY = Color.parseColor("#B3B3B3");
    private final int KEY_NORMAL = Color.parseColor("#2C2C2C");
    private final int KEY_PRESSED = Color.parseColor("#3C3C3C");

    // Audio generation constants
    private final int SAMPLE_RATE = 44100;
    private final int DOT_DURATION_MS = 100;
    private final int DASH_DURATION_MS = 300;
    private final int INTER_ELEMENT_PAUSE_MS = 100;
    private final int INTER_LETTER_PAUSE_MS = 300;
    private final int INTER_WORD_PAUSE_MS = 700;
    private final int FREQUENCY = 800; // Tone frequency in Hz

    @Override
    public void onCreate() {
        super.onCreate();
        initBrailleMap();
        initMorseMap();
    }

    private void initBrailleMap() {
        brailleMap = new HashMap<>();
        brailleMap.put("a", "⠁"); brailleMap.put("b", "⠃"); brailleMap.put("c", "⠉");
        brailleMap.put("d", "⠙"); brailleMap.put("e", "⠑"); brailleMap.put("f", "⠋");
        brailleMap.put("g", "⠛"); brailleMap.put("h", "⠓"); brailleMap.put("i", "⠊");
        brailleMap.put("j", "⠚"); brailleMap.put("k", "⠅"); brailleMap.put("l", "⠇");
        brailleMap.put("m", "⠍"); brailleMap.put("n", "⠝"); brailleMap.put("o", "⠕");
        brailleMap.put("p", "⠏"); brailleMap.put("q", "⠟"); brailleMap.put("r", "⠗");
        brailleMap.put("s", "⠎"); brailleMap.put("t", "⠞"); brailleMap.put("u", "⠥");
        brailleMap.put("v", "⠧"); brailleMap.put("w", "⠺"); brailleMap.put("x", "⠭");
        brailleMap.put("y", "⠽"); brailleMap.put("z", "⠵");

        brailleMap.put("1", "⠼⠁"); brailleMap.put("2", "⠼⠃"); brailleMap.put("3", "⠼⠉");
        brailleMap.put("4", "⠼⠙"); brailleMap.put("5", "⠼⠑"); brailleMap.put("6", "⠼⠋");
        brailleMap.put("7", "⠼⠛"); brailleMap.put("8", "⠼⠓"); brailleMap.put("9", "⠼⠊");
        brailleMap.put("0", "⠼⠚");

        brailleMap.put(" ", " ");
        brailleMap.put(".", "⠲"); brailleMap.put(",", "⠂"); brailleMap.put("?", "⠦");
        brailleMap.put("!", "⠖"); brailleMap.put(":", "⠒"); brailleMap.put(";", "⠆");
        brailleMap.put("'", "⠄"); brailleMap.put("-", "⠤"); brailleMap.put("(", "⠣");
        brailleMap.put(")", "⠜"); brailleMap.put("@", "⠈⠁"); brailleMap.put("#", "⠼");
    }

    private void initMorseMap() {
        morseMap = new HashMap<>();
        morseMap.put("a", ".-"); morseMap.put("b", "-..."); morseMap.put("c", "-.-.");
        morseMap.put("d", "-.."); morseMap.put("e", "."); morseMap.put("f", "..-.");
        morseMap.put("g", "--."); morseMap.put("h", "...."); morseMap.put("i", "..");
        morseMap.put("j", ".---"); morseMap.put("k", "-.-"); morseMap.put("l", ".-..");
        morseMap.put("m", "--"); morseMap.put("n", "-."); morseMap.put("o", "---");
        morseMap.put("p", ".--."); morseMap.put("q", "--.-"); morseMap.put("r", ".-.");
        morseMap.put("s", "..."); morseMap.put("t", "-"); morseMap.put("u", "..-");
        morseMap.put("v", "...-"); morseMap.put("w", ".--"); morseMap.put("x", "-..-");
        morseMap.put("y", "-.--"); morseMap.put("z", "--.."); morseMap.put(" ", "/");
        morseMap.put("1", ".----"); morseMap.put("2", "..---"); morseMap.put("3", "...--");
        morseMap.put("4", "....-"); morseMap.put("5", "....."); morseMap.put("6", "-....");
        morseMap.put("7", "--..."); morseMap.put("8", "---.."); morseMap.put("9", "----.");
        morseMap.put("0", "-----"); morseMap.put("?", "..--.."); morseMap.put("!", "-.-.--");
        morseMap.put(".", ".-.-.-"); morseMap.put(",", "--..--"); morseMap.put(":", "---...");
        morseMap.put(";", "-.-.-."); morseMap.put("'", ".----."); morseMap.put("-", "-....-");
        morseMap.put("(", "-.--."); morseMap.put(")", "-.--.-");
    }

    @Override
    public View onCreateInputView() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackground(createGradientBackground());
        mainLayout.setPadding(12, 12, 12, 12);

        // Elegant status bar with gradient
        createStatusBar(mainLayout);

        // Composing area for morse messages
        createComposingArea(mainLayout);

        // Mode control strip
        createModeControlStrip(mainLayout);

        // Number row with rounded corners
        createStyledKeyRow(mainLayout, new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}, "number");

        // Main keyboard rows
        createStyledKeyRow(mainLayout, new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}, "letter");
        createStyledKeyRow(mainLayout, new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"}, "letter");

        // Bottom letter row with backspace
        createBottomRow(mainLayout);

        // Punctuation with modern styling
        createStyledKeyRow(mainLayout, new String[]{".", ",", "?", "!", ":", ";", "'", "-"}, "punctuation");

        // Action bar (space, enter, morse send)
        createActionRow(mainLayout);

        return mainLayout;
    }

    private void createComposingArea(LinearLayout parent) {
        LinearLayout composingContainer = new LinearLayout(this);
        composingContainer.setOrientation(LinearLayout.VERTICAL);
        composingContainer.setBackground(createRoundedBackground(SURFACE_DARK, 12f));
        composingContainer.setPadding(12, 8, 12, 8);

        // Label with status
        TextView label = new TextView(this);
        String labelText = isComposingMode ? "📝 Compose Mode: ON - Type below" : "📝 Compose Mode: OFF";
        label.setText(labelText);
        label.setTextColor(isComposingMode ? SUCCESS : TEXT_SECONDARY);
        label.setTextSize(12);
        label.setTypeface(null, Typeface.BOLD);
        composingContainer.addView(label);

        // Composing text area
        composingArea = new TextView(this); // Changed from EditText to TextView
        composingArea.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120));
        composingArea.setBackground(createRoundedBackground(Color.parseColor("#2A2A2A"), 8f));
        composingArea.setTextColor(TEXT_PRIMARY);
        composingArea.setTextSize(14);
        composingArea.setPadding(12, 8, 12, 8);

        // Set initial text
        if (composingArea.getText().toString().isEmpty()) {
            composingArea.setText(isComposingMode ? "" : "Turn on Compose Mode to type here...");
            composingArea.setTextColor(isComposingMode ? TEXT_PRIMARY : TEXT_SECONDARY);
        }

        composingContainer.addView(composingArea);

        parent.addView(composingContainer);
    }

    private GradientDrawable createGradientBackground() {
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
        gradient.setColors(new int[]{
                Color.parseColor("#1A1A1A"),
                Color.parseColor("#121212")
        });
        gradient.setCornerRadius(24f);
        return gradient;
    }

    private void createStatusBar(LinearLayout parent) {
        LinearLayout statusContainer = new LinearLayout(this);
        statusContainer.setOrientation(LinearLayout.HORIZONTAL);
        statusContainer.setBackground(createRoundedBackground(SURFACE_DARK, 16f));
        statusContainer.setPadding(16, 12, 16, 12);

        // Status icon
        TextView icon = new TextView(this);
        icon.setText("⠿");
        icon.setTextColor(PRIMARY);
        icon.setTextSize(18);
        icon.setTypeface(null, Typeface.BOLD);
        statusContainer.addView(icon);

        // Status text
        statusBar = new TextView(this);
        updateStatusBar();
        statusBar.setTextColor(TEXT_PRIMARY);
        statusBar.setTextSize(13);
        statusBar.setPadding(12, 0, 0, 0);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        statusBar.setLayoutParams(statusParams);
        statusContainer.addView(statusBar);

        // Mode indicator
        TextView modeIndicator = new TextView(this);
        modeIndicator.setText(getModeEmoji());
        modeIndicator.setTextSize(16);
        statusContainer.addView(modeIndicator);

        parent.addView(statusContainer);
    }

    private void createModeControlStrip(LinearLayout parent) {
        LinearLayout controlStrip = new LinearLayout(this);
        controlStrip.setOrientation(LinearLayout.HORIZONTAL);
        controlStrip.setPadding(0, 8, 0, 8);

        Button modeBtn = createControlButton("MODE", PRIMARY, "🔄");
        modeBtn.setOnClickListener(v -> toggleMode());
        controlStrip.addView(modeBtn);

        Button capsBtn = createControlButton("CAPS", capsLockOn ? WARNING : Color.parseColor("#404040"), "⇧");
        capsBtn.setOnClickListener(v -> toggleCapsLock(capsBtn));
        controlStrip.addView(capsBtn);

        Button composeBtn = createControlButton("COMPOSE", isComposingMode ? SUCCESS : Color.parseColor("#404040"), "📝");
        composeBtn.setOnClickListener(v -> toggleComposingMode(composeBtn));
        controlStrip.addView(composeBtn);

        Button clearBtn = createControlButton("CLEAR", ERROR, "🗑");
        clearBtn.setOnClickListener(v -> clearAll());
        controlStrip.addView(clearBtn);

        parent.addView(controlStrip);
    }

    private Button createControlButton(String text, int color, String emoji) {
        Button button = new Button(this);
        button.setText(emoji + " " + text);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, 100, 1.0f));
        button.setBackground(createRoundedBackground(color, 12f));
        button.setTextColor(TEXT_PRIMARY);
        button.setTextSize(10);
        button.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMargins(3, 0, 3, 0);

        return button;
    }

    private void createStyledKeyRow(LinearLayout parent, String[] keys, String keyType) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 4, 0, 4);

        for (String key : keys) {
            Button button = createStyledKey(key, keyType);
            row.addView(button);
        }

        parent.addView(row);
    }

    private void createBottomRow(LinearLayout parent) {
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setPadding(0, 4, 0, 4);

        String[] bottomKeys = {"z", "x", "c", "v", "b", "n", "m"};
        for (String key : bottomKeys) {
            Button button = createStyledKey(key, "letter");
            bottomRow.addView(button);
        }

        // Elegant backspace button
        Button delBtn = new Button(this);
        delBtn.setText("⌫");
        delBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 2.0f));
        delBtn.setBackground(createRoundedBackground(ERROR, 12f));
        delBtn.setTextColor(TEXT_PRIMARY);
        delBtn.setTextSize(18);
        delBtn.setOnClickListener(v -> handleDelete());

        LinearLayout.LayoutParams delParams = (LinearLayout.LayoutParams) delBtn.getLayoutParams();
        delParams.setMargins(4, 0, 4, 0);
        bottomRow.addView(delBtn);

        parent.addView(bottomRow);
    }

    private void createActionRow(LinearLayout parent) {
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 8, 0, 0);

        // Space bar with gradient
        Button spaceBtn = new Button(this);
        spaceBtn.setText("SPACE");
        spaceBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 140, 2.5f));
        spaceBtn.setBackground(createGradientButton(SECONDARY, SUCCESS));
        spaceBtn.setTextColor(TEXT_PRIMARY);
        spaceBtn.setTextSize(12);
        spaceBtn.setTypeface(null, Typeface.BOLD);
        spaceBtn.setOnClickListener(v -> handleKeyPress(" "));

        LinearLayout.LayoutParams spaceParams = (LinearLayout.LayoutParams) spaceBtn.getLayoutParams();
        spaceParams.setMargins(4, 0, 4, 0);
        actionRow.addView(spaceBtn);

        // Send Morse button
        Button morseBtn = new Button(this);
        morseBtn.setText("Send Morse 🔊");
        morseBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 140, 2.5f));
        morseBtn.setBackground(createGradientButton(WARNING, Color.parseColor("#FF8A65")));
        morseBtn.setTextColor(TEXT_PRIMARY);
        morseBtn.setTextSize(12);
        morseBtn.setTypeface(null, Typeface.BOLD);
        morseBtn.setOnClickListener(v -> generateAndShareMorseAudio());

        LinearLayout.LayoutParams morseParams = (LinearLayout.LayoutParams) morseBtn.getLayoutParams();
        morseParams.setMargins(4, 0, 4, 0);
        actionRow.addView(morseBtn);

        // Enter button with icon
        Button enterBtn = new Button(this);
        enterBtn.setText("⏎");
        enterBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 140, 1.0f));
        enterBtn.setBackground(createRoundedBackground(PRIMARY, 16f));
        enterBtn.setTextColor(TEXT_PRIMARY);
        enterBtn.setTextSize(20);
        enterBtn.setOnClickListener(v -> handleEnter());

        LinearLayout.LayoutParams enterParams = (LinearLayout.LayoutParams) enterBtn.getLayoutParams();
        enterParams.setMargins(4, 0, 4, 0);
        actionRow.addView(enterBtn);

        parent.addView(actionRow);
    }

    private Button createStyledKey(String key, String keyType) {
        Button button = new Button(this);
        button.setText(capsLockOn ? key.toUpperCase() : key);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 1.0f));

        int backgroundColor = getKeyColor(keyType);
        button.setBackground(createRoundedBackground(backgroundColor, 8f));
        button.setTextColor(TEXT_PRIMARY);
        button.setTextSize(keyType.equals("number") ? 14 : 16);
        button.setTypeface(null, keyType.equals("letter") ? Typeface.BOLD : Typeface.NORMAL);
        button.setOnClickListener(v -> handleKeyPress(key));

        // Add subtle margins for spacing
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) button.getLayoutParams();
        params.setMargins(2, 0, 2, 0);

        return button;
    }

    private int getKeyColor(String keyType) {
        switch (keyType) {
            case "number": return Color.parseColor("#2D2D30");
            case "letter": return KEY_NORMAL;
            case "punctuation": return Color.parseColor("#363636");
            default: return KEY_NORMAL;
        }
    }

    private GradientDrawable createRoundedBackground(int color, float radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable createGradientButton(int startColor, int endColor) {
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        gradient.setColors(new int[]{startColor, endColor});
        gradient.setCornerRadius(12f);
        return gradient;
    }

    private String getModeEmoji() {
        switch (currentMode) {
            case "Grade 1": return "⠁";
            case "Grade 2": return "⠃";
            case "8-Dot": return "⣿";
            case "Reversed": return "⠸";
            case "Malayalam Morse": return "📻";
            default: return "⠿";
        }
    }

    private void generateAndShareMorseAudio() {
        String message = composingArea.getText().toString().trim();

        // Skip placeholder text
        if (message.isEmpty() || message.equals("Turn on Compose Mode to type here...")) {
            // Show some feedback that message is empty
            composingArea.setText("Please type a message first!");
            composingArea.setTextColor(ERROR);

            // Reset after 2 seconds
            handler.postDelayed(() -> {
                if (isComposingMode) {
                    composingArea.setText("");
                    composingArea.setTextColor(TEXT_PRIMARY);
                } else {
                    composingArea.setText("Turn on Compose Mode to type here...");
                    composingArea.setTextColor(TEXT_SECONDARY);
                }
            }, 2000);
            return;
        }

        try {
            // Show processing status
            composingArea.setText("Converting to morse audio...");
            composingArea.setTextColor(WARNING);

            // Convert text to morse code
            String morseCode = textToMorse(message);
            composingArea.setText("Morse: " + morseCode);

            // Generate audio file
            handler.postDelayed(() -> {
                try {
                    File audioFile = generateMorseAudioFile(morseCode);

                    if (audioFile != null && audioFile.exists()) {
                        // Show file details for debugging
                        composingArea.setText("Audio created: " + audioFile.getName() + " (" + audioFile.length() + " bytes)");
                        composingArea.setTextColor(SUCCESS);

                        // Share the audio file after a short delay
                        handler.postDelayed(() -> {
                            try {
                                shareAudioFile(audioFile);
                                // Don't assume success - let shareAudioFile handle the status
                            } catch (Exception e) {
                                composingArea.setText("Share failed: " + e.getMessage());
                                composingArea.setTextColor(ERROR);
                                e.printStackTrace();
                            }
                        }, 1500);
                    } else {
                        composingArea.setText("Error: Audio file not created");
                        composingArea.setTextColor(ERROR);
                    }
                } catch (Exception e) {
                    composingArea.setText("Error generating audio: " + e.getMessage());
                    composingArea.setTextColor(ERROR);
                }
            }, 1000);

        } catch (Exception e) {
            e.printStackTrace();
            composingArea.setText("Error: " + e.getMessage());
            composingArea.setTextColor(ERROR);

            handler.postDelayed(() -> {
                composingArea.setText("");
                composingArea.setTextColor(TEXT_PRIMARY);
            }, 3000);
        }
    }

    private String textToMorse(String text) {
        StringBuilder morseBuilder = new StringBuilder();
        text = text.toLowerCase();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String morse = morseMap.get(String.valueOf(c));

            if (morse != null) {
                morseBuilder.append(morse);
                if (i < text.length() - 1) {
                    morseBuilder.append(" ");
                }
            } else if (c == ' ') {
                morseBuilder.append(" / ");
            } else {
                // For unknown characters, just add a space
                morseBuilder.append(" ");
            }
        }

        return morseBuilder.toString();
    }

    private File generateMorseAudioFile(String morseCode) throws IOException {
        // Use app's private external files directory - no permissions needed
        File outputDir = getExternalFilesDir(null);
        if (outputDir == null) {
            // Fallback to cache directory
            outputDir = getCacheDir();
        }

        File morseDir = new File(outputDir, "MorseAudio");
        if (!morseDir.exists()) {
            morseDir.mkdirs();
        }

        // Create unique filename with timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis();
        File audioFile = new File(morseDir, "morse_" + timestamp + ".wav");

        // Generate PCM audio data using custom sounds
        byte[] audioData = generateCustomMorsePCM(morseCode);

        // Save as WAV file
        saveAsWav(audioFile, audioData);

        return audioFile;
    }

    private byte[] generateCustomMorsePCM(String morseCode) {
        List<byte[]> audioSegments = new ArrayList<>();
        int totalLength = 0;

        for (int i = 0; i < morseCode.length(); i++) {
            char c = morseCode.charAt(i);
            byte[] segment = null;

            switch (c) {
                case '.':
                    segment = loadAudioFromResource(R.raw.eh);
                    break;
                case '-':
                    segment = loadAudioFromResource(R.raw.eeeeh);
                    break;
                case '?':
                    segment = loadAudioFromResource(R.raw.huh);
                    break;
                case ' ':
                    segment = generateSilencePCM(INTER_LETTER_PAUSE_MS);
                    break;
                case '/':
                    segment = generateSilencePCM(INTER_WORD_PAUSE_MS);
                    break;
                default:
                    segment = generateSilencePCM(INTER_ELEMENT_PAUSE_MS);
                    break;
            }

            if (segment != null) {
                audioSegments.add(segment);
                totalLength += segment.length;

                // Add inter-element pause after dots and dashes
                if (c == '.' || c == '-') {
                    byte[] pause = generateSilencePCM(INTER_ELEMENT_PAUSE_MS);
                    audioSegments.add(pause);
                    totalLength += pause.length;
                }
            }
        }

        // Combine all segments
        byte[] finalAudio = new byte[totalLength];
        int currentPos = 0;

        for (byte[] segment : audioSegments) {
            System.arraycopy(segment, 0, finalAudio, currentPos, segment.length);
            currentPos += segment.length;
        }

        return finalAudio;
    }

    private byte[] loadAudioFromResource(int resourceId) {
        try {
            InputStream inputStream = getResources().openRawResource(resourceId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            byte[] audioData = outputStream.toByteArray();
            outputStream.close();

            // Skip WAV header if present (first 44 bytes for standard WAV)
            if (audioData.length > 44 &&
                    audioData[0] == 'R' && audioData[1] == 'I' &&
                    audioData[2] == 'F' && audioData[3] == 'F') {
                byte[] pcmData = new byte[audioData.length - 44];
                System.arraycopy(audioData, 44, pcmData, 0, pcmData.length);
                return pcmData;
            }

            return audioData;

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to silence if audio loading fails
            return generateSilencePCM(200);
        }
    }

    private byte[] generateSilencePCM(int durationMs) {
        int numSamples = (int) (durationMs * SAMPLE_RATE / 1000.0);
        byte[] silenceData = new byte[numSamples * 2]; // 16-bit audio

        // Fill with zeros (silence)
        for (int i = 0; i < silenceData.length; i++) {
            silenceData[i] = 0;
        }

        return silenceData;
    }

    private void saveAsWav(File file, byte[] pcmData) throws IOException {
        FileOutputStream out = new FileOutputStream(file);

        // WAV header
        int dataLength = pcmData.length;
        int fileLength = dataLength + 36;

        // RIFF header
        out.write("RIFF".getBytes());
        out.write(intToByteArray(fileLength), 0, 4);
        out.write("WAVE".getBytes());

        // Format chunk
        out.write("fmt ".getBytes());
        out.write(intToByteArray(16), 0, 4); // Chunk size
        out.write(shortToByteArray((short) 1), 0, 2); // Audio format (PCM)
        out.write(shortToByteArray((short) 1), 0, 2); // Number of channels
        out.write(intToByteArray(SAMPLE_RATE), 0, 4); // Sample rate
        out.write(intToByteArray(SAMPLE_RATE * 2), 0, 4); // Byte rate
        out.write(shortToByteArray((short) 2), 0, 2); // Block align
        out.write(shortToByteArray((short) 16), 0, 2); // Bits per sample

        // Data chunk
        out.write("data".getBytes());
        out.write(intToByteArray(dataLength), 0, 4);
        out.write(pcmData);

        out.close();
    }

    private byte[] intToByteArray(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff)
        };
    }

    private byte[] shortToByteArray(short value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)
        };
    }

    private void shareAudioFile(File audioFile) {
        try {
            composingArea.setText("Preparing to share...");
            composingArea.setTextColor(WARNING);

            // Check if file exists and has content
            if (!audioFile.exists()) {
                composingArea.setText("Error: Audio file doesn't exist");
                composingArea.setTextColor(ERROR);
                return;
            }

            if (audioFile.length() == 0) {
                composingArea.setText("Error: Audio file is empty");
                composingArea.setTextColor(ERROR);
                return;
            }

            // Create the share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/wav");

            Uri fileUri;

            // Try to use FileProvider for Android 7.0+ compatibility
            try {
                fileUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", audioFile);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                // Fallback to direct file URI for older Android versions
                fileUri = Uri.fromFile(audioFile);
            }

            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Morse Code Audio");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Morse code audio message");

            // Add flags
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Create chooser
            Intent chooser = Intent.createChooser(shareIntent, "Share Morse Audio");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            composingArea.setText("Opening share dialog...");

            // Try to start the activity
            startActivity(chooser);

            // If we get here without exception, assume success
            composingArea.setText("Share dialog opened successfully!");
            composingArea.setTextColor(SUCCESS);

            // Clear after delay
            handler.postDelayed(() -> {
                composingArea.setText("");
                composingArea.setTextColor(TEXT_PRIMARY);
            }, 3000);

        } catch (android.content.ActivityNotFoundException e) {
            composingArea.setText("No apps found to share audio");
            composingArea.setTextColor(ERROR);
        } catch (SecurityException e) {
            composingArea.setText("Permission denied. Check storage permissions");
            composingArea.setTextColor(ERROR);
        } catch (Exception e) {
            composingArea.setText("Share error: " + e.getClass().getSimpleName());
            composingArea.setTextColor(ERROR);
            e.printStackTrace();
        }
    }

    private void toggleComposingMode(Button composeBtn) {
        isComposingMode = !isComposingMode;
        composeBtn.setBackground(createRoundedBackground(
                isComposingMode ? SUCCESS : Color.parseColor("#404040"), 12f));

        // Update the entire keyboard view to reflect the mode change
        setInputView(onCreateInputView());

        updateStatusBar();
    }

    // Rest of your existing methods remain unchanged
    private void handleKeyPress(String key) {
        // Handle composing mode - type into composing area instead
        if (isComposingMode && composingArea != null) {
            String currentText = composingArea.getText().toString();

            // Clear placeholder text when first typing
            if (currentText.equals("Turn on Compose Mode to type here...")) {
                currentText = "";
                composingArea.setTextColor(TEXT_PRIMARY);
            }

            String processedKey = capsLockOn ? key.toUpperCase() : key.toLowerCase();
            composingArea.setText(currentText + processedKey);
            return;
        }

        // Normal keyboard behavior
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            String processedKey = capsLockOn ? key.toUpperCase() : key.toLowerCase();

            if (currentMode.equals("Malayalam Morse")) {
                handleMorseMode(processedKey, inputConnection);
            } else {
                String brailleChar = brailleMap.get(processedKey.toLowerCase());

                if (brailleChar != null) {
                    String finalChar = applyModeTransformation(brailleChar);
                    inputConnection.commitText(finalChar, 1);
                    charactersTyped++;
                    updateStatusBar();
                } else {
                    inputConnection.commitText(key, 1);
                }
            }
        }
    }

    private String applyModeTransformation(String brailleChar) {
        switch (currentMode) {
            case "Grade 2":
                if (brailleChar.equals("⠁⠝⠙")) return "⠯";
                if (brailleChar.equals("⠞⠓⠑")) return "⠮";
                return brailleChar + "⠄";
            case "8-Dot":
                return "⣿" + brailleChar;
            case "Reversed":
                return new StringBuilder(brailleChar).reverse().toString();
            default:
                return brailleChar;
        }
    }

    private void handleMorseMode(String key, InputConnection inputConnection) {
        String morseCode = morseMap.get(key.toLowerCase());
        if (morseCode != null) {
            inputConnection.commitText(morseCode + " ", 1);
            playMalayalamMorse(morseCode);
            charactersTyped++;
            updateStatusBar();
        } else if (key.equals("?")) {
            inputConnection.commitText("..--.. ", 1);
            playMalayalamMorse("..-..");
        } else {
            inputConnection.commitText(key, 1);
        }
    }

    private void playMalayalamMorse(String morseCode) {
        playMorseSequence(morseCode, 0);
    }

    private void playMorseSequence(String morseCode, int index) {
        if (index >= morseCode.length()) return;

        char symbol = morseCode.charAt(index);
        int soundResource;
        int duration;

        switch (symbol) {
            case '.':
                soundResource = R.raw.eh;
                duration = 200;
                break;
            case '-':
                soundResource = R.raw.eeeeh;
                duration = 600;
                break;
            case '/':
                soundResource = R.raw.huh;
                duration = 400;
                break;
            case '?':
                soundResource = R.raw.huh;
                duration = 500;
                break;
            default:
                handler.postDelayed(() -> playMorseSequence(morseCode, index + 1), 100);
                return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, soundResource);
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }

            handler.postDelayed(() -> playMorseSequence(morseCode, index + 1), duration + 50); // Added 50ms buffer

        } catch (Exception e) {
            handler.postDelayed(() -> playMorseSequence(morseCode, index + 1), 150);
        }
    }

    private void toggleMode() {
        String[] modes = {"Grade 1", "Grade 2", "8-Dot", "Reversed", "Malayalam Morse"};
        for (int i = 0; i < modes.length; i++) {
            if (currentMode.equals(modes[i])) {
                currentMode = modes[(i + 1) % modes.length];
                break;
            }
        }
        updateStatusBar();
        setInputView(onCreateInputView());
    }

    private void toggleCapsLock(Button capsBtn) {
        capsLockOn = !capsLockOn;
        capsBtn.setBackground(createRoundedBackground(capsLockOn ? WARNING : Color.parseColor("#404040"), 12f));
        updateStatusBar();
        setInputView(onCreateInputView());
    }

    private void handleDelete() {
        // Handle composing mode deletion
        if (isComposingMode && composingArea != null) {
            String currentText = composingArea.getText().toString();

            // Don't delete placeholder text
            if (currentText.equals("Turn on Compose Mode to type here...")) {
                return;
            }

            if (currentText.length() > 0) {
                composingArea.setText(currentText.substring(0, currentText.length() - 1));
            }
            return;
        }

        // Normal deletion
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.deleteSurroundingText(1, 0);
            if (charactersTyped > 0) charactersTyped--;
            updateStatusBar();
        }
    }

    private void handleEnter() {
        // Handle composing mode enter
        if (isComposingMode && composingArea != null) {
            String currentText = composingArea.getText().toString();

            // Clear placeholder text when first typing
            if (currentText.equals("Turn on Compose Mode to type here...")) {
                currentText = "";
                composingArea.setTextColor(TEXT_PRIMARY);
            }

            composingArea.setText(currentText + "\n");
            return;
        }

        // Normal enter
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        }
    }

    private void clearAll() {
        // Handle composing mode clear
        if (isComposingMode && composingArea != null) {
            composingArea.setText("");
            return;
        }

        // Normal clear
        InputConnection inputConnection = getCurrentInputConnection();
        if (inputConnection != null) {
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
            String capsStatus = capsLockOn ? " • CAPS" : "";
            String composeStatus = isComposingMode ? " • COMPOSE" : "";
            statusBar.setText("Mode: " + currentMode + " • Characters: " + charactersTyped + capsStatus + composeStatus);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}