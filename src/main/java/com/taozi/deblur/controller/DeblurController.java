package com.taozi.deblur.controller;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.taozi.deblur.service.DeblurService;
import com.taozi.deblur.service.EvaService;
import com.taozi.deblur.service.GPUService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/deblur")
public class DeblurController {

    private static final Logger logger = LoggerFactory.getLogger(DeblurController.class);

    @Autowired
    private Session session;
    @Autowired
    private Channel channel;
    @Autowired
    private PrintStream commander;
    @Autowired
    private ByteArrayOutputStream os;
    @Autowired
    RedisTemplate<String, Object> redisTemplate;
    @Autowired
    GPUService gpuService;
    @Autowired
    DeblurService deblurService;
    @Autowired
    EvaService evaService;

    @GetMapping("/login")
    public String login() {
        return "index.html";
    }

    /**
     * 上传图片文件
     */
    @PostMapping("/upload")
    @ResponseBody
    @CrossOrigin
    public String uploadImage(@RequestParam("imageFile") MultipartFile imageFile, @RequestParam("taskId") String taskId) throws IOException {
        logger.info("tsakId:" + taskId);
        logger.info("文件名：" + imageFile.getOriginalFilename());
        String fileName = deblurService.uploadImage(imageFile, taskId);
        logger.info("文件大小：" + imageFile.getSize() + " bytes");
        logger.info("图片上传成功");
        return fileName;
    }

    @PostMapping("/uploadVideo")
    @ResponseBody
    @CrossOrigin
    public String uploadVideo(@RequestParam("videoFile") MultipartFile videoFile, @RequestParam("taskId") String taskId) throws IOException {
        logger.info("tsakId:" + taskId);
        logger.info("文件名：" + videoFile.getOriginalFilename());
        String fileName = deblurService.uploadVedio(videoFile, taskId);
        logger.info("文件大小：" + videoFile.getSize() + " bytes");
        logger.info("视频上传成功");
        return fileName;
    }

    @PostMapping("/v2img")
    @ResponseBody
    @CrossOrigin
    public boolean changeVideo(@RequestParam("videoName") String videoName, @RequestParam("taskId") String taskId) throws IOException {
        deblurService.changeVideo(videoName, taskId);
        return true;
    }

    /**
     * 图像去模糊
     */
    @PostMapping("/doDeblur")
    @ResponseBody
    @CrossOrigin
    public List<String> doDeblur(@RequestParam("gpuNum") int gpuNum, @RequestParam("imgName") String imgName, @RequestParam("taskId") String taskId) throws IOException {
        String key = "gpu" + gpuNum;
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, 1);
        if (!absent) {
            return null;
        }
        logger.info(key + "已加锁");
        redisTemplate.expire(key, 30000, TimeUnit.MILLISECONDS);
        List<String> res = deblurService.doDeblur(gpuNum, imgName, taskId);
        return res;
    }

    @PostMapping("/doDeblurAll")
    @ResponseBody
    @CrossOrigin
    public Boolean doDeblurAll(@RequestParam("gpuNum") int gpuNum, @RequestParam("taskId") String taskId,
                               @RequestParam(value = "isVideo", defaultValue = "false") boolean isVideo,
                               @RequestParam(value = "frame", defaultValue = "60") String frame) throws IOException {
        String key = "gpu" + gpuNum;
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, 1);
        if (!absent) {
            return null;
        }
        logger.info(key + "已加锁");
        redisTemplate.expire(key, 30000, TimeUnit.MILLISECONDS);
        Boolean res = deblurService.doDeblurAll(gpuNum, taskId, isVideo);
        if (isVideo) {
            deblurService.makeVideo(taskId, Float.parseFloat(frame));
        }
        return res;
    }

