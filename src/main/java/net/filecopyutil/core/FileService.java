package net.filecopyutil.core;

/**
 * Created by rado on 16.05.2015.
 */
public interface FileService {

    boolean isValidInputArg();

    void copy();

    void init();

    boolean isValidDestination();
}
