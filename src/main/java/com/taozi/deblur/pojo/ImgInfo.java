package com.taozi.deblur.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ImgInfo {
    private List<String> imgPath;
    private int ssim;
    private int psnr;
}
