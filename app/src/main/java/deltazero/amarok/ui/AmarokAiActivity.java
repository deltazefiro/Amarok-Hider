package deltazero.amarok.ui;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import deltazero.amarok.AmarokActivity;
import deltazero.amarok.R;

public class AmarokAiActivity extends AmarokActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages;
    private Handler handler;
    private Random random;
    private View typingIndicator;
    private View dot1, dot2, dot3;

    // Funny canned responses for the prank
    private final String[] responses = {
            "I'm analyzing your privacy files... 🤔",
            "Hmm, interesting question! But have you tried hiding your browser history?",
            "Error 404: Intelligence not found. Just kidding! 😄",
            "Beep boop. I'm totally a real AI and not a prank. 🤖",
            "I'm consulting the ancient texts... They say: April Fools! 🎉",
            "Loading AI response... Just kidding, there's no AI here!",
            "Your question has been forwarded to our team of experts. They're all on vacation.",
            "I would love to help, but I'm just a placeholder! Have a nice day! 😊"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amarok_ai);

        // Initialize
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        messages = new ArrayList<>();

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.ai_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Binding views
        recyclerView = findViewById(R.id.ai_recycler_view);
        etMessage = findViewById(R.id.ai_et_message);
        btnSend = findViewById(R.id.ai_btn_send);
        typingIndicator = findViewById(R.id.typing_indicator_layout);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        // Setup RecyclerView
        chatAdapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        // Add initial AI greeting with delay for effect
        handler.postDelayed(() -> {
            showTypingIndicator();
            handler.postDelayed(() -> {
                hideTypingIndicator();
                addMessage("Hello! I'm the Amarok AI. How can I help you with your privacy files? 🚀", false);
            }, 2000);
        }, 500);

        // Setup send button
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String userMessage = etMessage.getText().toString().trim();
        if (userMessage.isEmpty())
            return;

        // Add user message
        addMessage(userMessage, true);
        etMessage.setText("");

        // Simulate AI "thinking" with typing indicator
        btnSend.setEnabled(false);
        showTypingIndicator();

        handler.postDelayed(() -> {
            hideTypingIndicator();
            String aiResponse = responses[random.nextInt(responses.length)];
            addMessage(aiResponse, false);
            btnSend.setEnabled(true);
        }, 1200 + random.nextInt(1800));
    }

    private void showTypingIndicator() {
        typingIndicator.setVisibility(View.VISIBLE);
        recyclerView.postDelayed(() ->
                recyclerView.smoothScrollToPosition(messages.size()), 100);

        // Animate typing dots
        animateDot(dot1, 0);
        animateDot(dot2, 200);
        animateDot(dot3, 400);
    }

    private void hideTypingIndicator() {
        typingIndicator.setVisibility(View.GONE);
    }

    private void animateDot(View dot, long delay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(dot, "translationY", 0f, -15f, 0f);
        animator.setDuration(600);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setStartDelay(delay);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        // Store animator to cancel later
        dot.setTag(animator);
    }

    private void addMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        recyclerView.smoothScrollToPosition(messages.size() - 1);
    }

    // ChatMessage model
    static class ChatMessage {
        String text;
        boolean isUser;

        ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    // RecyclerView Adapter
    class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatMessage> messages;

        ChatAdapter(List<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            LinearLayout userMessageLayout;
            LinearLayout aiMessageLayout;
            TextView tvUserMessage;
            TextView tvAiMessage;

            ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                userMessageLayout = itemView.findViewById(R.id.user_message_layout);
                aiMessageLayout = itemView.findViewById(R.id.ai_message_layout);
                tvUserMessage = itemView.findViewById(R.id.tv_user_message);
                tvAiMessage = itemView.findViewById(R.id.tv_ai_message);
            }

            void bind(ChatMessage message) {
                if (message.isUser) {
                    userMessageLayout.setVisibility(View.VISIBLE);
                    aiMessageLayout.setVisibility(View.GONE);
                    tvUserMessage.setText(message.text);

                    // Animate user message from right
                    itemView.setTranslationX(100f);
                    itemView.setAlpha(0f);
                    itemView.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                } else {
                    userMessageLayout.setVisibility(View.GONE);
                    aiMessageLayout.setVisibility(View.VISIBLE);
                    tvAiMessage.setText(message.text);

                    // Animate AI message from left
                    itemView.setTranslationX(-100f);
                    itemView.setAlpha(0f);
                    itemView.animate()
                            .translationX(0f)
                            .alpha(1f)
                            .setDuration(300)
                            .setInterpolator(new AccelerateDecelerateInterpolator())
                            .start();
                }
            }
        }
    }
}

