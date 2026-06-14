package com.aibrowser.agent;

import com.aibrowser.data.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

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
public final class AiService_Factory implements Factory<AiService> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public AiService_Factory(Provider<OkHttpClient> clientProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.clientProvider = clientProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AiService get() {
    return newInstance(clientProvider.get(), settingsRepositoryProvider.get());
  }

  public static AiService_Factory create(javax.inject.Provider<OkHttpClient> clientProvider,
      javax.inject.Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AiService_Factory(Providers.asDaggerProvider(clientProvider), Providers.asDaggerProvider(settingsRepositoryProvider));
  }

  public static AiService_Factory create(Provider<OkHttpClient> clientProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AiService_Factory(clientProvider, settingsRepositoryProvider);
  }

  public static AiService newInstance(OkHttpClient client, SettingsRepository settingsRepository) {
    return new AiService(client, settingsRepository);
  }
}
