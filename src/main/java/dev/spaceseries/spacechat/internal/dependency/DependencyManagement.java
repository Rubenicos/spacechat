package dev.spaceseries.spacechat.internal.dependency;

import dev.spaceseries.spacechat.SpaceChat;
import dev.spaceseries.spacechat.internal.dependency.relocation.JarRelocationConvention;
import dev.spaceseries.spacechat.internal.dependency.relocation.JarRelocator;
import dev.spaceseries.spacechat.internal.dependency.relocation.Relocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This is the full dependency management class, which handles the dependencies of the project by
 * managing them. It first creates a folder if it cannot be found. Then, it creates a relocation
 * folder for the relocated jars. After that, the methods are up to the user to decide when to
 * install, load, or relocate the binaries.
 */
public class DependencyManagement {

    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private final Set<File> files;
    private final File dir;
    private final File relocatedDir;
    private final Logger logger;

    /**
     * Instantiates a new DependencyManagement.
     *
     * @param file file directory
     */
    public DependencyManagement(@NotNull final File file) {
        files = new HashSet<>();
        dir = file;
        this.logger = SpaceChat.getInstance().getLogger();
        if (!dir.exists()) {
            if (dir.mkdir() || dir.mkdirs()) {
                logger.info(
                        String.format(
                                "Dependency Directory (%s) does not exist... Creating a folder",
                                dir.getAbsolutePath()));
            } else {
                logger.info(String.format("Dependency Directory (%s) exists!", dir.getAbsolutePath()));
            }
        }
        relocatedDir = new File(dir, "relocated");
        if (!relocatedDir.exists()) {
            if (relocatedDir.mkdir()) {
                logger.info(
                        String.format(
                                "Relocated Directory (%s) does not exist... Creating a folder",
                                relocatedDir.getAbsolutePath()));
            } else {
                logger.info(
                        String.format("Relocated Directory (%s) exists!", relocatedDir.getAbsolutePath()));
            }
        }
    }

    /**
     * Installs all libraries from links.
     */
    public void install() {
        final List<Callable<Object>> tasks = new ArrayList<>();
        for (final RepositoryDependency dependency : RepositoryDependency.values()) {
            if (!checkExists(relocatedDir, dependency)) {
                tasks.add(Executors.callable(() -> installDependency(dependency)));
            }
        }
        try {
            EXECUTOR_SERVICE.invokeAll(tasks);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Installs a specific dependency.
     *
     * @param dependency the repository dependency
     */
    private void installDependency(@NotNull final RepositoryDependency dependency) {
        final String artifact = dependency.getArtifact();
        File file = null;
        if (dependency.getResolution() == DependencyResolution.MAVEN_DEPENDENCY) {
            logger.info(String.format("Checking Maven Central Repository for %s", artifact));
            try {
                file = DependencyUtilities.downloadMavenDependency(dependency, dir.getAbsolutePath());
            } catch (final IOException e) {
                logger.info(String.format("Could NOT find %s in Maven Central Repository!", artifact));
                e.printStackTrace();
            }
        } else if (dependency.getResolution() == DependencyResolution.JITPACK_DEPENDENCY) {
            logger.info(String.format("Checking Jitpack Central Repository for %s", artifact));
            try {
                file = DependencyUtilities.downloadJitpackDependency(dependency, dir.getAbsolutePath());
            } catch (final IOException e) {
                logger.info(String.format("Could NOT find %s in Jitpack Central Repository!", artifact));
                e.printStackTrace();
            }
        }else if (dependency.getResolution() == DependencyResolution.YAKO_DEPENDENCY) {
            logger.info(String.format("Checking Yakovliam Releases Repository for %s", artifact));
            try {
                file = DependencyUtilities.downloadYakoDependency(dependency, dir.getAbsolutePath());
            } catch (final IOException e) {
                logger.info(String.format("Could NOT find %s in Jitpack Central Repository!", artifact));
                e.printStackTrace();
            }
        }
        if (file != null) {
            files.add(file);
        }
    }

    /**
     * Relocates Dependencies.
     */
    public void relocate() {
        for (final File f : Objects.requireNonNull(dir.listFiles())) {
            if (f.getName().contains("asm")) {
                try {
                    DependencyUtilities.loadDependency(f);
                    files.remove(f);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
        final List<Relocation> relocations =
                Arrays.stream(JarRelocationConvention.values())
                        .map(JarRelocationConvention::getRelocation)
                        .collect(Collectors.toList());
        final List<Callable<Object>> tasks = new ArrayList<>();
        for (final File f : files) {
            tasks.add(
                    Executors.callable(
                            () -> {
                                try {
                                    new JarRelocator(f, new File(relocatedDir, f.getName()), relocations).run();
                                } catch (final IOException e) {
                                    e.printStackTrace();
                                }
                            }));
        }
        try {
            EXECUTOR_SERVICE.invokeAll(tasks);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Install and load.
     */
    public void load() {
        for (final File f : Objects.requireNonNull(relocatedDir.listFiles())) {
            try {
                DependencyUtilities.loadDependency(f);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if dependency exists in the directory beforehand.
     *
     * @param dir        the directory
     * @param dependency the dependency
     * @return the boolean
     */
    private boolean checkExists(
            @NotNull final File dir, @NotNull final RepositoryDependency dependency) {
        if (!dir.exists()) {
            return false;
        }
        for (final File f : Objects.requireNonNull(dir.listFiles())) {
            if (f.getName().contains(dependency.getArtifact())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the files that were downloaded.
     *
     * @return the set of files downloaded
     */
    public Set<File> getFiles() {
        return files;
    }
}