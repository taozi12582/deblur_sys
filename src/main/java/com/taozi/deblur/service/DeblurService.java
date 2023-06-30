package com.taozi.deblur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeblurService {


    private static final Logger logger = LoggerFactory.getLogger(DeblurService.class);
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    public List<String> doDeblur(int gpuNum, String imgName, String taskId) throws IOException {
        String blurDir = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/blur/";
        String sharpDir = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/sharp/";
        List<String> filePaths = new ArrayList<>();
        Process process = null;
        String[] shell = {"/stdStorage/taozi/deblur_sys/src/main/java/com/taozi/deblur/util/doDeblur.sh", blurDir, sharpDir, gpuNum + ""};
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

    public Boolean doDeblurAll(int gpuNum, String taskId) throws IOException {
        String blurDir = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/blur/";
        String sharpDir = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/sharp/";
        Process process = null;
        String[] shell = {"/stdStorage/taozi/deblur_sys/src/main/java/com/taozi/deblur/util/selfdeblur.sh", blurDir, sharpDir, gpuNum + ""};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while (null != (line = reader.readLine())) {
            logger.info(line);
        }
        saveRes2Redis("/home/tzx/selfDeblurPredict/res.txt");
        return true;
    }

    private void saveRes2Redis(String path) {
        try {
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String lineTxt1;
                while ((lineTxt1 = br.readLine()) != null) {
                    String[] res = lineTxt1.toString().split(",");
                    redisTemplate.opsForValue().append(res[0], res[1]);
                    logger.info(res[0] + "\t" + res[1]);
                }
                br.close();
            } else {
                System.out.println("文件不存在!");
            }
            file.delete();
        } catch (Exception e) {
            System.out.println("文件读取错误!");
        }
    }

    public String uploadImage(MultipartFile imageFile, String taskId) throws IOException {
        Process process = null;
        String blurDir = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/blur/";
        String sharpDir = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/sharp/";
        String[] shell = {"mkdir", "-p", blurDir, sharpDir};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while (null != (line = reader.readLine())) {
            logger.info(line);
        }
        String imgName = imageFile.getOriginalFilename();
        String fileName = System.currentTimeMillis() + imgName.substring(imgName.length() - 4, imgName.length());
        Path filePath = Paths.get(blurDir + fileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }
}
