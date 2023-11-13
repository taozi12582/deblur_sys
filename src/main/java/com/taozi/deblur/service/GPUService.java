package com.taozi.deblur.service;

import com.taozi.deblur.controller.DeblurController;
import com.taozi.deblur.pojo.GPUInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class GPUService {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(GPUService.class);

    private String getGPU() throws IOException {
        Process process = null;
        String[] shell = {"/bin/bash", "-c", "nvidia-smi"};
        process = Runtime.getRuntime().exec(shell);
        process.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuffer stringBuffer = new StringBuffer();
        String line = "";
        while (null != (line = reader.readLine())) {
            stringBuffer.append(line + "\n");
        }
        System.out.println(stringBuffer.toString());
        return stringBuffer.toString();
    }

    public List<Boolean> getGPUList() throws IOException {
        List<GPUInfo> gpus = getGPUInfo();
        List<Boolean> res = new ArrayList<>();
        for (GPUInfo gpuInfo : gpus) {
            System.out.println(gpuInfo.getUsageRate());
            res.add(gpuInfo.getUsageRate() < 20);
        }
        List<Boolean> gpuInfo = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            gpuInfo.add(!redisTemplate.hasKey("gpu" + i) && res.get(i));
            logger.info(String.valueOf(redisTemplate.hasKey("gpu" + i)));
        }
        logger.info(gpuInfo.toString());
        return gpuInfo;
    }

    private List<GPUInfo> getGPUInfo() throws IOException {
        String gpus = getGPU();
        String[] split = gpus.split("\\|===============================\\+======================\\+======================\\|");
        String[] gpusInfo = split[1].split("                                                                               ");
        // 分割多个gpu
        String[] gpuInfo = gpusInfo[0].split("\\+-------------------------------\\+----------------------\\+----------------------\\+");
        List<GPUInfo> gpuInfoList = new ArrayList<>();
        for (int i = 0; i < gpuInfo.length - 1; i++) {
            GPUInfo gpuInfo1 = new GPUInfo();
            String[] nameAndInfo = gpuInfo[i].split("\n");
            String[] split1 = nameAndInfo[1].split("\\|")[1] // 0  TITAN V             Off
                    .split("\\s+");//去空格
            gpuInfo1.setNumber(Integer.parseInt(split1[1]));
            StringBuffer name = new StringBuffer();
            for (int j = 0; j < split1.length - 1; j++) {
                if (j > 1 && j != split1.length) {
                    name.append(split1[j] + " ");
                }
            }
            gpuInfo1.setName(name.toString());

            String[] info = nameAndInfo[2].split("\\|")[2].split("\\s+");
            gpuInfo1.setUsedMemory(info[1]);
            gpuInfo1.setTotalMemory(info[3]);
            int useable = Integer.parseInt(gpuInfo1.getTotalMemory().split("MiB")[0]) - Integer.parseInt(gpuInfo1.getUsedMemory().split("MiB")[0]);
            gpuInfo1.setUseableMemory(useable + "MiB");
            Double usageRate = Integer.parseInt(gpuInfo1.getUsedMemory().split("MiB")[0]) * 100.00 / Integer.parseInt(gpuInfo1.getTotalMemory().split("MiB")[0]);
            gpuInfo1.setUsageRate(usageRate);
            gpuInfoList.add(gpuInfo1);

        }
        return gpuInfoList;
    }

    public int selectGPU() throws IOException {
        List<Boolean> res = getGPUList();
        for (int i = 0; i < res.size(); i++) {
            if (res.get(i)) {
                return i;
            }
        }
        return -1;
    }
}
