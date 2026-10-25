package com.sukoon.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText question;
    private TextView status, answer;
    private Button mic, send, download;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private volatile boolean modelDownloading = false;
    private final int PERMISSION_MIC = 42;
    private final int BG = Color.rgb(24, 13, 35);
    private final int PINK = Color.rgb(255, 128, 200);
    private static final String MODEL_FILE = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm";
    private static final String MODEL_URL = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("hi", "IN"));
        });
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle p) { status.setText("सुन रही हूँ, जानू…"); }
                public void onBeginningOfSpeech() { status.setText("बोलते रहो…"); }
                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() { status.setText("तुम्हारी बात समझ रही हूँ…"); }
                public void onError(int e) { status.setText("आवाज़ नहीं सुन पाई। दोबारा माइक्रोफोन दबाओ। (" + e + ")"); }
                public void onResults(Bundle b) {
                    ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null && !r.isEmpty()) { question.setText(r.get(0)); askAI(r.get(0)); }
                }
                public void onPartialResults(Bundle b) {}
                public void onEvent(int t, Bundle b) {}
            });
        }
        File model = getModelFile();
        if (model.exists() && model.length() > 100_000_000L) {
            status.setText("मॉडल फोन में मौजूद है। सुकून तैयार करने के लिए ‘मॉडल चालू करें’ दबाओ।");
            download.setText("🧠 मॉडल चालू करें");
        }
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 34, 24, 24);
        root.setBackgroundColor(BG);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("🌸 SUKOON AI 🌸");
        title.setTextColor(PINK); title.setTextSize(28); title.setGravity(Gravity.CENTER);
        root.addView(title, lp(-1, -2, 10));

        TextView hello = new TextView(this);
        hello.setText("हाँ जानू, मैं आ गई! ❤️");
        hello.setTextColor(Color.WHITE); hello.setTextSize(19); hello.setGravity(Gravity.CENTER);
        root.addView(hello, lp(-1, -2, 8));

        TextView explain = new TextView(this);
        explain.setText("बिना API key वाला लोकल AI। मॉडल एक बार डाउनलोड होगा; उसके बाद सवाल फोन पर ही प्रोसेस होंगे।");
        explain.setTextColor(0xffe5d6ed); explain.setTextSize(14); explain.setGravity(Gravity.CENTER);
        root.addView(explain, lp(-1, -2, 14));

        download = button("⬇️ AI मॉडल डाउनलोड करें (लगभग 329 MB)");
        root.addView(download, lp(-1, -2, 8));
        download.setOnClickListener(v -> {
            File model = getModelFile();
            if (model.exists() && model.length() > 100_000_000L) {
                loadModel(model);
            } else {
                downloadModel();
            }
        });

        question = new EditText(this);
        question.setHint("अपना सवाल यहाँ लिखो…");
        question.setTextColor(Color.WHITE); question.setHintTextColor(0xffc8b8d0);
        question.setMinLines(2); question.setGravity(Gravity.TOP);
        root.addView(question, lp(-1, -2, 12));

        mic = button("🎙️ बोलकर पूछो");
        root.addView(mic, lp(-1, -2, 8));
        send = button("❤️ सुकून से पूछो");
        root.addView(send, lp(-1, -2, 8));

        status = new TextView(this); status.setTextColor(PINK); status.setTextSize(15);
        root.addView(status, lp(-1, -2, 12));
        answer = new TextView(this); answer.setTextColor(Color.WHITE); answer.setTextSize(18);
        answer.setTextIsSelectable(true);
        root.addView(answer, lp(-1, -2, 12));
        setContentView(scroll);

        mic.setOnClickListener(v -> startMic());
        send.setOnClickListener(v -> askAI(question.getText().toString().trim()));
    }

    private LinearLayout.LayoutParams lp(int w, int h, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.bottomMargin = bottom; return p;
    }

    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.rgb(40,12,34));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PINK)); return b;
    }

    private File getModelFile() {
        File dir = new File(getFilesDir(), "models");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, MODEL_FILE);
    }

    private void downloadModel() {
        if (modelDownloading) return;
        modelDownloading = true;
        download.setEnabled(false);
        status.setText("मॉडल डाउनलोड हो रहा है। लगभग 329 MB डेटा लगेगा; Wi‑Fi हो तो बेहतर है।");
        new Thread(() -> {
            HttpURLConnection c = null;
            File target = getModelFile();
            File temp = new File(target.getAbsolutePath() + ".part");
            try {
                c = (HttpURLConnection) new URL(MODEL_URL).openConnection();
                c.setConnectTimeout(20000); c.setReadTimeout(60000);
                c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent", "SukoonAI-Android/2.0");
                int code = c.getResponseCode();
                if (code < 200 || code >= 300) throw new IOException("डाउनलोड सर्वर ने HTTP " + code + " दिया।");
                long total = c.getContentLengthLong();
                try (InputStream in = new BufferedInputStream(c.getInputStream());
                     FileOutputStream out = new FileOutputStream(temp)) {
                    byte[] buf = new byte[1024 * 128]; long done = 0; int n; long last = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n); done += n;
                        if (done - last > 2_000_000L) {
                            long d = done;
                            runOnUiThread(() -> status.setText("मॉडल डाउनलोड: " + (d / (1024 * 1024)) + " MB" +
                                    (total > 0 ? " / " + (total / (1024 * 1024)) + " MB" : "")));
                            last = done;
                        }
                    }
                    out.flush();
                }
                if (temp.length() < 100_000_000L) throw new IOException("डाउनलोड अधूरा है; दोबारा कोशिश करें।");
                if (target.exists()) target.delete();
                if (!temp.renameTo(target)) throw new IOException("मॉडल फाइल सेव नहीं हो पाई। फोन की खाली जगह जाँचें।");
                runOnUiThread(() -> {
                    status.setText("मॉडल डाउनलोड पूरा हुआ ❤️ अब इसे फोन पर तैयार कर रही हूँ…");
                    download.setText("🧠 मॉडल चालू करें");
                    download.setEnabled(true);
                    modelDownloading = false;
                    loadModel(target);
                });
            } catch (Exception e) {
                temp.delete();
                String msg = e.getMessage() == null ? "डाउनलोड में दिक्कत हुई।" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("मॉडल डाउनलोड नहीं हुआ: " + msg);
                    download.setEnabled(true); modelDownloading = false;
                });
            } finally { if (c != null) c.disconnect(); }
        }).start();
    }

    private void loadModel(File model) {
        download.setEnabled(false);
        status.setText("सुकून का लोकल AI चालू हो रहा है। पहली बार कुछ समय लग सकता है…");
        new Thread(() -> {
            try {
                LocalModelRunner.load(model.getAbsolutePath());
                runOnUiThread(() -> {
                    status.setText("सुकून तैयार है ❤️ अब बिना API key और बिना इंटरनेट सवाल पूछ सकते हो।");
                    download.setText("✅ लोकल AI तैयार है");
                    download.setEnabled(true);
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("लोकल मॉडल शुरू नहीं हुआ: " + msg);
                    download.setText("🔄 मॉडल फिर से चालू करें");
                    download.setEnabled(true);
                });
            }
        }).start();
    }

    private void startMic() {
        if (recognizer == null) { status.setText("इस फोन पर Speech Recognition उपलब्ध नहीं है। लिखकर पूछ सकते हो।"); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_MIC); return;
        }
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            recognizer.startListening(i);
        } catch (Exception e) { status.setText("माइक्रोफोन शुरू नहीं हुआ। अनुमति जाँचो।"); }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERMISSION_MIC && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startMic();
        else status.setText("बोलकर पूछने के लिए माइक्रोफोन अनुमति ज़रूरी है।");
    }

    private void askAI(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) { status.setText("पहले सवाल लिखो या माइक्रोफोन दबाओ।"); return; }
        if (!LocalModelRunner.isLoaded()) {
            status.setText("पहले ऊपर मॉडल डाउनलोड करके ‘मॉडल चालू करें’ दबाओ।");
            return;
        }
        status.setText("सुकून फोन पर जवाब सोच रही है…");
        send.setEnabled(false); mic.setEnabled(false);
        new Thread(() -> {
            try {
                String reply = LocalModelRunner.ask(prompt);
                runOnUiThread(() -> {
                    answer.setText("💗 SUKOON: " + reply);
                    status.setText("जवाब मिल गया ❤️ (लोकल AI)");
                    if (tts != null) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "sukoon_reply");
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "लोकल AI में दिक्कत हुई।" : e.getMessage();
                runOnUiThread(() -> status.setText("जवाब नहीं मिला: " + msg));
            } finally {
                runOnUiThread(() -> { send.setEnabled(true); mic.setEnabled(true); });
            }
        }).start();
    }

    @Override protected void onDestroy() {
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        new Thread(() -> LocalModelRunner.close()).start();
        super.onDestroy();
    }
}
