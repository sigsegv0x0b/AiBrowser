package com.aibrowser;

import com.aibrowser.agent.AiService;
import com.aibrowser.data.SettingsRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AiService> aiServiceProvider;

  public MainActivity_MembersInjector(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AiService> aiServiceProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.aiServiceProvider = aiServiceProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AiService> aiServiceProvider) {
    return new MainActivity_MembersInjector(settingsRepositoryProvider, aiServiceProvider);
  }

  public static MembersInjector<MainActivity> create(
      javax.inject.Provider<SettingsRepository> settingsRepositoryProvider,
      javax.inject.Provider<AiService> aiServiceProvider) {
    return new MainActivity_MembersInjector(Providers.asDaggerProvider(settingsRepositoryProvider), Providers.asDaggerProvider(aiServiceProvider));
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
    injectAiService(instance, aiServiceProvider.get());
  }

  @InjectedFieldSignature("com.aibrowser.MainActivity.settingsRepository")
  public static void injectSettingsRepository(MainActivity instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }

  @InjectedFieldSignature("com.aibrowser.MainActivity.aiService")
  public static void injectAiService(MainActivity instance, AiService aiService) {
    instance.aiService = aiService;
  }
}
