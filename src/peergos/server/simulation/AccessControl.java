package peergos.server.simulation;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface AccessControl {

    String getOwner(Path path);

    List<String> get(Path path, FileSystem.Permission permission);

    void add(Path path, String reader, FileSystem.Permission permission);

    void remove(Path path, String reader, FileSystem.Permission permission);


    /**
     * Model owners, reader and writers
     */
    class AccessControlUnit {
        private Map<Path, List<String>> allowed = new HashMap<>();

        List<String> getAllowed(Path path) {
            return allowed.computeIfAbsent(path, p -> new ArrayList<>());
        }

        void addAllowed(Path path, String user) {
            getAllowed(path).add(user);
        }

        void removeAllowed(Path path, String user) {
            getAllowed(path).remove(user);
        }
    }

    default boolean can(Path path, String user, FileSystem.Permission permission) {

        return getOwner(path).equals(user) || get(path, permission).contains(user);
    }

    class MemoryImpl implements AccessControl {
        public AccessControlUnit readers = new AccessControlUnit();
        public AccessControlUnit writers = new AccessControlUnit();

        @Override
        public String getOwner(Path path) {
            /**
             * Should be "/owner/..."
             */
            try {
                return path.getName(0).toString();
            } catch (Throwable t) {
                throw t;
            }
        }

        @Override
        public List<String> get(Path path, FileSystem.Permission permission) {
            switch (permission) {
                case WRITE:
                    return writers.getAllowed(path);
                case READ:
                    List<String> allowed = readers.getAllowed(path);
                    List<String> allowed2 = writers.getAllowed(path);
                    return new ArrayList<>(Stream.of(allowed, allowed2).flatMap(e -> e.stream()).collect(Collectors.toSet()));
                default:
                    throw new IllegalStateException("Unimplemented");
            }
        }

        @Override
        public void add(Path path, String reader, FileSystem.Permission permission) {
            switch (permission) {
                case READ:
                    readers.addAllowed(path, reader);
                    return;
                case WRITE:
                    writers.addAllowed(path, reader);
                    return;
                default:
                    throw new IllegalStateException("Unimplemented");
            }
        }

        @Override
        public void remove(Path path, String reader, FileSystem.Permission permission) {
            switch (permission) {
                case READ:
                    readers.removeAllowed(path, reader);
                    return;
                case WRITE:
                    writers.removeAllowed(path, reader);
                    return;
                default:
                    throw new IllegalStateException("Unimplemented");
            }
        }
    }
}
