package com.uteq.software.labrumiologia;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.uteq.software.labrumiologia.network.ApiClient;
import com.uteq.software.labrumiologia.network.ChatRequest;
import com.uteq.software.labrumiologia.network.ChatResponse;
import com.uteq.software.labrumiologia.network.ChatSource;
import com.uteq.software.labrumiologia.ui.ChatAdapter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    private String equipmentId;
    private ChatAdapter adapter;
    private TextInputEditText input;
    private MaterialButton btnSend;

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

        ApiClient.chat(new ChatRequest(question, equipmentId), new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                showReply(response.isSuccessful() && response.body() != null
                        ? response.body()
                        : null, "HTTP " + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                showReply(null, t.getMessage() != null ? t.getMessage() : "sin conexión");
            }
        });
    }

    private void showReply(ChatResponse body, String errorDetail) {
        btnSend.setEnabled(true);
        adapter.removeLastIfPlaceholder();
        if (body == null) {
            adapter.add(new ChatAdapter.Message("Asistente", getString(R.string.chat_error) + "\n" + errorDetail, null));
            return;
        }
        adapter.add(new ChatAdapter.Message("Asistente", body.answer, formatSources(body)));
    }

    private String formatSources(ChatResponse body) {
        if (body.sources == null || body.sources.isEmpty()) {
            return getString(R.string.sources_label) + " (sin coincidencias documentales)";
        }
        StringBuilder sb = new StringBuilder(getString(R.string.sources_label)).append('\n');
        for (ChatSource s : body.sources) {
            sb.append("• ").append(s.title != null ? s.title : "documento");
            if (s.page != null) sb.append(" (pág. ").append(s.page).append(')');
            sb.append('\n');
        }
        return sb.toString().trim();
    }
}
