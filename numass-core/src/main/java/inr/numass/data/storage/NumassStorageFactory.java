package inr.numass.data.storage;

import com.github.robtimus.filesystems.sftp.SFTPEnvironment;
import hep.dataforge.context.Context;
import hep.dataforge.context.Global;
import hep.dataforge.meta.Meta;
import hep.dataforge.meta.MetaBuilder;
import hep.dataforge.storage.api.Storage;
import hep.dataforge.storage.api.StorageType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by darksnake on 17-May-17.
 */
public class NumassStorageFactory implements StorageType {

    /**
     * Build local storage with Global context. Used for tests.
     *
     * @param file
     * @return
     */
    @NotNull
    public static NumassStorage buildLocal(File file, boolean monitor) {
        Path path = file.toPath();
        Meta meta = new MetaBuilder("storage")
                .setValue("path", path)
                .setValue("monitor", monitor);

        return new NumassStorage(Global.instance(), meta, path);
    }

    @Override
    public String type() {
        return "numass";
    }

    @NotNull
    @Override
    public Storage build(Context context, Meta meta) {
        if (meta.hasValue("path")) {
            URI uri = URI.create(meta.getString("path"));
            Path path;
            if (uri.getScheme().startsWith("ssh")) {
                try {
                    String username = meta.getString("userName", uri.getUserInfo());
                    //String host = meta.getString("host", uri.getHost());
                    int port = meta.getInt("port", 22);
                    SFTPEnvironment env = new SFTPEnvironment()
                            .withUsername(username)
                            .withPassword(meta.getString("password","").toCharArray());
                    FileSystem fs = FileSystems.newFileSystem(uri, env,context.getClassLoader());
                    path = fs.getPath(uri.getPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                path = Paths.get(uri);
            }
            return new NumassStorage(context, meta, path);
        } else {
            context.getLogger().warn("A storage path not provided. Creating default root storage in the working directory");
            return new NumassStorage(context, meta, context.getIo().getWorkDirectory());
        }
    }
}
