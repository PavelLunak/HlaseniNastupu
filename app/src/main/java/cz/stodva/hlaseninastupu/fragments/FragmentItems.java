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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.adapters.AdapterItems;
import cz.stodva.hlaseninastupu.listeners.OnNewPageLoadedListener;
import cz.stodva.hlaseninastupu.utils.Animators;


public class FragmentItems extends Fragment {

    RecyclerView recyclerView;
    TextView labelPagesCount, labelTotalItemsCount, labelNoItems;
    ImageView imgArrowLeft, imgArrowRight;
    CheckBox chbFilter;

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
        chbFilter = view.findViewById(R.id.chbFilter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgArrowLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(imgArrowLeft, true);
                activity.pageDown(new OnNewPageLoadedListener() {
                    @Override
                    public void onNewPageLoaded() {
                        recyclerView.scrollToPosition(0);
                    }
                });

                updatePageInfo();
            }
        });

        imgArrowRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(imgArrowRight, true);
                activity.pageUp(new OnNewPageLoadedListener() {
                    @Override
                    public void onNewPageLoaded() {
                        recyclerView.scrollToPosition(0);
                    }
                });

                updatePageInfo();
            }
        });

        chbFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                activity.setPage(1);
                activity.setShowOnlyWaitingReports(isChecked);
                activity.updateItems(null);
            }
        });

        updateAdapter();
        chbFilter.setChecked(activity.isShowOnlyWaitingReports());
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

    public void updatePageInfo() {
        labelTotalItemsCount.setText((activity.isShowOnlyWaitingReports() ? "Počet aktivních hlášení: " : "Počet uložených hlášení: ") + activity.getItemsCount());
        labelPagesCount.setText("Stránka: " + activity.getPage() + "/" + activity.getPagesCount());

        if (activity.getPage() <= 1) imgArrowLeft.setVisibility(View.GONE);
        else imgArrowLeft.setVisibility(View.VISIBLE);

        if (activity.getPage() < activity.getPagesCount()) imgArrowRight.setVisibility(View.VISIBLE);
        else imgArrowRight.setVisibility(View.GONE);
    }

    public void updateFragment() {
        updatePageInfo();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    public AdapterItems getAdapter() {
        return adapter;
    }
}