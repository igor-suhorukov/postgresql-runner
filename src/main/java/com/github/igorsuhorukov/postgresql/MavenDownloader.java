package com.github.igorsuhorukov.postgresql;

import com.github.igorsuhorukov.smreed.dropship.MavenClassLoader;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.config.store.IPackageResolver;
import de.flapdoodle.embed.process.distribution.ArchiveType;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.store.IDownloader;

import java.io.File;
import java.io.IOException;

/**
 * Download PG artifact from maven compatible binary repository manager
 */
public class MavenDownloader implements IDownloader {
    @Override
    public String getDownloadUrl(IDownloadConfig runtime, Distribution distribution) {
        return runtime.getDownloadPath().getPath(distribution) + runtime.getPackageResolver().getPath(distribution);
    }

    @Override
    public File download(IDownloadConfig runtime, Distribution distribution) throws IOException {
        String downloadPath = runtime.getDownloadPath().getPath(distribution);
        String[] path = downloadPath.split("\\?");
        try {
            if(path.length==2){
                String repository = path[1];
                String groupAndArtifact = path[0];
                String groupArtifactVersion = getGroupArtifactVersion(groupAndArtifact, distribution, runtime.getPackageResolver());
                return new File(MavenClassLoader.using(repository).resolveArtifact(groupArtifactVersion).getFile());
            } else {
                String groupArtifactVersion = getGroupArtifactVersion(downloadPath, distribution, runtime.getPackageResolver());
                return new File(MavenClassLoader.usingCentralRepo().resolveArtifact(groupArtifactVersion).getFile());
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static String getGroupArtifactVersion(String groupAndArtifact, Distribution distribution, IPackageResolver packageResolver) {
        String sversion = distribution.getVersion().asInDownloadPath();

        ArchiveType archiveType = packageResolver.getArchiveType(distribution);
        String sarchiveType;
        switch (archiveType) {
            case TGZ:
                sarchiveType = "tar.gz";
                break;
            case ZIP:
                sarchiveType = "zip";
                break;
            default:
                throw new IllegalArgumentException("Unknown ArchiveType "
                        + archiveType);
        }

        String splatform;
        switch (distribution.getPlatform()) {
            case Linux:
                splatform = "linux";
                break;
            case Windows:
                splatform = "windows";
                break;
            case OS_X:
                splatform = "osx";
                break;
            default:
                throw new IllegalArgumentException("Unknown Platform "
                        + distribution.getPlatform());
        }

        String bitsize = "";
        switch (distribution.getBitsize()) {
            case B32:
                switch (distribution.getPlatform()) {
                    case Windows:
                    case Linux:
                    case OS_X:
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "32 bit supported only on Windows, MacOS, Linux, platform is "
                                        + distribution.getPlatform());
                }
                break;
            case B64:
                switch (distribution.getPlatform()) {
                    case Linux:
                    case Windows:
                        bitsize = "-x64";
                        break;
                    case OS_X:
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "64 bit supported only on Linux and Windows, platform is "
                                        + distribution.getPlatform());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown BitSize " + distribution.getBitsize());
        }

        return String.format("%s:%s:%s%s:%s", groupAndArtifact, sarchiveType, splatform, bitsize, sversion);
    }

}
