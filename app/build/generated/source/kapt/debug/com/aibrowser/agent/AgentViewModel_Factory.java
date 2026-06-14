package com.aibrowser.agent;

import android.content.Context;
import com.aibrowser.browser.TabManager;
import com.aibrowser.data.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class AgentViewModel_Factory implements Factory<AgentViewModel> {
  private final Provider<AiService> aiServiceProvider;

  private final Provider<McpController> mcpControllerProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<TabManager> tabManagerProvider;

  private final Provider<Context> contextProvider;

  public AgentViewModel_Factory(Provider<AiService> aiServiceProvider,
      Provider<McpController> mcpControllerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<TabManager> tabManagerProvider, Provider<Context> contextProvider) {
    this.aiServiceProvider = aiServiceProvider;
    this.mcpControllerProvider = mcpControllerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.tabManagerProvider = tabManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AgentViewModel get() {
    return newInstance(aiServiceProvider.get(), mcpControllerProvider.get(), settingsRepositoryProvider.get(), tabManagerProvider.get(), contextProvider.get());
  }

  public static AgentViewModel_Factory create(javax.inject.Provider<AiService> aiServiceProvider,
      javax.inject.Provider<McpController> mcpControllerProvider,
      javax.inject.Provider<SettingsRepository> settingsRepositoryProvider,
      javax.inject.Provider<TabManager> tabManagerProvider,
      javax.inject.Provider<Context> contextProvider) {
    return new AgentViewModel_Factory(Providers.asDaggerProvider(aiServiceProvider), Providers.asDaggerProvider(mcpControllerProvider), Providers.asDaggerProvider(settingsRepositoryProvider), Providers.asDaggerProvider(tabManagerProvider), Providers.asDaggerProvider(contextProvider));
  }

  public static AgentViewModel_Factory create(Provider<AiService> aiServiceProvider,
      Provider<McpController> mcpControllerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<TabManager> tabManagerProvider, Provider<Context> contextProvider) {
    return new AgentViewModel_Factory(aiServiceProvider, mcpControllerProvider, settingsRepositoryProvider, tabManagerProvider, contextProvider);
  }

  public static AgentViewModel newInstance(AiService aiService, McpController mcpController,
      SettingsRepository settingsRepository, TabManager tabManager, Context context) {
    return new AgentViewModel(aiService, mcpController, settingsRepository, tabManager, context);
  }
}
