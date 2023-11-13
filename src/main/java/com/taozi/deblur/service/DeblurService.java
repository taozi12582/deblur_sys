package com.taozi.deblur.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DeblurService {


    private static final Logger logger = LoggerFactory.getLogger(DeblurService.class);
    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Value("${server.imgServerPath}")
    private String imgServerPath;
    @Value("${server.shServerPath}")
    private String shServerPath;
    @Value("${server.imgTomcatPath}")
    private String imgTomcatPath;

    public List<String> doDeblur(int gpuNum, String imgName, String taskId) throws IOException {
        String blurDir = imgServerPath + taskId + "/blur/";
        String sharpDir = imgServerPath + taskId + "/sharp/";
        List<String> filePaths = new ArrayList<>();
        String[] shell = {shServerPath + "doDeblur.sh", blurDir, sharpDir, gpuNum + ""};
        doShell(shell);
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

    public Boolean doDeblurAll(int gpuNum, String taskId, Boolean isVideo) throws IOException {
        String blurDir = imgServerPath + taskId + "/blur/";
        String sharpDir = imgServerPath + taskId + "/sharp/";
        String[] shell = {shServerPath + "selfdeblur.sh", blurDir, sharpDir, gpuNum + ""};
        doShell(shell);
        if (!isVideo) {
            saveRes2Redis("/home/tzx/selfDeblurPredict/res.txt");
        }
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
        String blurDir = imgServerPath + taskId + "/blur/";
        String sharpDir = imgServerPath + taskId + "/sharp/";
        String[] shell = {"mkdir", "-p", blurDir, sharpDir};
        doShell(shell);
        String imgName = imageFile.getOriginalFilename();
        String fileName = System.currentTimeMillis() + imgName.substring(imgName.length() - 4, imgName.length());
        Path filePath = Paths.get(blurDir + fileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public String uploadVedio(MultipartFile videoFile, String taskId) throws IOException {
        String videoDir = imgServerPath + taskId + "/video/";
        String imgDir = imgServerPath + taskId + "/blur/";
        String sharpDir = imgServerPath + taskId + "/sharp/";
        String videoRes = imgServerPath + taskId + "/videoRes/";
        String[] shell = {"mkdir", "-p", videoDir, imgDir, sharpDir, videoRes};
        doShell(shell);
        String imgName = videoFile.getOriginalFilename();
        String fileName = System.currentTimeMillis() + imgName.substring(imgName.length() - 4, imgName.length());
        Path filePath = Paths.get(videoDir + fileName);
        Files.copy(videoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        String frame = changeVideo(fileName, taskId);
        logger.info("抽帧完成...");
        return fileName + ',' + frame;
    }


    public void makeVideo(String taskId, float frame) throws IOException {
        String imgPath = imgServerPath + taskId + "/sharp/";
        String resVideoPath = imgServerPath + taskId + "/videoRes/" + "res.avi";
        String[] shell = {shServerPath + "i2v.sh", imgPath, resVideoPath, String.valueOf(frame)};
        doShell(shell);
    }


    public String changeVideo(String videoName, String taskId) throws IOException {
        logger.info("开始抽帧...");
        String videoDir = imgServerPath + taskId + "/video/";
        String imgDir = imgServerPath + taskId + "/blur/";
        Process process = null;
        String[] shell = {shServerPath + "v2i.sh", videoDir + videoName, imgDir};
        return doShell(shell);
    }

    public Boolean datasetDeblur(int gpuNum, String taskId, String[] nameList, Integer type, Integer model) throws IOException {
        String originDir = imgServerPath + "dataset/motion/" + (type == 0 ? "ir" : "vis");
        //创建文件夹
        for (String s : nameList) {
            logger.info(s);
        }
        String dirName = imgServerPath + taskId;
        makeDir(dirName + "/blur");
        makeDir(dirName + "/sharp");
        //复制到新文件夹
        for (String imgName : nameList) {
            String blurPath = originDir + "/blur/" + imgName;
            String sharpPath = originDir + "/sharp/" + imgName;
            cpFile(blurPath, dirName + "/blur");
            cpFile(sharpPath, dirName + "/sharp");
        }
        deblurProcessSelect(dirName + "/blur", dirName + "/sharp", gpuNum, model);
        return true;
    }

    private void deblurProcessSelect(String blurDir, String sharpDir, int gpuNum, Integer model) throws IOException {
        String[] shell = {shServerPath + "doDeblur.sh", blurDir, sharpDir, gpuNum + ""};
        doShell(shell);
    }

    private void makeDir(String dirName) throws IOException {
        String[] shell = {"mkdir", "-p", dirName};
        doShell(shell);
    }

    private void cpFile(String fileName, String destDir) throws IOException {
        String[] shell = {"cp", fileName, destDir};
        logger.info(Arrays.toString(shell));
        doShell(shell);
    }


    private Boolean deblurProcess(String blurDir, String sharpDir, int gpuNum) throws IOException {
        String[] shell = {shServerPath + "selfdeblur.sh", blurDir, sharpDir, gpuNum + ""};
        doShell(shell);
        saveRes2Redis("/home/tzx/selfDeblurPredict/res.txt");
        return true;
    }

    private String doShell(String[] shell) throws IOException {
        Process process = null;
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        String res = null;
        while (null != (line = reader.readLine())) {
            logger.info(line);
            res = line;
        }
        return res;
    }

    public String[] getImgList(String imgType, int datasetNum) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < datasetNum; i++) {
            String dirPrefix = imgType.equals("vis") ? imgTomcatPath + "vis/blur/" : imgTomcatPath + "ir/blur/";
            sb.append(dirPrefix).append(String.format("%06d", i)).append(".jpg#");
        }
        sb.append(String.format("%06d", datasetNum)).append(".jpg");
        return sb.toString().split("#");
    }
}
