package com.substringtechnologies.jpaadmin.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Named storages built from {@code spring.jpa.admin.storage.*}, plus any custom {@link FileStorage} beans. */
public class StorageRegistry {

    private static final String S3_CLIENT = "software.amazon.awssdk.services.s3.S3Client";

    private final Map<String, FileStorage> storages = new LinkedHashMap<>();
    private final Map<String, StorageSettings> settings;

    /**
     * @param localUploadEndpoint admin API URL that receives local uploads
     * @param custom              application-provided storages, matched by {@link FileStorage#type()}
     */
    public StorageRegistry(Map<String, StorageSettings> settings, String localUploadEndpoint, Collection<FileStorage> custom) {
        this.settings = settings;
        settings.forEach((name, config) -> storages.put(name, create(name, config, localUploadEndpoint, custom)));
    }

    public FileStorage get(String name) {
        FileStorage storage = storages.get(name);
        if (storage == null) throw new UploadException(500, "Storage \"" + name + "\" is not configured");
        return storage;
    }

    public boolean contains(String name) { return storages.containsKey(name); }

    public Set<String> names() { return Collections.unmodifiableSet(storages.keySet()); }

    public List<LocalFileStorage> localStorages() {
        return storages.values().stream().filter(LocalFileStorage.class::isInstance).map(LocalFileStorage.class::cast).toList();
    }

    public StorageSettings settings(String name) { return settings.get(name); }

    private static FileStorage create(String name, StorageSettings config, String localUploadEndpoint, Collection<FileStorage> custom) {
        String type = config.getType() == null ? "local" : config.getType().trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "local" -> new LocalFileStorage(config, localUploadEndpoint);
            case "imagekit" -> new ImageKitFileStorage(name, config);
            case "s3" -> {
                if (!isPresent(S3_CLIENT)) {
                    throw new IllegalStateException("spring.jpa.admin.storage." + name + " uses type s3, which needs the dependency "
                            + "software.amazon.awssdk:s3 on the classpath");
                }
                try {
                    // Loaded reflectively so applications without the AWS SDK never touch its classes.
                    yield (FileStorage) Class.forName("com.substringtechnologies.jpaadmin.storage.S3FileStorage")
                            .getConstructor(String.class, StorageSettings.class).newInstance(name, config);
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause() instanceof RuntimeException r ? r : new IllegalStateException(e.getCause());
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Could not create S3 storage " + name, e);
                }
            }
            default -> custom.stream().filter(s -> s.type().equalsIgnoreCase(type)).findFirst().orElseThrow(() -> new IllegalStateException(
                    "spring.jpa.admin.storage." + name + ".type \"" + type + "\" is unknown; use local, s3, imagekit or declare a FileStorage bean with that type"));
        };
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className, false, StorageRegistry.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