//cd /home/tzx/.cache/JetBrains/RemoteDev/dist/5d97d7e529d1b_ideaIU-2023.2.2/plugins/remote-dev-server/bin/

    /**
     * type 0: ir
     * type 1: vis
     * model 0: srn
     * model 1: self
     **/
    @PostMapping("/datasetDeblur")
    @ResponseBody
    @CrossOrigin
    public Boolean datasetDeblur(@RequestParam("taskId") String taskId,
                                 @RequestParam("type") Integer type, @RequestParam("model") Integer model,
                                 @RequestParam("nameList") String... nameList) throws IOException {
        int gpuNum = gpuService.selectGPU();
        if (gpuNum < 0) {
            logger.info("无空闲gpu");
        }
        String key = "gpu" + gpuNum;
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, 1);
        if (!absent) {
            return null;
        }
        logger.info(key + "已加锁");
        redisTemplate.expire(key, 30000, TimeUnit.MILLISECONDS);
        Boolean res = deblurService.datasetDeblur(gpuNum, taskId, nameList, type, model);
        return res;
    }


    @PostMapping("/getConsole")
    @ResponseBody
    @CrossOrigin
    public String getConsole() {
        String s = os.toString();
        os.reset();
        return s;
    }

    /**
     * 释放gpu锁
     */
    @PostMapping("/releaseGPU")
    @ResponseBody
    @CrossOrigin
    public Boolean testRedis(@RequestParam("gpuNum") int gpuNum) {
        String key = "gpu" + gpuNum;
        logger.info("释放锁" + key);
        return redisTemplate.delete(key);
    }

    /**
     * 获取四张显卡的使用信息
     **/
    @PostMapping("/getGPUinfo")
    @ResponseBody
    @CrossOrigin
    public List<Boolean> testGPU() throws IOException {
        List<Boolean> gpuInfo = gpuService.getGPUList();
        return gpuInfo;
    }

    /**
     * 需和doDeblur使用同一块cpu
     * img的指标和图片异步返回，同时进行
     */
    @PostMapping("/getImgInfo")
    @ResponseBody
    @CrossOrigin
    public String[] getImginfo(@RequestParam("imgName") String imgName, @RequestParam("taskId") String taskId) throws JSchException, IOException {
        logger.info("getImginfo:\t" + imgName + "\t" + taskId);
        String imgValue = evaService.getImgValue(imgName, taskId);
        return imgValue.split(",");
    }

    @PostMapping("/getImgDisplay")
    @ResponseBody
    @CrossOrigin
    public String[] getImgDisplay(@RequestParam("imgName") String imgName, @RequestParam("taskId") String taskId) {
        String time = String.valueOf(redisTemplate.opsForValue().get(imgName));
        String[] res = {"http://202.115.17.206:8081/" + taskId + "/blur/" + imgName, "http://202.115.17.206:8081/" + taskId + "/sharp/" + imgName, time};
        logger.info("redis key: " + imgName + "\tvalue: " + time);
        return res;
    }


    @PostMapping("/deleteTask")
    @ResponseBody
    @CrossOrigin
    public Boolean deleteTask(@RequestParam("taskId") String taskId) throws IOException {
        String taskPath = "/stdStorage/taozi/deblur_sys/img/" + taskId;
        Process process = null;
        String[] shell = {"rm", "-rf", taskPath};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while (null != (line = reader.readLine())) {
            logger.info(line);
        }
        logger.info(taskId + "文件夹已删除");
        return true;
    }

    @PostMapping("/deleteImg")
    @ResponseBody
    @CrossOrigin
    public Boolean deleteImg(@RequestParam("taskId") String taskId, @RequestParam("imgName") String imgName) throws IOException {
        String taskPath = "/stdStorage/taozi/deblur_sys/img/" + taskId + "/" + imgName;
        Process process = null;
        String[] shell = {"rm", "-rf", taskPath};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = "";
        while (null != (line = reader.readLine())) {
            logger.info(line);
        }
        logger.info(taskId + "/" + imgName + "已删除");
        return true;
    }


    /**
     * imgName 需全名
     */
//    @PostMapping("/showDemo")
//    @ResponseBody
//    @CrossOrigin
//    public String[] showDemo(@RequestParam("gpuNum") int gpuNum, @RequestParam("imgName") String imgName) throws JSchException, IOException {
//        logger.info("showDemo:\t" + imgName);
//        String imgValue = evaService.getImgValue(imgName, gpuNum, "demoTruth");
//        return imgValue.split(",");
//    }


    @PostMapping("/imgList")
    @ResponseBody
    @CrossOrigin
    public String[] getImgList(@RequestParam("imgType") String imgType) {
        int datasetNum = 7838;
        return deblurService.getImgList(imgType, datasetNum);
    }
}