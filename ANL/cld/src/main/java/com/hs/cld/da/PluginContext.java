package com.hs.cld.da;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;

public class PluginContext extends ContextWrapper {

    private Resources resources;
    private AssetManager assetManager;
    private Resources.Theme theme;
    private ClassLoader classLoader;
    public PluginContext(Context base, Resources resources, AssetManager assetManager, Resources.Theme theme, ClassLoader classLoader) {
        super(base);
        this.resources = resources;
        this.assetManager = assetManager;
        this.theme = theme;
        this.classLoader = classLoader;
    }

    public PluginContext(Context base) {
        super(base);
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public AssetManager getAssets() {
        return assetManager;
    }

    @Override
    public Resources.Theme getTheme() {
        return theme;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
