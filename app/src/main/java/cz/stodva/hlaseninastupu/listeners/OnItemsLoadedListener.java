package cz.stodva.hlaseninastupu.listeners;

import java.util.ArrayList;

import cz.stodva.hlaseninastupu.objects.Report;

public interface OnItemsLoadedListener {
    void onItemsLoaded(ArrayList<Report> loadedItems);
}
