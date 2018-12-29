package peergos.shared.user.fs.cryptree;

import peergos.shared.*;
import peergos.shared.cbor.*;
import peergos.shared.crypto.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.crypto.random.*;
import peergos.shared.crypto.symmetric.*;
import peergos.shared.io.ipfs.multihash.*;
import peergos.shared.user.fs.*;
import peergos.shared.util.*;

import java.util.*;
import java.util.concurrent.*;

public interface CryptreeNode extends Cborable {

    int CURRENT_FILE_VERSION = 1;
    int CURRENT_DIR_VERSION = 1;

    MaybeMultihash committedHash();

    boolean isDirectory();

    int getVersion();

    default int getVersionAndType() {
        return getVersion() << 1 | (isDirectory() ? 0 : 1);
    }

    SymmetricKey getParentKey(SymmetricKey baseKey);

    SymmetricKey getMetaKey(SymmetricKey baseKey);

    EncryptedCapability getParentLink();

    FileProperties getProperties(SymmetricKey parentKey);

    CompletableFuture<? extends CryptreeNode> updateProperties(PublicKeyHash owner,
                                                               PublicKeyHash writer,
                                                               Capability writableCapability,
                                                               FileProperties newProps,
                                                               NetworkAccess network);

    boolean isDirty(SymmetricKey baseKey);

    CompletableFuture<? extends CryptreeNode> copyTo(PublicKeyHash currentOwner,
                                                     PublicKeyHash currentWriter,
                                                     SymmetricKey baseKey,
                                                     SymmetricKey newBaseKey,
                                                     Location newParentLocation,
                                                     SymmetricKey parentparentKey,
                                                     SigningPrivateKeyAndPublicHash entryWriterKey,
                                                     byte[] newMapKey,
                                                     NetworkAccess network,
                                                     SafeRandom random);

    default CompletableFuture<RetrievedCapability> getParent(PublicKeyHash owner,
                                                             PublicKeyHash writer,
                                                             SymmetricKey baseKey,
                                                             NetworkAccess network) {
        EncryptedCapability parentLink = getParentLink();
        if (parentLink == null)
            return CompletableFuture.completedFuture(null);

        return network.retrieveAllMetadata(Arrays.asList(new Triple<>(owner, writer, parentLink.toCapability(baseKey)))).thenApply(res -> {
            RetrievedCapability retrievedCapability = res.stream().findAny().get();
            return retrievedCapability;
        });
    }

    static CryptreeNode fromCbor(CborObject cbor, Multihash hash) {
        if (! (cbor instanceof CborObject.CborList))
            throw new IllegalStateException("Incorrect cbor for FileAccess: " + cbor);

        List<? extends Cborable> value = ((CborObject.CborList) cbor).value;
        int versionAndType = (int) ((CborObject.CborLong) value.get(0)).value;
        boolean isFile = (versionAndType & 1) != 0;
        if (isFile)
            return FileAccess.fromCbor(cbor, hash);
        return DirAccess.fromCbor(cbor, hash);
    }
}
