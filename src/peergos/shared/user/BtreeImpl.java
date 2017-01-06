package peergos.shared.user;

import peergos.shared.cbor.*;
import peergos.shared.corenode.CoreNode;
import peergos.shared.crypto.*;
import peergos.shared.ipfs.api.Multihash;
import peergos.shared.merklebtree.MaybeMultihash;
import peergos.shared.merklebtree.MerkleBTree;
import peergos.shared.merklebtree.PairMultihash;
import peergos.shared.storage.ContentAddressedStorage;
import peergos.shared.util.*;

import java.util.concurrent.*;

public class BtreeImpl implements Btree {
    private final CoreNode coreNode;
    private final ContentAddressedStorage dht;
    private static final boolean LOGGING = false;

    public BtreeImpl(CoreNode coreNode, ContentAddressedStorage dht) {
        this.coreNode = coreNode;
        this.dht = dht;
    }

    private <T> T log(T result, String toPrint) {
        if (LOGGING)
            System.out.println(toPrint);
        return result;
    }

    private CompletableFuture<WriterData> getWriterData(MaybeMultihash hash) {
        if (!hash.isPresent())
            return CompletableFuture.completedFuture(WriterData.createEmpty());
        return dht.get(hash.get())
                .thenApply(dataOpt -> {
                    if (! dataOpt.isPresent())
                        throw new IllegalStateException("Couldn't retrieve WriterData from dht! " + hash);
                    return WriterData.fromCbor(dataOpt.get(), null);
                });
    }

    @Override
    public CompletableFuture<Boolean> put(User sharingKey, byte[] mapKey, Multihash value) {
        UserPublicKey publicSharingKey = sharingKey.toUserPublicKey();
        return coreNode.getMetadataBlob(publicSharingKey)
                .thenCompose(holderHashOpt -> getWriterData(holderHashOpt)
                .thenCompose(holder -> {
                    MaybeMultihash btreeRootHash = holder.btree.isPresent() ? MaybeMultihash.of(holder.btree.get()) : MaybeMultihash.EMPTY();
                    return MerkleBTree.create(publicSharingKey, btreeRootHash, dht)
                            .thenCompose(btree -> btree.put(publicSharingKey, mapKey, value))
                            .thenApply(newRoot -> log(newRoot, "BTREE.put (" + ArrayOps.bytesToHex(mapKey) + ", " + value + ") => " + newRoot))
                            .thenCompose(newBtreeRoot -> holder.withBtree(newBtreeRoot).commit(sharingKey, coreNode, dht));
                })
        );
    }

    @Override
    public CompletableFuture<MaybeMultihash> get(UserPublicKey sharingKey, byte[] mapKey) {
        UserPublicKey publicSharingKey = sharingKey.toUserPublicKey();
        return coreNode.getMetadataBlob(publicSharingKey)
                .thenCompose(holderHashOpt -> getWriterData(holderHashOpt))
                .thenCompose(holder -> {
                    MaybeMultihash btreeRootHash = holder.btree.isPresent() ? MaybeMultihash.of(holder.btree.get()) : MaybeMultihash.EMPTY();
                    return MerkleBTree.create(publicSharingKey, btreeRootHash, dht)
                            .thenCompose(btree -> btree.get(mapKey))
                            .thenApply(maybe -> log(maybe, "BTREE.get (" + ArrayOps.bytesToHex(mapKey) + ", root="+btreeRootHash+" => " + maybe));
                });
    }

    @Override
    public CompletableFuture<Boolean> remove(User sharingKey, byte[] mapKey) {
        UserPublicKey publicSharingKey = sharingKey.toUserPublicKey();
        return coreNode.getMetadataBlob(publicSharingKey)
                .thenCompose(holderHashOpt -> getWriterData(holderHashOpt))
                .thenCompose(holder -> {
                    MaybeMultihash btreeRootHash = holder.btree.isPresent() ? MaybeMultihash.of(holder.btree.get()) : MaybeMultihash.EMPTY();
                    return MerkleBTree.create(publicSharingKey, btreeRootHash, dht)
                            .thenCompose(btree -> btree.delete(publicSharingKey, mapKey))
                            .thenApply(pair -> log(pair, "BTREE.rm (" + ArrayOps.bytesToHex(mapKey) + "  => " + pair))
                            .thenCompose(newBtreeRoot -> holder.withBtree(newBtreeRoot).commit(sharingKey, coreNode, dht));
                });
    }
}
