package net.coreprotect.api;

import java.io.Serializable;

/**
 * Wrapper class for storing custom block data from BlockDataProviders.
 */
public class BlockDataProviderData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Marker string to identify this as provider data
     */
    public static final String MARKER = "__COREPROTECT_PROVIDER_DATA__";

    private final byte[] data;

    /**
     * Creates a new BlockDataProviderData wrapper.
     *
     * @param data The serialized provider data
     */
    public BlockDataProviderData(byte[] data) {
        this.data = data;
    }

    /**
     * Gets the serialized provider data.
     *
     * @return The provider data bytes
     */
    public byte[] getData() {
        return data;
    }
}