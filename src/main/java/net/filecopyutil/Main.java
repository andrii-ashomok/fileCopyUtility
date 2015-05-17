package net.filecopyutil;

import net.filecopyutil.core.FileService;
import net.filecopyutil.core.FileServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main {
    private static Logger log  = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                log.error("ERR001 - Uncaught exception occurred: ", e);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        });

        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        FileService fs = context.getBean(FileServiceImpl.class);
        fs.init();
    }
}
