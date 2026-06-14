package com.aibrowser.agent;

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
public final class McpController_Factory implements Factory<McpController> {
  private final Provider<ToolExecutor> toolExecutorProvider;

  public McpController_Factory(Provider<ToolExecutor> toolExecutorProvider) {
    this.toolExecutorProvider = toolExecutorProvider;
  }

  @Override
  public McpController get() {
    return newInstance(toolExecutorProvider.get());
  }

  public static McpController_Factory create(
      javax.inject.Provider<ToolExecutor> toolExecutorProvider) {
    return new McpController_Factory(Providers.asDaggerProvider(toolExecutorProvider));
  }

  public static McpController_Factory create(Provider<ToolExecutor> toolExecutorProvider) {
    return new McpController_Factory(toolExecutorProvider);
  }

  public static McpController newInstance(ToolExecutor toolExecutor) {
    return new McpController(toolExecutor);
  }
}
