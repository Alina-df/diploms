package com.example.alinadiplom.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.alinadiplom.ChatActivity;
import com.example.alinadiplom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PersonProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_person_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvName = view.findViewById(R.id.textView_profile_name);
        TextView tvRoom = view.findViewById(R.id.textView_profile_room);
        TextView tvAbout = view.findViewById(R.id.textView_profile_about);
        TextView tvTags = view.findViewById(R.id.textView_profile_tags);
        ImageView ivAvatar = view.findViewById(R.id.imageView_profile_avatar);
        ImageButton btnTelegram = view.findViewById(R.id.button_profile_telegram);
        ImageButton btnChat = view.findViewById(R.id.button_profile_chat);
        ImageButton btnBack = view.findViewById(R.id.button_back);
        ImageButton btnDelete = view.findViewById(R.id.button_delete);


        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        Bundle args = getArguments();
        if (args != null) {
            String name = args.getString("name", "");
            String room = args.getString("room", "");
            String about = args.getString("about", "");
            String tags = args.getString("tags", "");
            String avatarUrl = args.getString("avatarUrl", "");
            String telegramLink = args.getString("telegramLink", "");
            String authorId = args.getString("personId", "");
            String adId = args.getString("adId", ""); // Теперь тут должен быть непустой ключ

            // Для отладки:
            Log.d("DEBUG_AD_ID", "Получен adId: " + adId);

            tvName.setText(name);
            tvRoom.setText(room);
            tvAbout.setText(about);
            tvTags.setText(tags);

            if (!avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.circle_progress)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.circle_progress);
            }

            btnTelegram.setOnClickListener(v -> {
                if (!telegramLink.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(telegramLink)));
                } else {
                    Toast.makeText(requireContext(), "Ссылка на Telegram не указана", Toast.LENGTH_SHORT).show();
                }
            });

            btnChat.setOnClickListener(v -> {
                String currentUserId = FirebaseAuth.getInstance().getUid();
                if (currentUserId == null) {
                    Toast.makeText(requireContext(), "Требуется авторизация", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentUserId.equals(authorId)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("adId", adId);
                    Navigation.findNavController(v).navigate(R.id.action_to_chat_list, bundle);
                } else {
                    openChat(adId, authorId, currentUserId);
                }
            });

            String currentUserId = FirebaseAuth.getInstance().getUid();
            if (currentUserId != null && currentUserId.equals(authorId)) {
                btnDelete.setVisibility(View.VISIBLE);
            }

            btnDelete.setOnClickListener(v -> {
                if (!adId.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Удалить объявление")
                            .setMessage("Вы уверены, что хотите удалить свое объявление?")
                            .setPositiveButton("Удалить", (dialog, which) -> deleteAd(adId))
                            .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    Toast.makeText(requireContext(), "ID объявления не найден", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void deleteAd(String adId) {
        DatabaseReference noticeRef = FirebaseDatabase.getInstance()
                .getReference("notices")
                .child(adId);

        noticeRef.removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Объявление удалено", Toast.LENGTH_SHORT).show();
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Ошибка при удалении: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openChat(String adId, String authorId, String currentUserId) {
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference()
                .child("messages")
                .child(adId)
                .child(currentUserId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Intent intent = new Intent(requireActivity(), ChatActivity.class);
                intent.putExtra("adId", adId);
                intent.putExtra("authorId", authorId);
                intent.putExtra("currentUserId", currentUserId);

                if (!snapshot.exists()) {
                    Map<String, Object> initData = new HashMap<>();
                    initData.put("creatorId", authorId);
                    chatRef.getParent().updateChildren(initData);
                }

                startActivity(intent);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
