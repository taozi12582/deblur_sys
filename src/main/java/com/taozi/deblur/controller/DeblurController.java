package com.taozi.deblur.controller;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.taozi.deblur.pojo.ImgInfo;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    public String uploadImage(@RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        String blurDir = "/stdStorage/taozi/deblur_sys/img/blur/";
        Process process;
        String[] shell = {"rm", "-rf", blurDir + "*"};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        commander.println("rm -rf /stdStorage/taozi/deblur_sys/img/blur/*");
        logger.info("文件名：" + imageFile.getOriginalFilename());
        logger.info("文件大小：" + imageFile.getSize() + " bytes");
        String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
        Path filePath = Paths.get(blurDir + fileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("图片上传成功");
        return fileName;
    }

    /**
     * 图像去模糊
     */
    @PostMapping("/doDeblur")
    @ResponseBody
    @CrossOrigin
    public List<String> doDeblur(@RequestParam("gpuNum") int gpuNum, @RequestParam("imgName") String imgName) throws IOException {
        String key = "gpu" + gpuNum;
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(key, 1);
        if (!absent) {
            return null;
        }
        logger.info(key + "已加锁");
        redisTemplate.expire(key, 30000, TimeUnit.MILLISECONDS);
        return deblurService.doDeblur(gpuNum, imgName);
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
        logger.info("释放锁"+key);
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
        List<Boolean> res = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            res.add(!redisTemplate.hasKey("gpu" + i) && gpuInfo.get(i));
            logger.info(String.valueOf(redisTemplate.hasKey("gpu" + i)));
        }
        logger.info(gpuInfo.toString());
        return res;
    }

    /**
     * 需和doDeblur使用同一块cpu
     * img的指标和图片异步返回，同时进行
     * */
    @PostMapping("/getImginfo")
    @ResponseBody
    @CrossOrigin
    public String[] getImginfo(@RequestParam("gpuNum") int gpuNum, @RequestParam("imgName") String imgName) throws JSchException, IOException {
        String imgValue = evaService.getImgValue(imgName, gpuNum);
        return imgValue.split(",");
    }

}
