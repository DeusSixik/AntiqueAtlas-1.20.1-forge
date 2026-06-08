package com.stereowalker.unionlib.api.collectors;

import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReloadListeners {
    private final List<PreparableReloadListener> listeners = new ArrayList<>();

    public void listenTo(PreparableReloadListener listener) {
        listeners.add(listener);
    }

    public List<PreparableReloadListener> listeners() {
        return Collections.unmodifiableList(listeners);
    }
}
