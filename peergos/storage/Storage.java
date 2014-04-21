package peergos.storage;

import peergos.util.ByteArrayWrapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Storage
{
    private final File root;
    private final long maxBytes;
    private final AtomicLong totalSize = new AtomicLong(0);
    private final AtomicLong promisedSize = new AtomicLong(0);
    private final Map<ByteArrayWrapper, Integer> pending = new ConcurrentHashMap();
    private final Map<ByteArrayWrapper, Integer> existing = new ConcurrentHashMap();

    public Storage(File root, long maxBytes) throws IOException
    {
        this.root = root;
        this.maxBytes = maxBytes;
        if (root.exists())
        {
            for (File f: root.listFiles())
            {
                if (f.isDirectory())
                    continue;
                ByteArrayWrapper name = new ByteArrayWrapper(peergos.util.Arrays.hexToBytes(f.getName()));
                Fragment frag = new Fragment(name);
                int size = frag.getSize();
                existing.put(name, size);
            }
        }
        else
            root.mkdirs();
    }

    public File getRoot()
    {
        return root;
    }

    public boolean isWaitingFor(byte[] key)
    {
        return pending.containsKey(new ByteArrayWrapper(key));
    }

    public boolean accept(ByteArrayWrapper key, int size)
    {
        if (existing.containsKey(key))
            return false; // don't overwrite old data for now (not sure this would ever be a problem with a cryptographic hash..
        boolean res = totalSize.get() + promisedSize.get() + size < maxBytes;
        if (res)
            promisedSize.getAndAdd(size);
        pending.put(key, size);
        return res;
    }

    public boolean put(ByteArrayWrapper key, byte[] value)
    {
        if (value.length != pending.get(key))
            return false;
        pending.remove(key);
        existing.put(key, value.length);
        // commit data
        try
        {
            new Fragment(key).write(value);
        } catch (IOException e)
        {
            e.printStackTrace();
            existing.remove(key);
            return false;
        }
        return true;
    }

    public byte[] get(ByteArrayWrapper key)
    {
        try {
            return new Fragment(key).read();
        } catch (IOException e)
        {
            e.printStackTrace();
            existing.remove(key);
            return null;
        }
    }

    public boolean contains(ByteArrayWrapper key)
    {
        return existing.containsKey(key);
    }

    public int sizeOf(ByteArrayWrapper key)
    {
        if (!existing.containsKey(key))
            return 0;
        return existing.get(key);
    }

    public class Fragment
    {
        String name;

        public Fragment(ByteArrayWrapper key)
        {
            name = peergos.util.Arrays.bytesToHex(key.data);
        }

        public int getSize()
        {
            return (int)new File(root, name).length(); // all fragments are WELL under 4GiB!
        }

        public void write(byte[] data) throws IOException
        {
            OutputStream out = new FileOutputStream(new File(root, name));
            out.write(data);
            out.flush();
            out.close();
        }

        public byte[] read() throws IOException{
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            InputStream in = new FileInputStream(new File(root, name));
            byte[] buf = new byte[4096];
            int read;
            while ((read = in.read(buf, 0, buf.length)) > 0)
                bout.write(buf, 0, read);
            return bout.toByteArray();
        }
    }
}