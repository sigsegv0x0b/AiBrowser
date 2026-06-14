package com.aibrowser.agent;

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
public final class AgentViewModel_Factory implements Factory<AgentViewModel> {
  private final Provider<AiService> aiServiceProvider;

  private final Provider<McpController> mcpControllerProvider;

  public AgentViewModel_Factory(Provider<AiService> aiServiceProvider,
      Provider<McpController> mcpControllerProvider) {
    this.aiServiceProvider = aiServiceProvider;
    this.mcpControllerProvider = mcpControllerProvider;
  }

  @Override
  public AgentViewModel get() {
    return newInstance(aiServiceProvider.get(), mcpControllerProvider.get());
  }

  public static AgentViewModel_Factory create(javax.inject.Provider<AiService> aiServiceProvider,
      javax.inject.Provider<McpController> mcpControllerProvider) {
    return new AgentViewModel_Factory(Providers.asDaggerProvider(aiServiceProvider), Providers.asDaggerProvider(mcpControllerProvider));
  }

  public static AgentViewModel_Factory create(Provider<AiService> aiServiceProvider,
      Provider<McpController> mcpControllerProvider) {
    return new AgentViewModel_Factory(aiServiceProvider, mcpControllerProvider);
  }

  public static AgentViewModel newInstance(AiService aiService, McpController mcpController) {
    return new AgentViewModel(aiService, mcpController);
  }
}
