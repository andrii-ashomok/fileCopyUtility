package net.filecopyutil.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Created by rado on 17.05.2015.
 */
@Test
public class FileServiceTest {

    private FileServiceImpl fileService = new FileServiceImpl();
    private static final String sourcePath = "src/test/resources/source";
    private static final String tempPath1 = "src/test/resources/temp1";
    private static final String tempPath2 = "src/test/resources/temp2";


    @Test(priority = 1)
    public void testIsValidInputArg() {
        assert !fileService.isValidInputArg();

        fileService.setSourcePath(sourcePath);

        assert !fileService.isValidInputArg();

        fileService.setCountCopyFiles(10);

        assert !fileService.isValidInputArg();

        fileService.setSplitCountCopyFiles(3);

        assert !fileService.isValidInputArg();

        fileService.setDestinationPath(new File(tempPath1).getAbsolutePath());

        assert fileService.isValidInputArg();

        fileService.setDestinationPath(new File(tempPath1).getAbsolutePath()
                .concat(",").concat(new File(tempPath2).getAbsolutePath()));

        assert fileService.isValidInputArg();

        fileService.setSplitCountCopyFiles(11);

        assert !fileService.isValidInputArg();
    }

    @Test(priority = 2)
    public void testIsValidDestination() {
        fileService.setDestinationPath("wrong/folder/path");
        assert !fileService.isValidDestination();

        fileService.setDestinationPath(tempPath1);
        assert fileService.isValidDestination();

        fileService.setDestinationPath(tempPath1.concat(",")
                .concat(tempPath2));
        assert fileService.isValidDestination();
    }

    @Test(priority = 3)
    public void testCopy() throws InterruptedException {
        fileService.setSourcePath(sourcePath);
        fileService.setDestinationPath(tempPath1.concat(",")
                .concat(tempPath2));
        fileService.setCountCopyFiles(5);
        fileService.setPoolSize(3);
        fileService.setSplitCountCopyFiles(2);
        assert fileService.isValidDestination();

        fileService.copy();

        Thread.sleep(1000);

        Stream<File> fileStream1 = Stream.of(new File(tempPath1)
                .listFiles());

        Stream<File> fileStream2 = Stream.of(new File(tempPath2)
                .listFiles());

        assert fileStream1.count() > 0;
        assert fileStream2.count() > 0;

        assert Stream.of(new File(tempPath1)
                .listFiles()).allMatch(File::delete);
        assert Stream.of(new File(tempPath2)
                .listFiles()).allMatch(File::delete);
    }

    @Test
    public void testSplitArrayByStep() {
        List<Integer> sourceList = new ArrayList<>();
        int x = 0;
        int max = 31;

        while (x < max) {
            sourceList.add(ThreadLocalRandom.current().nextInt());
            x++;
        }

        System.out.println("Size: " + sourceList.size());

        int userStep = 6;
        int currentValue = 0;
        int step = 0;

        List<Integer> subList = new ArrayList<>();
        while(currentValue < sourceList.size()) {
            step += userStep;

            if (step > sourceList.size())
                step = sourceList.size();

            System.out.println("From " + currentValue + " to " + step);

            subList.addAll(sourceList.subList(currentValue, step));

            currentValue = step;

            System.out.println("Size: " + subList.size());
        }

        assert subList.size() == sourceList.size();
    }

}
