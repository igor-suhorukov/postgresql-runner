package com.github.igorsuhorukov.postgresql.model;

import java.io.File;

public class FileWithArgs {
    private File file;
    private String[] cliArgs;

    public FileWithArgs(File file, String[] cliArgs) {
        this.file = file;
        this.cliArgs = cliArgs;
    }

    public File getFile() {
        return file;
    }

    public String[] getCliArgs() {
        return cliArgs;
    }
}
