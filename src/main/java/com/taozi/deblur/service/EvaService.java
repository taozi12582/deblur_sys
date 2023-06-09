package com.taozi.deblur.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;


@Service
public class EvaService {

    private static final Logger logger = LoggerFactory.getLogger(EvaService.class);
    @Autowired
    private PrintStream commander;

    public String getImgValue(String imgName, int gpuNum) throws IOException, JSchException {
        Process process = null;
        String[] shell = {"/stdStorage/taozi/deblur_sys/src/main/java/com/taozi/deblur/util/getValue.sh", imgName, "" + gpuNum};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader rb = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String info = "";
//        while (null != (info = rb.readLine())) {
//            logger.info(info);
//        }
//        String imgNameB = imgName.substring(0, imgName.lastIndexOf("."));
//        String pathName = "/stdStorage/taozi/deblur_sys/" + imgNameB + ".txt";
//        if (!waitForProcess(pathName)) {
//            return null;
//        }
//        FileReader reader = new FileReader(pathName);
//        BufferedReader br = new BufferedReader(reader);
//        String line;
//        line = br.readLine();
//        logger.info(line);
//        commander.println("rm -rf " + pathName);
        if (null != (info = rb.readLine())) {
            logger.info(info);
        }
        return info;
    }

    private Boolean waitForProcess(String filePath) {
        logger.info(filePath);
        File file = new File(filePath);
        logger.info("等待中..." + filePath);
        int num = 0, redo = 20000 / 100;
        while (!file.exists() && num <= redo) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            num++;
        }
        if (num >= redo) {
            logger.info("处理超时");
            return false;
        }
        logger.info("处理完成");
        return true;
    }


}
