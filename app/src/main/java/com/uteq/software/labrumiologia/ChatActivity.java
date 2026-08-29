package com.uteq.software.labrumiologia;

import android.os.Bundle;
import android.widget.Toast;

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
    private String equipmentLabel;
    private ChatAdapter adapter;
    private TextInputEditText input;
    private MaterialButton btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        equipmentId = getIntent().getStringExtra(DetectionActivity.EXTRA_EQUIPMENT_ID);
        equipmentLabel = getIntent().getStringExtra(DetectionActivity.EXTRA_EQUIPMENT_LABEL);

        findViewById(R.id.chatEquipmentLabel);
        ((android.widget.TextView) findViewById(R.id.chatEquipmentLabel))
                .setText("Asistente · " + (equipmentLabel != null ? equipmentLabel : equipmentId));

        RecyclerView messages = findViewById(R.id.chatMessages);
        messages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter();
        messages.setAdapter(adapter);

        input = findViewById(R.id.chatInput);
        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> sendMessage());

        adapter.add(new ChatAdapter.Message(
                "Sistema",
                "Pregunte sobre función, operación, seguridad, mantenimiento o uso académico. Las respuestas se basan solo en documentos del laboratorio.",
                null
        ));
    }

    private void sendMessage() {
        String question = input.getText() != null ? input.getText().toString().trim() : "";
        if (question.isEmpty()) return;

        adapter.add(new ChatAdapter.Message("Usted", question, null));
        input.setText("");
        btnSend.setEnabled(false);

        ApiClient.get().chat(new ChatRequest(question, equipmentId)).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(@NonNull Call<ChatResponse> call, @NonNull Response<ChatResponse> response) {
                btnSend.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    adapter.add(new ChatAdapter.Message("Asistente", getString(R.string.chat_error), null));
                    return;
                }
                ChatResponse body = response.body();
                String sources = formatSources(body);
                adapter.add(new ChatAdapter.Message("Asistente", body.answer, sources));
            }

            @Override
            public void onFailure(@NonNull Call<ChatResponse> call, @NonNull Throwable t) {
                btnSend.setEnabled(true);
                adapter.add(new ChatAdapter.Message("Asistente", getString(R.string.chat_error), null));
                Toast.makeText(ChatActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatSources(ChatResponse body) {
        if (body.sources == null || body.sources.isEmpty()) {
            return getString(R.string.sources_label) + " (sin coincidencias documentales)";
        }
        StringBuilder sb = new StringBuilder(getString(R.string.sources_label));
        sb.append('\n');
        for (ChatSource s : body.sources) {
            sb.append("• ").append(s.title != null ? s.title : "documento");
            if (s.page != null) sb.append(" (pág. ").append(s.page).append(')');
            sb.append('\n');
        }
        return sb.toString().trim();
    }
}
