package com.aibrowser.browser;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class BrowserViewModel_Factory implements Factory<BrowserViewModel> {
  private final Provider<TabManager> tabManagerProvider;

  public BrowserViewModel_Factory(Provider<TabManager> tabManagerProvider) {
    this.tabManagerProvider = tabManagerProvider;
  }

  @Override
  public BrowserViewModel get() {
    return newInstance(tabManagerProvider.get());
  }

  public static BrowserViewModel_Factory create(
      javax.inject.Provider<TabManager> tabManagerProvider) {
    return new BrowserViewModel_Factory(Providers.asDaggerProvider(tabManagerProvider));
  }

  public static BrowserViewModel_Factory create(Provider<TabManager> tabManagerProvider) {
    return new BrowserViewModel_Factory(tabManagerProvider);
  }

  public static BrowserViewModel newInstance(TabManager tabManager) {
    return new BrowserViewModel(tabManager);
  }
}
