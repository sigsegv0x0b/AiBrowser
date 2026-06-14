package com.aibrowser.browser;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class TabManager_Factory implements Factory<TabManager> {
  private final Provider<Context> contextProvider;

  public TabManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TabManager get() {
    return newInstance(contextProvider.get());
  }

  public static TabManager_Factory create(javax.inject.Provider<Context> contextProvider) {
    return new TabManager_Factory(Providers.asDaggerProvider(contextProvider));
  }

  public static TabManager_Factory create(Provider<Context> contextProvider) {
    return new TabManager_Factory(contextProvider);
  }

  public static TabManager newInstance(Context context) {
    return new TabManager(context);
  }
}
