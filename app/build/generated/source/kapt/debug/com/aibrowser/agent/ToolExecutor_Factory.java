package com.aibrowser.agent;

import com.aibrowser.browser.TabManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ToolExecutor_Factory implements Factory<ToolExecutor> {
  private final Provider<TabManager> tabManagerProvider;

  public ToolExecutor_Factory(Provider<TabManager> tabManagerProvider) {
    this.tabManagerProvider = tabManagerProvider;
  }

  @Override
  public ToolExecutor get() {
    return newInstance(tabManagerProvider.get());
  }

  public static ToolExecutor_Factory create(javax.inject.Provider<TabManager> tabManagerProvider) {
    return new ToolExecutor_Factory(Providers.asDaggerProvider(tabManagerProvider));
  }

  public static ToolExecutor_Factory create(Provider<TabManager> tabManagerProvider) {
    return new ToolExecutor_Factory(tabManagerProvider);
  }

  public static ToolExecutor newInstance(TabManager tabManager) {
    return new ToolExecutor(tabManager);
  }
}
