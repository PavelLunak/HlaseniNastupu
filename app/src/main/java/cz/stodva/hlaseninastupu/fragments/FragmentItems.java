package cz.stodva.hlaseninastupu.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.adapters.AdapterItems;


public class FragmentItems extends Fragment {

    RecyclerView recyclerView;
    TextView labelPagesCount, labelTotalItemsCount, labelNoItems;
    ImageView imgArrowLeft, imgArrowRight;

    MainActivity activity;
    AdapterItems adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_items, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        labelPagesCount = view.findViewById(R.id.labelPagesCount);
        labelTotalItemsCount = view.findViewById(R.id.labelTotalItemsCount);
        labelNoItems = view.findViewById(R.id.labelNoItems);
        imgArrowLeft = view.findViewById(R.id.imgArrowLeft);
        imgArrowRight = view.findViewById(R.id.imgArrowRight);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void updateAdapter() {
        if (recyclerView == null) return;

        if (activity.getItems() == null) {
            labelNoItems.setVisibility(View.VISIBLE);
        } else if (activity.getItems().isEmpty()) {
            labelNoItems.setVisibility(View.VISIBLE);
        } else {
            labelNoItems.setVisibility(View.GONE);
        }

        adapter = new AdapterItems(activity);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
    }
}