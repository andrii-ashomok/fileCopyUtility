package net.filecopyutil.core;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FileServiceImpl implements FileService {
    private static Logger log  = LoggerFactory.getLogger(FileServiceImpl.class);

    @Value("${executor.pool.size}")
    private int poolSize;

    // param -DsourcePath, it is path to files that will be coping
    private String sourcePath;

    // param -DcountCopyFiles, it is count of files that will be coping
    private int countCopyFiles;

    // param -DdestinationPath, it can be more then one path where file copies will be saving
    private String destinationPath;

    // param -DsplitCountCopyFiles, destinationPath size
    private int splitCountCopyFiles;

    private List<File> destinationFolderPathList;

    private ExecutorService executor;

    private Predicate<String> isStrNonEmpty = str -> Objects.isNull(str) || str.isEmpty();

    private Predicate<Integer> isCountPositive = count -> count <= 0;

    private Predicate<File> isFileFolder = file -> file.exists() && file.isDirectory();

    private Predicate<List> isListNonValid = list -> Objects.isNull(list) || list.isEmpty();


    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public void setCountCopyFiles(int countCopyFiles) {
        this.countCopyFiles = countCopyFiles;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public void setSplitCountCopyFiles(int splitCountCopyFiles) {
        this.splitCountCopyFiles = splitCountCopyFiles;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
        executor = Executors.newFixedThreadPool(poolSize);
    }

    @Override
    public void init() {
        executor = Executors.newSingleThreadExecutor();

        if (isValidInputArg()) {
            cleanDestinationFolders();
            copy();
        }
    }

    @Override
    public boolean isValidInputArg() {
        if (isStrNonEmpty.test(sourcePath)) {
            log.error("No source folder path (check parameter \"-DsourcePath\")");
            return false;
        }
        
        if (isStrNonEmpty.test(destinationPath)) {
            log.error("No destination folders path (check parameter \"-DdestinationPath\")");
            return false;
        }
        
        if (isCountPositive.test(countCopyFiles)) {
            log.error("Count of files to copy need to be bigger then 0 (check parameter \"-DcountCopyFiles\")");
            return false;
        }

        if (isCountPositive.test(splitCountCopyFiles)) {
            log.error("Count of files to copy in each folder need to be bigger then 0 (check parameter \"-DsplitCountCopyFiles\")");
            return false;
        }

        if (countCopyFiles < splitCountCopyFiles) {
            log.error("Parameter \"-DsplitCountCopyFiles\" bigger then \"-DcountCopyFiles\"");
            return false;
        }

        File fileSourcePath = new File(sourcePath);
        if (!isFileFolder.test(fileSourcePath)) {
            log.error("The Source folder path {} doesn't exists or not directory (check parameter \"-DsourcePath\")",
                    sourcePath);
            return false;
        }

        if (isListNonValid.test(Arrays.asList(fileSourcePath.listFiles()))) {
            log.error("Not found any files to copy in {}", fileSourcePath.getPath());
            return false;
        }

        if (!isValidDestination())
            return false;

        log.info("Start to copy {} files with step {} from {} to each of {} ",
                countCopyFiles, splitCountCopyFiles, sourcePath, destinationPath);

        return true;
    }

    @Override
    public boolean isValidDestination() {
        if (destinationPath.contains(","))
            destinationFolderPathList = Stream
                    .of(destinationPath.split(","))
                    .map(File::new)
                    .filter(this::isValidFolderPath)
                    .collect(Collectors.toList());
        else
            destinationFolderPathList = Stream.of(destinationPath)
                    .map(File::new)
                    .filter(this::isValidFolderPath)
                    .collect(Collectors.toList());

        if (isListNonValid.test(destinationFolderPathList)) {
            log.error("No valid destination folders (check parameter \"-DdestinationPath\")");
            return false;
        }

        return true;
    }

    private boolean isValidFolderPath(File file) {
        if (isFileFolder.test(file))
            return true;

        log.error("The folder path {} doesn't exists or not directory",
                file.getPath());
        return false;
    }

    @Override
    public void copy() {
        List<File> sourceFileList = Stream.of(new File(sourcePath).listFiles())
                .collect(Collectors.toList());

        int currentValue = 0;
        List<File> subFileList;
        List<File> copyFileList;
        if (countCopyFiles < sourceFileList.size())
            subFileList = sourceFileList.subList(currentValue, countCopyFiles);
        else
            subFileList = sourceFileList;

        int arrSize = subFileList.size();
        log.debug("Prepared {} files to copy", arrSize);

        TaskExecutor taskExecutor = new ConcurrentTaskExecutor();

        int nextPosition = 0;
        while (arrSize > currentValue) {

            nextPosition += splitCountCopyFiles;
            if (nextPosition > arrSize)
                nextPosition = arrSize;

            log.debug("Get position from {} to {}",
                    currentValue, nextPosition);

            copyFileList = subFileList.subList(currentValue, nextPosition);
            currentValue = nextPosition;

            if (isListNonValid.test(copyFileList)) {
                log.info("No files to copy");
                break;
            }

            executor.submit(new FileCopyHandler(copyFileList));

//            taskExecutor.execute(new FileCopyHandler(copyFileList));

            log.debug("Processed {} files", currentValue);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Error while shutdown execute service {}", e.getMessage(), e);
        }

        log.info("Copy finished");
        destinationFolderPathList.stream()
                .forEach(dir -> log.info("Directory {} has {} files",
                        dir.getName(), dir.listFiles().length));

    }

    public void cleanDestinationFolders() {
        destinationFolderPathList.stream()
                .peek(file -> log.debug("Remove files from {}", file.getAbsolutePath()))
                .flatMap(folder -> Stream.of(folder.listFiles()))
                .forEach(file -> {
                    log.debug("Remove file {}", file.getAbsolutePath());
                    file.delete();
                });
    }

    private class FileCopyHandler implements Runnable {

        private List<File> fileList;

        FileCopyHandler(List<File> fileList) {
            this.fileList = fileList;
        }

        @Override
        public void run() {
            StopWatch watch = new StopWatch();
            watch.start();

            if (isListNonValid.test(fileList)) {
                log.error("File list incorrect while copy process continue, {}",
                        fileList);
                watch.stop();
                return;
            }

            int count = destinationFolderPathList.size() - 1;
            int currentFolderNumber = 0;

            File currentFile, destDir;
            for (int i = 0; i < fileList.size(); i++) {
                currentFile = fileList.get(i);
                destDir = destinationFolderPathList.get(currentFolderNumber);

                currentFolderNumber = currentFolderNumber >= count ? 0 : currentFolderNumber + 1;

                try {
                    FileUtils.copyFileToDirectory(currentFile, destDir);

                    log.info("Copy {} to {}",
                            currentFile.getName(), destDir.getAbsolutePath());

                } catch (IOException e) {
                    log.error("Error while {} file is coping to {}, {}",
                            currentFile.getName(), destDir.getAbsolutePath(), e.getMessage(), e);
                }
            }

            watch.stop();
            destinationFolderPathList.stream()
                    .forEach(dir -> log.debug("Directory {} has {} files, copy duration {} ms",
                            dir.getAbsolutePath(), dir.listFiles().length, watch.getLastTaskTimeMillis()));

        }
    }
}
