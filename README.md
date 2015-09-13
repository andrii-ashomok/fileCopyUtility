# fileCopyUtility
Programm copy files from one folder to another using threads.
Java 8, springframework 4

Run java to install  your source folders:
 -DsourcePath, folder path of files that will be copying;
 -DcountCopyFiles, number of files that will be copying;
 -DdestinationPath, it can be more then one path where file copies will be saving;
 -DsplitCountCopyFiles, split copy process;

Also you can use core.properties to install your source folders, but you need to add @Value from spring.
