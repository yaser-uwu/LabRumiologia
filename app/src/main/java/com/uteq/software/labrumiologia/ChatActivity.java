package com.uteq.software.labrumiologia;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.uteq.software.labrumiologia.data.LocalGuide;
import com.uteq.software.labrumiologia.ui.ChatAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {
    private String equipmentId;
    private ChatAdapter adapter;
    private TextInputEditText input;
    private MaterialButton btnSend;
    private LocalGuide guide;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        equipmentId = getIntent().getStringExtra(DetectionActivity.EXTRA_EQUIPMENT_ID);
        String label = getIntent().getStringExtra(DetectionActivity.EXTRA_EQUIPMENT_LABEL);
        ((TextView) findViewById(R.id.chatEquipmentLabel))
                .setText(getString(R.string.chat_title, label != null ? label : equipmentId));

        RecyclerView messages = findViewById(R.id.chatMessages);
        messages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        messages.setAdapter(adapter);
        guide = new LocalGuide(this);

        input = findViewById(R.id.chatInput);
        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> sendMessage());
        adapter.add(new ChatAdapter.Message("Sistema", getString(R.string.chat_intro), null));
    }

    private void sendMessage() {
        String question = input.getText() != null ? input.getText().toString().trim() : "";
        if (question.isEmpty()) return;

        adapter.add(new ChatAdapter.Message("Usted", question, null));
        input.setText("");
        btnSend.setEnabled(false);
        adapter.add(new ChatAdapter.Message("Asistente", getString(R.string.chat_consulting), null, true));

        io.execute(() -> {
            LocalGuide.Reply reply = guide.ask(question, equipmentId);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                btnSend.setEnabled(true);
                adapter.removeLastIfPlaceholder();
                adapter.add(new ChatAdapter.Message("Asistente", reply.answer, reply.sources));
            });
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }
}
