package com.fluence.menu;

import android.content.ComponentName;
import android.graphics.Bitmap;

/** Una app instalada que aparece en el menú. */
final class AppItem {
    final String label;
    final ComponentName component;
    final Bitmap icon;

    AppItem(String label, ComponentName component, Bitmap icon) {
        this.label = label;
        this.component = component;
        this.icon = icon;
    }

    String key() {
        return component.flattenToString();
    }

    String packageName() {
        return component.getPackageName();
    }
}
