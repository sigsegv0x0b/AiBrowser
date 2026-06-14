package com.aibrowser.agent;

import com.aibrowser.browser.TabManager;
import com.aibrowser.data.SettingsRepository;
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

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public ToolExecutor_Factory(Provider<TabManager> tabManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.tabManagerProvider = tabManagerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public ToolExecutor get() {
    return newInstance(tabManagerProvider.get(), settingsRepositoryProvider.get());
  }

  public static ToolExecutor_Factory create(javax.inject.Provider<TabManager> tabManagerProvider,
      javax.inject.Provider<SettingsRepository> settingsRepositoryProvider) {
    return new ToolExecutor_Factory(Providers.asDaggerProvider(tabManagerProvider), Providers.asDaggerProvider(settingsRepositoryProvider));
  }

  public static ToolExecutor_Factory create(Provider<TabManager> tabManagerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new ToolExecutor_Factory(tabManagerProvider, settingsRepositoryProvider);
  }

  public static ToolExecutor newInstance(TabManager tabManager,
      SettingsRepository settingsRepository) {
    return new ToolExecutor(tabManager, settingsRepository);
  }
}
