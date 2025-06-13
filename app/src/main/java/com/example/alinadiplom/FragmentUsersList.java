package com.example.alinadiplom;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alinadiplom.adapter.UserAdapter;
import com.example.alinadiplom.model.UserItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Фрагмент «Список всех пользователей», с поиском по ФИО и назначением админства.
 */
public class FragmentUsersList extends Fragment {

    private RecyclerView recyclerUsers;
    private ProgressBar progressBar;
    private EditText editSearch;
    private UserAdapter adapter;
    private DatabaseReference usersRef;
    private ValueEventListener usersListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerUsers = view.findViewById(R.id.recycler_users);
        progressBar = view.findViewById(R.id.progressBar_users);
        editSearch = view.findViewById(R.id.editText_search);

        // Настраиваем RecyclerView
        recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserAdapter();
        recyclerUsers.setAdapter(adapter);

        // Ссылка на ветку "Users"
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        loadAllUsers();

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // не нужно
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterByFio(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // не нужно
            }
        });
    }

    private void loadAllUsers() {
        progressBar.setVisibility(View.VISIBLE);

        // Если предыдущий слушатель ещё не был удалён, удалим его
        if (usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }

        usersListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserItem> list = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserItem user = ds.getValue(UserItem.class);
                    if (user != null) {
                        // Ключ узла = UID
                        user.uid = ds.getKey();
                        list.add(user);
                    }
                }

                // Передаём весь список в адаптер
                adapter.setUsers(list);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка загрузки пользователей: "
                            + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Отписываемся от слушателя
        if (usersListener != null) {
            usersRef.removeEventListener(usersListener);
            usersListener = null;
        }
    }
}
