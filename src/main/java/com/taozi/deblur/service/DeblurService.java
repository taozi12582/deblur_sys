package com.taozi.deblur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeblurService {


    private static final Logger logger = LoggerFactory.getLogger(DeblurService.class);
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    public List<String> doDeblur(int gpuNum, String imgName) throws IOException {
        String blurDir = "/stdStorage/taozi/deblur_sys/img/blur/";
        String sharpDir = "/stdStorage/taozi/deblur_sys/img/sharp/";
        List<String> filePaths = new ArrayList<>();
        Process process = null;
        String[] shell = {"/stdStorage/taozi/deblur_sys/src/main/java/com/taozi/deblur/util/doDeblur.sh", "/stdStorage/taozi/deblur_sys/img/blur", "/stdStorage/taozi/deblur_sys/img/sharp", gpuNum + ""};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while (null != (line = reader.readLine())) {
            logger.info(line);
        }
//        commander.println("python run_model.py --input_path=" + blurDir + " --output_path=" + sharpDir + " --gpu=" + gpuNum);
        filePaths.add("http://202.115.17.206:8081/blur/" + imgName);
        filePaths.add("http://202.115.17.206:8081/sharp/" + imgName);
        if (!waitForProcess(sharpDir + imgName)) {
            return null;
        }
        return filePaths;
    }

    private Boolean waitForProcess(String filePath) {
        logger.info(filePath);
        File file = new File(filePath);
        logger.info("等待中..." + filePath);
        int num = 0, redo = 60000 / 100;
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
