package cz.stodva.hlaseninastupu.listeners;

import cz.stodva.hlaseninastupu.objects.Report;

public interface OnNextLastReportLoadedListener {
    void onNextLastReportLoaded(Report[] result);
}
