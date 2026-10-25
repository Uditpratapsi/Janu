package com.sukoon.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private EditText question, keyBox;
    private TextView status, answer;
    private Button mic, send, saveKey;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private SharedPreferences prefs;
    private String apiKey = "";
    private final int PERMISSION_MIC = 42;
    private final int BG = Color.rgb(24, 13, 35);
    private final int PINK = Color.rgb(255, 128, 200);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("sukoon", MODE_PRIVATE);
        apiKey = prefs.getString("api_key", "");
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
                public void onError(int e) {
                    status.setText("आवाज़ नहीं सुन पाई। दोबारा माइक्रोफोन दबाओ। (" + e + ")");
                }
                public void onResults(Bundle b) {
                    ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (r != null && !r.isEmpty()) { question.setText(r.get(0)); askAI(r.get(0)); }
                }
                public void onPartialResults(Bundle b) {}
                public void onEvent(int t, Bundle b) {}
            });
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
        title.setTextColor(PINK); title.setTextSize(28); title.setGravity(17);
        root.addView(title, lp(-1,-2,10));
        TextView hello = new TextView(this);
        hello.setText("हाँ जानू, मैं आ गई! ❤️");
        hello.setTextColor(Color.WHITE); hello.setTextSize(19); hello.setGravity(17);
        root.addView(hello, lp(-1,-2,16));

        keyBox = new EditText(this);
        keyBox.setSingleLine(true); keyBox.setHint("Gemini API key यहाँ डालें");
        keyBox.setTextColor(Color.WHITE); keyBox.setHintTextColor(0xffc8b8d0);
        keyBox.setInputType(129); keyBox.setText(apiKey);
        root.addView(keyBox, lp(-1,-2,6));
        saveKey = button("API KEY सेव करें");
        root.addView(saveKey, lp(-1,-2,8));
        saveKey.setOnClickListener(v -> {
            String k=keyBox.getText().toString().trim();
            if(k.isEmpty()) { status.setText("पहले Gemini API key डालो।"); return; }
            apiKey=k; prefs.edit().putString("api_key", k).apply();
            status.setText("API key इस फोन में सेव हो गई।");
        });

        question = new EditText(this);
        question.setHint("अपना सवाल यहाँ लिखो…");
        question.setTextColor(Color.WHITE); question.setHintTextColor(0xffc8b8d0);
        question.setMinLines(2); question.setGravity(48);
        root.addView(question, lp(-1,-2,12));
        mic = button("🎙️ बोलकर पूछो");
        root.addView(mic, lp(-1,-2,8));
        send = button("❤️ सुकून से पूछो");
        root.addView(send, lp(-1,-2,8));
        status = new TextView(this); status.setTextColor(PINK); status.setTextSize(15);
        root.addView(status, lp(-1,-2,12));
        answer = new TextView(this); answer.setTextColor(Color.WHITE); answer.setTextSize(18);
        answer.setTextIsSelectable(true);
        root.addView(answer, lp(-1,-2,12));
        setContentView(scroll);
        mic.setOnClickListener(v -> startMic());
        send.setOnClickListener(v -> askAI(question.getText().toString().trim()));
    }

    private LinearLayout.LayoutParams lp(int w,int h,int bottom) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.bottomMargin=bottom; return p;
    }
    private Button button(String text) {
        Button b=new Button(this); b.setText(text); b.setTextColor(Color.rgb(40,12,34));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PINK)); return b;
    }
    private void startMic() {
        if (recognizer == null) { status.setText("इस फोन पर Speech Recognition उपलब्ध नहीं है।"); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_MIC); return;
        }
        try {
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            recognizer.startListening(i);
        } catch(Exception e) { status.setText("माइक्रोफोन शुरू नहीं हुआ। सेटिंग और इंटरनेट जाँचो।"); }
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==PERMISSION_MIC && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) startMic();
        else status.setText("बोलकर पूछने के लिए माइक्रोफोन अनुमति ज़रूरी है।");
    }

    private void askAI(String prompt) {
        if(prompt==null || prompt.trim().isEmpty()) { status.setText("पहले सवाल लिखो या माइक्रोफोन दबाओ।"); return; }
        if(apiKey.isEmpty()) { status.setText("पहले ऊपर Gemini API key डालकर सेव करो।"); return; }
        status.setText("सुकून जवाब सोच रही है…"); send.setEnabled(false); mic.setEnabled(false);
        new Thread(() -> {
            HttpURLConnection c=null;
            try {
                String[] models={"gemini-2.5-flash","gemini-2.0-flash"};
                JSONObject data=null; String error="AI से जवाब नहीं मिला।";
                for(String model:models) {
                    try {
                        URL u=new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+apiKey);
                        c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST");
                        c.setConnectTimeout(15000); c.setReadTimeout(30000); c.setDoOutput(true);
                        c.setRequestProperty("Content-Type","application/json; charset=utf-8");
                        JSONObject body=new JSONObject();
                        JSONObject sys=new JSONObject(); sys.put("parts",new JSONArray().put(new JSONObject().put("text","तुम सुकून हो, एक प्यारी, समझदार और मददगार हिंदी AI सहायक। सरल हिंदी में जवाब दो। यूज़र को सम्मान से संबोधित करो।")));
                        body.put("systemInstruction",sys);
                        body.put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",new JSONArray().put(new JSONObject().put("text",prompt)))));
                        try(OutputStream os=c.getOutputStream()){ os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
                        int code=c.getResponseCode();
                        InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n;
                        while((n=is.read(buf))!=-1)out.write(buf,0,n);
                        JSONObject resp=new JSONObject(out.toString("UTF-8"));
                        if(code>=200&&code<300) { data=resp; break; }
                        error=resp.optJSONObject("error")!=null?resp.getJSONObject("error").optString("message","API error "+code):"API error "+code;
                    } finally { if(c!=null){c.disconnect();c=null;} }
                }
                if(data==null) throw new Exception(error);
                JSONArray parts=data.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
                StringBuilder sb=new StringBuilder(); for(int j=0;j<parts.length();j++) sb.append(parts.getJSONObject(j).optString("text",""));
                String reply=sb.toString().trim(); if(reply.isEmpty()) throw new Exception("खाली जवाब मिला।");
                runOnUiThread(() -> { answer.setText("💗 SUKOON: "+reply); status.setText("जवाब मिल गया ❤️"); if(tts!=null)tts.speak(reply,TextToSpeech.QUEUE_FLUSH,null,"sukoon_reply"); });
            } catch(Exception e) {
                String msg=e.getMessage()==null?"नेटवर्क या API में दिक्कत।":e.getMessage();
                runOnUiThread(() -> { status.setText("जवाब नहीं मिला: "+msg); });
            } finally { runOnUiThread(() -> {send.setEnabled(true); mic.setEnabled(true);}); }
        }).start();
    }
    @Override protected void onDestroy() {
        if(recognizer!=null) recognizer.destroy();
        if(tts!=null){tts.stop();tts.shutdown();}
        super.onDestroy();
    }
}
