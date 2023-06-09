package com.taozi.deblur.pojo;

import lombok.Data;

@Data
public class GPUInfo {
    private Integer number;

    private String name;

    private String totalMemory;

    private String usedMemory;

    private String useableMemory;

    private Double usageRate;
}

