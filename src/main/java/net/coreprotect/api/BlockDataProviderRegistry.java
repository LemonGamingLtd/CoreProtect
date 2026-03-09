package net.coreprotect.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import net.coreprotect.utility.Chat;

/**
 * Registry for managing BlockDataProvider instances.
 */
public class BlockDataProviderRegistry {

    private static final Map<String, BlockDataProvider> providers = new ConcurrentHashMap<>();
    private static final Map<Material, List<BlockDataProvider>> materialProviders = new ConcurrentHashMap<>();

    private BlockDataProviderRegistry() {
        throw new IllegalStateException("Registry class");
    }

    /**
     * Registers a BlockDataProvider.
     *
     * @param provider The provider to register
     * @return true if registration was successful, false if a provider with the same ID already exists
     */
    public static boolean register(BlockDataProvider provider) {
        if (provider == null || provider.getProviderId() == null || provider.getProviderId().isEmpty()) {
            return false;
        }

        String providerId = provider.getProviderId().toLowerCase();
        if (providers.containsKey(providerId)) {
            Chat.console("BlockDataProvider with ID '" + providerId + "' is already registered.");
            return false;
        }

        providers.put(providerId, provider);

        if (provider.getHandledMaterials() != null) {
            for (Material material : provider.getHandledMaterials()) {
                materialProviders.computeIfAbsent(material, k -> new ArrayList<>()).add(provider);
            }
        }

        Chat.console("Registered BlockDataProvider: " + providerId);
        return true;
    }

    /**
     * Unregisters a BlockDataProvider by its ID.
     *
     * @param providerId The provider ID to unregister
     * @return true if the provider was unregistered, false if it wasn't registered
     */
    public static boolean unregister(String providerId) {
        if (providerId == null || providerId.isEmpty()) {
            return false;
        }

        providerId = providerId.toLowerCase();
        BlockDataProvider removed = providers.remove(providerId);

        if (removed != null) {
            if (removed.getHandledMaterials() != null) {
                for (Material material : removed.getHandledMaterials()) {
                    List<BlockDataProvider> list = materialProviders.get(material);
                    if (list != null) {
                        list.remove(removed);
                        if (list.isEmpty()) {
                            materialProviders.remove(material);
                        }
                    }
                }
            }
            Chat.console("Unregistered BlockDataProvider: " + providerId);
            return true;
        }

        return false;
    }

    /**
     * Checks if a provider is registered.
     *
     * @param providerId The provider ID to check
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(String providerId) {
        if (providerId == null) {
            return false;
        }
        return providers.containsKey(providerId.toLowerCase());
    }

    /**
     * Gets all providers that handle a specific material.
     *
     * @param material The material to check
     * @return List of providers handling this material, or empty list if none
     */
    public static List<BlockDataProvider> getProvidersForMaterial(Material material) {
        return materialProviders.getOrDefault(material, new ArrayList<>());
    }

    /**
     * Checks if any providers are registered for a material.
     *
     * @param material The material to check
     * @return true if any providers handle this material
     */
    public static boolean hasProvidersForMaterial(Material material) {
        List<BlockDataProvider> list = materialProviders.get(material);
        return list != null && !list.isEmpty();
    }

    /**
     * Serializes custom data from all applicable providers for a block.
     *
     * @param blockState The block state to serialize
     * @return Combined serialized data from all providers, or null if no data
     */
    public static byte[] serializeCustomData(BlockState blockState) {
        if (blockState == null) {
            return null;
        }

        Material material = blockState.getType();
        List<BlockDataProvider> applicableProviders = getProvidersForMaterial(material);

        if (applicableProviders.isEmpty()) {
            return null;
        }

        Map<String, byte[]> providerData = new HashMap<>();

        for (BlockDataProvider provider : applicableProviders) {
            try {
                byte[] data = provider.serialize(blockState);
                if (data != null && data.length > 0) {
                    providerData.put(provider.getProviderId(), data);
                }
            }
            catch (Exception e) {
                Chat.console("Error serializing data for provider '" + provider.getProviderId() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (providerData.isEmpty()) {
            return null;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(providerData.size());

            for (Map.Entry<String, byte[]> entry : providerData.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeInt(entry.getValue().length);
                dos.write(entry.getValue());
            }

            dos.flush();
            return baos.toByteArray();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Restores custom data to a block from all applicable providers.
     *
     * @param block      The block to restore data to
     * @param customData The combined serialized data from all providers
     */
    public static void restoreCustomData(Block block, byte[] customData) {
        if (block == null || customData == null || customData.length == 0) {
            return;
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(customData);
            DataInputStream dis = new DataInputStream(bais)) {

            int providerCount = dis.readInt();

            for (int i = 0; i < providerCount; i++) {
                String providerId = dis.readUTF();
                int dataLength = dis.readInt();
                byte[] data = new byte[dataLength];
                dis.readFully(data);

                BlockDataProvider provider = providers.get(providerId.toLowerCase());
                if (provider != null) {
                    try {
                        provider.restore(block, data);
                    }
                    catch (Exception e) {
                        Chat.console("Error restoring data for provider '" + providerId + "': " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Clears all registered providers.
     */
    public static void clear() {
        providers.clear();
        materialProviders.clear();
    }
}